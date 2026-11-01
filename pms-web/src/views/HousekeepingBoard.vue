<template>
  <el-card>
    <template #header>
      <div style="display:flex; justify-content:space-between; align-items:center; gap:12px">
        <span>房态看板</span>
        <div style="display:flex; align-items:center; gap:8px">
          <el-button size="small" @click="shift(-7)">« 前 7 天</el-button>
          <el-button size="small" @click="shift(7)">后 7 天 »</el-button>
          <el-button size="small" @click="resetRange">回到今日</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>
    </template>

    <div class="cal-wrap" @click.self="cancelRange">
      <table class="cal">
        <thead>
          <tr>
            <th class="corner">房型\\房号\\日期</th>
            <th v-for="d in board.dates" :key="d" class="date-head" :class="{ today: d === todayStr }">
              <div class="wd">{{ weekday(d) }}</div>
              <div class="md">{{ md(d) }}</div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in board.rooms" :key="r.id">
            <td class="room-head" title="点击查看房务（派单/报修）" @click.stop="openRoomPanel(r)">
              <div style="font-weight:700">{{ r.number }}</div>
              <div style="font-size:11px; color:#888">{{ roomTypeName(r.roomTypeId) }}</div>
              <div class="rh-badges">
                <span class="dot" :class="'c-' + (r.rentStatus === 'CLOSED' ? 'closed' : r.cleanStatus === 'DIRTY' ? 'dirty' : 'clean')"></span>
                <span v-if="pendingTasks[r.id]" class="badge badge-clean">待清扫</span>
                <span v-if="openFixes[r.id]" class="badge badge-fix">维修</span>
              </div>
            </td>
            <td v-for="d in board.dates" :key="r.id + ':' + d"
                class="cell" :class="cellClass(r, d)"
                @click="onCellClick(r, d)"
                @mouseenter="onCellHover(r, d)">
              <template v-if="cellOf(r, d)">
                <div class="guest">{{ name2(cellOf(r, d).guestName) }}</div>
                <div class="ch">
                  <i class="ch-icon" />
                  <template v-if="cellOf(r, d).type === 'block'">{{ cellOf(r, d).reason || '封房中' }}</template>
                  <template v-else>{{ cellOf(r, d).channelName || '—' }}</template>
                </div>
              </template>
              <template v-else>
                <button class="more-btn" title="更多操作" @click.stop="openCell(r, d)">⋯</button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="legend">
      <span class="lg cell-stay"></span>在住
      <span class="lg cell-res"></span>已排房(预订)
      <span class="lg cell-clean"></span>打开·净
      <span class="lg cell-dirty"></span>打开·脏
      <span class="lg cell-closed"></span>关闭/封房
    </div>

    <!-- 格子操作面板 -->
    <el-dialog v-model="actionVisible" :title="actionTitle" width="360px">
      <div v-if="selected" style="display:flex; flex-direction:column; gap:10px">
        <!-- 空房操作 -->
        <template v-if="!selected.cell">
          <!-- 关闭房：仅允许打开 -->
          <template v-if="selected.room.rentStatus === 'CLOSED'">
            <el-button type="warning" @click="onToggleRent">打开房间</el-button>
          </template>
          <!-- 打开房：录单走左键范围选房，⋯ 提供其余操作 -->
          <template v-else>
            <el-button type="success" @click="openHourly">钟点房</el-button>
            <el-button @click="onRemark">备注{{ selected.room.remark ? '（已有）' : '' }}</el-button>
            <el-button type="danger" @click="onToggleRent">关闭房间</el-button>
            <div class="status-row">
              <span class="status-label">卫生状态</span>
              <el-button-group>
                <el-button size="small" :type="selected.room.cleanStatus === 'CLEAN' ? 'success' : ''"
                           @click="onSetClean('CLEAN')">净房</el-button>
                <el-button size="small" :type="selected.room.cleanStatus === 'DIRTY' ? 'warning' : ''"
                           @click="onSetClean('DIRTY')">脏房</el-button>
              </el-button-group>
            </div>
          </template>
        </template>
        <!-- 已排房(预订)操作 -->
        <template v-else-if="selected.cell.type === 'reservation'">
          <el-button type="warning" @click="onCheckin">改为已入住（办理入住）</el-button>
          <div style="color:#999; font-size:12px">预订号 #{{ selected.cell.id }} · {{ selected.cell.status }}（待入住）</div>
        </template>
        <!-- 封房操作 -->
        <template v-else-if="selected.cell.type === 'block'">
          <el-button type="warning" @click="onReleaseBlock">释放封房（恢复可售）</el-button>
          <div style="color:#999; font-size:12px">
            封房 #{{ selected.cell.id }} · {{ selected.cell.checkIn }} → {{ selected.cell.checkOut }}
            {{ selected.cell.reason ? ' · ' + selected.cell.reason : '' }}
          </div>
        </template>
        <!-- 在住操作 -->
        <template v-else-if="selected.cell.type === 'stay'">
          <el-button type="primary" @click="openExtend">续住</el-button>
          <el-button type="warning" @click="onTransfer">换房</el-button>
          <el-button type="danger" @click="onCheckout">退房</el-button>
          <el-button type="info" @click="onUndoCheckin">改为待入住（撤销入住）</el-button>
          <div class="status-row">
            <span class="status-label">卫生状态</span>
            <el-button-group>
              <el-button size="small" :type="selected.room.cleanStatus === 'CLEAN' ? 'success' : ''"
                         @click="onSetClean('CLEAN')">净房</el-button>
              <el-button size="small" :type="selected.room.cleanStatus === 'DIRTY' ? 'warning' : ''"
                         @click="onSetClean('DIRTY')">脏房</el-button>
            </el-button-group>
          </div>
          <div style="color:#999; font-size:12px">在住 #{{ selected.cell.id }}{{ selected.cell.hourly ? ' · 钟点房' : '' }}</div>
        </template>
      </div>
    </el-dialog>

    <!-- 录入订单 -->
    <ReservationCreateDialog v-model="createVisible" :prefill="createPrefill" @success="onOrderCreated" />

    <!-- 范围选房后操作选择器（悬浮浮层） -->
    <teleport to="body">
      <div v-if="rangePopVisible" class="range-pop" :style="{ left: rangePopPos.x + 'px', top: rangePopPos.y + 'px' }">
        <div class="rp-header">
          <strong>{{ rangeSelected?.room.number }}</strong>
          <span>已选中 {{ rangeNights() }} 晚</span>
        </div>
        <div class="rp-actions">
          <el-button size="small" type="primary" @click="rangeCreateOrder">录入订单</el-button>
          <el-button size="small" @click="rangeRemark">备注</el-button>
          <el-button v-if="rangeSelected?.room.rentStatus === 'CLOSED'" size="small" type="warning" @click="rangeCloseRoom">打开</el-button>
          <el-button v-else size="small" type="danger" @click="rangeCloseRoom">关房</el-button>
        </div>
      </div>
    </teleport>

    <!-- 钟点房 -->
    <el-dialog v-model="hourlyVisible" title="钟点房" width="420px">
      <el-form :model="hourlyForm" label-width="90px">
        <el-form-item label="房号"><el-tag>{{ hourlyForm.roomNumber }}</el-tag></el-form-item>
        <el-form-item label="客人姓名"><el-input v-model="hourlyForm.guestName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="hourlyForm.guestPhone" /></el-form-item>
        <el-form-item label="时长(小时)"><el-input-number v-model="hourlyForm.hours" :min="1" /></el-form-item>
        <el-form-item label="小时单价"><el-input-number v-model="hourlyForm.hourlyRate" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="收款方式">
          <el-select v-model="hourlyForm.method">
            <el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" />
            <el-option label="银行卡" value="CARD" /><el-option label="支付宝" value="ALIPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="实收金额"><el-input-number v-model="hourlyForm.amount" :min="0" :precision="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="hourlyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitHourly">确认入住</el-button>
      </template>
    </el-dialog>

    <!-- 续住 -->
    <el-dialog v-model="extendVisible" title="续住" width="420px">
      <el-form :model="extendForm" label-width="90px">
        <el-form-item label="续住天数"><el-input-number v-model="extendForm.days" :min="1" /></el-form-item>
        <el-form-item label="收款方式">
          <el-select v-model="extendForm.method">
            <el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" />
            <el-option label="银行卡" value="CARD" /><el-option label="支付宝" value="ALIPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="实收金额"><el-input-number v-model="extendForm.amount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="extendForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extendVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitExtend">确认续住</el-button>
      </template>
    </el-dialog>
    <!-- 房务面板：点房号打开 -->
    <el-dialog v-model="panelVisible" :title="panelTitle" width="470px">
      <template v-if="panelRoom">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="房型">{{ roomTypeName(panelRoom.roomTypeId) }}</el-descriptions-item>
          <el-descriptions-item label="楼层">{{ panelRoom.floor ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="可售状态">
            <el-tag size="small" :type="panelRoom.rentStatus === 'OPEN' ? 'success' : panelRoom.rentStatus === 'ASSIGNED' ? 'warning' : 'info'">
              {{ rentText(panelRoom.rentStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="卫生状态">
            <el-tag size="small" effect="plain" :type="panelRoom.cleanStatus === 'CLEAN' ? 'success' : 'warning'">
              {{ panelRoom.cleanStatus === 'CLEAN' ? '净房' : '脏房' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ panelRoom.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <div class="panel-block">
          <div class="panel-block-title">清扫任务</div>
          <template v-if="panelTask">
            <div class="panel-line">
              {{ taskTypeText(panelTask.type) }} · 派单于 {{ fmtTime(panelTask.createdAt) }}
            </div>
            <el-button size="small" type="success" @click="doFinishTask">完成清扫（转净房）</el-button>
          </template>
          <div v-else class="panel-muted">无进行中任务</div>
        </div>

        <div class="panel-block">
          <div class="panel-block-title">维修单</div>
          <template v-if="panelFix">
            <div class="panel-line">{{ panelFix.issue }} · 报修于 {{ fmtTime(panelFix.createdAt) }}</div>
            <el-button size="small" type="success" @click="doFixMaintenance">修复（恢复可售）</el-button>
          </template>
          <div v-else class="panel-muted">无未完成报修</div>
        </div>

        <div class="panel-actions">
          <el-button type="primary" :disabled="!!panelTask" @click="openTaskDialog">派清扫单</el-button>
          <el-button type="danger" :disabled="!!panelFix" @click="openRepairDialog">报修</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 派清扫单 -->
    <el-dialog v-model="taskVisible" title="派清扫单" width="440px">
      <el-form label-width="80px">
        <el-form-item label="房号"><el-tag>{{ panelRoom?.number }}</el-tag></el-form-item>
        <el-form-item label="清扫类型">
          <el-radio-group v-model="taskForm.type">
            <el-radio-button value="DAILY">日常清扫</el-radio-button>
            <el-radio-button value="CHECKOUT_CLEAN">退房清扫</el-radio-button>
            <el-radio-button value="DEEP_CLEAN">深度清洁</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="指派给">
          <el-select v-model="taskForm.assigneeId" clearable placeholder="不指派" style="width:100%">
            <el-option v-for="s in staffList" :key="s.id" :label="s.realName || s.username" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTask">派单</el-button>
      </template>
    </el-dialog>

    <!-- 报修 -->
    <el-dialog v-model="repairVisible" title="报修" width="440px">
      <el-form label-width="80px">
        <el-form-item label="房号"><el-tag>{{ panelRoom?.number }}</el-tag></el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="repairForm.issue" type="textarea" :rows="3" placeholder="如：空调不制冷、马桶漏水" />
        </el-form-item>
      </el-form>
      <div class="tip-line">报修后该房置为「关闭」不可售，修复后自动恢复可售。</div>
      <template #footer>
        <el-button @click="repairVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="submitRepair">提交报修</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { housekeepingApi, roomApi, stayApi, reservationApi, systemApi } from '@/api/modules'
import ReservationCreateDialog from '@/components/ReservationCreateDialog.vue'

const todayStr = new Date().toISOString().slice(0, 10)
const from = ref(todayStr)
const to = ref(addDays(todayStr, 20))

const board = reactive<{ rooms: any[]; dates: string[]; cells: Record<string, any> }>({
  rooms: [], dates: [], cells: {},
})
const roomTypeMap = ref<Record<number, any>>({})

const selected = ref<{ room: any; date: string; cell: any } | null>(null)
const actionVisible = ref(false)
const saving = ref(false)

// 录入订单
const createVisible = ref(false)
const createPrefill = ref<any>({})

// 范围选房后的操作选择器（悬浮浮层）
const rangeSelected = ref<{ room: any; start: string; end: string } | null>(null)
const rangePopVisible = ref(false)
const rangePopPos = reactive({ x: 0, y: 0 })

// 钟点房
const hourlyVisible = ref(false)
const hourlyForm = reactive({ roomId: 0, roomNumber: '', guestName: '', guestPhone: '', hours: 3, hourlyRate: 50, method: 'CASH', amount: 0 })

// 续住
const extendVisible = ref(false)
const extendForm = reactive({ stayId: 0, days: 1, method: 'CASH', amount: 0, remark: '' })

const actionTitle = computed(() => {
  if (!selected.value) return ''
  const r = selected.value.room
  const c = selected.value.cell
  return c ? `房号 ${r.number} · ${selected.value.date}（${c.type === 'stay' ? '在住' : c.type === 'block' ? '封房' : '预订'}）`
           : `房号 ${r.number} · ${selected.value.date}（空房）`
})

function addDays(d: string, n: number) {
  const dt = new Date(d); dt.setDate(dt.getDate() + n); return dt.toISOString().slice(0, 10)
}
function weekday(d: string) { return '日一二三四五六'[new Date(d).getDay()] + '周' }
function md(d: string) { const dt = new Date(d); return (dt.getMonth() + 1) + '-' + dt.getDate() }
function roomTypeName(id: number) { return roomTypeMap.value[id]?.name || '' }

async function load() {
  const b: any = await housekeepingApi.calendar(from.value, to.value)
  board.rooms = b.rooms || []
  board.dates = b.dates || []
  board.cells = b.cells || {}
  const types: any[] = await roomApi.types()
  roomTypeMap.value = Object.fromEntries(types.map(t => [t.id, t]))
  await loadDuty()
}

// ---- 房务：清扫任务 / 维修单 ----
const pendingTasks = ref<Record<number, any>>({})
const openFixes = ref<Record<number, any>>({})

async function loadDuty() {
  const [tasks, fixes] = await Promise.all([
    housekeepingApi.tasks({ status: 'PENDING' }),
    housekeepingApi.maintenances({ status: 'OPEN' }),
  ])
  pendingTasks.value = Object.fromEntries((tasks as any[]).map((t) => [t.roomId, t]))
  openFixes.value = Object.fromEntries((fixes as any[]).map((m) => [m.roomId, m]))
}

const panelVisible = ref(false)
const panelRoom = ref<any>(null)
const panelTask = computed(() => (panelRoom.value ? pendingTasks.value[panelRoom.value.id] : null))
const panelFix = computed(() => (panelRoom.value ? openFixes.value[panelRoom.value.id] : null))
const panelTitle = computed(() =>
  panelRoom.value ? `房号 ${panelRoom.value.number} · ${roomTypeName(panelRoom.value.roomTypeId)}` : '',
)

const staffList = ref<any[]>([])
const taskVisible = ref(false)
const taskForm = reactive({ type: 'DAILY', assigneeId: undefined as number | undefined })
const repairVisible = ref(false)
const repairForm = reactive({ issue: '' })

function openRoomPanel(r: any) {
  cancelRange()
  panelRoom.value = r
  panelVisible.value = true
}

/** 刷新看板与房务数据，并保持面板指向最新的房间对象。 */
async function refreshAll() {
  const roomId = panelRoom.value?.id
  await load()
  if (roomId) panelRoom.value = board.rooms.find((x: any) => x.id === roomId) || null
}

function taskTypeText(t: string) {
  return { DAILY: '日常清扫', CHECKOUT_CLEAN: '退房清扫', DEEP_CLEAN: '深度清洁' }[t] || t
}
function rentText(s: string) {
  return { OPEN: '打开', CLOSED: '关闭', ASSIGNED: '已排房' }[s] || s
}
function fmtTime(s: string) {
  return s ? String(s).replace('T', ' ').slice(0, 16) : '—'
}

async function openTaskDialog() {
  if (!staffList.value.length) {
    try { staffList.value = (await systemApi.staff()) as any[] } catch {}
  }
  taskForm.type = panelRoom.value?.cleanStatus === 'DIRTY' ? 'CHECKOUT_CLEAN' : 'DAILY'
  taskForm.assigneeId = undefined
  taskVisible.value = true
}

async function submitTask() {
  if (!panelRoom.value) return
  saving.value = true
  try {
    await housekeepingApi.createTask({
      room_id: panelRoom.value.id,
      type: taskForm.type,
      assignee_id: taskForm.assigneeId ?? null,
    })
    ElMessage.success('已派单')
    taskVisible.value = false
    await refreshAll()
  } finally { saving.value = false }
}

async function doFinishTask() {
  if (!panelTask.value) return
  await housekeepingApi.finishTask(panelTask.value.id)
  ElMessage.success('已完成清扫，房间转净房')
  await refreshAll()
}

function openRepairDialog() {
  repairForm.issue = ''
  repairVisible.value = true
}

async function submitRepair() {
  if (!panelRoom.value) return
  if (!repairForm.issue.trim()) { ElMessage.warning('请填写故障描述'); return }
  saving.value = true
  try {
    await housekeepingApi.reportMaintenance({ room_id: panelRoom.value.id, issue: repairForm.issue.trim() })
    ElMessage.success('已报修，房间已置为不可售')
    repairVisible.value = false
    await refreshAll()
  } finally { saving.value = false }
}

async function doFixMaintenance() {
  if (!panelFix.value) return
  await housekeepingApi.fixMaintenance(panelFix.value.id)
  ElMessage.success('已修复，房间恢复可售')
  await refreshAll()
}

function shift(n: number) { from.value = addDays(from.value, n); to.value = addDays(to.value, n); load() }
function resetRange() { from.value = todayStr; to.value = addDays(todayStr, 20); load() }

function cellKey(roomId: number, date: string) { return roomId + ':' + date }
function cellOf(r: any, d: string) { return board.cells[cellKey(r.id, d)] }
/** 预订姓名取前两个汉字 */
function name2(s: string) { return (s || '—').slice(0, 2) }

// ---- 日期范围多选（左键点入住日 → 右移 → 再点离店日 → 自动开录入订单弹窗）----
const rangeStart = ref<{ roomId: number; date: string } | null>(null)
const rangeHover = ref<{ roomId: number; date: string } | null>(null)

function inRange(r: any, d: string) {
  if (!rangeStart.value || rangeStart.value.roomId !== r.id) return false
  if (!rangeHover.value || rangeHover.value.roomId !== r.id) return d === rangeStart.value.date
  const a = rangeStart.value.date, b = rangeHover.value.date
  const lo = a < b ? a : b, hi = a < b ? b : a
  // 范围高亮 = 入住日（含）到用户选中的最后一晚（含）→ 即全部间夜
  return d >= lo && d <= hi
}
function cellClass(r: any, d: string) {
  const c = cellOf(r, d)
  if (c) {
    if (c.type === 'stay') return 'cell-stay'
    if (c.type === 'block') return 'cell-closed'
    return 'cell-res'
  }
  if (r.rentStatus === 'CLOSED') return inRange(r, d) ? 'cell-closed cell-range' : 'cell-closed'
  if (inRange(r, d)) return 'cell-clean cell-range'
  return r.cleanStatus === 'DIRTY' ? 'cell-dirty' : 'cell-clean'
}

function onCellClick(r: any, d: string) {
  const c = cellOf(r, d)
  if (c) { openCell(r, d); return }            // occupied → 操作弹窗
  // 空格（含已关闭房）→ 范围选房，左键点选出现蓝色选中样式
  if (!rangeStart.value) {
    rangeStart.value = { roomId: r.id, date: d }
    rangeHover.value = { roomId: r.id, date: d }
    return
  }
  if (rangeStart.value.roomId === r.id && d >= rangeStart.value.date) {
    // 同房当天或更右 → 选定离店日，弹出操作选择器（录入订单/关闭/备注）
    openRangeAction(rangeStart.value.date, addDays(d, 1), r)
    rangeStart.value = null
    rangeHover.value = null
  } else {
    // 跨房或更左 → 重置起点
    rangeStart.value = { roomId: r.id, date: d }
    rangeHover.value = { roomId: r.id, date: d }
  }
}
function onCellHover(r: any, d: string) {
  if (!rangeStart.value) return
  if (cellOf(r, d)) return
  if (r.id === rangeStart.value.roomId) rangeHover.value = { roomId: r.id, date: d }
}
function openRangeAction(start: string, end: string, r: any) {
  rangeSelected.value = { room: r, start, end }
  rangePopPos.x = (window.event as any)?.clientX ?? 0
  rangePopPos.y = ((window.event as any)?.clientY ?? 0) - 70
  rangePopVisible.value = true
}
function rangeNights() {
  const rs = rangeSelected.value
  if (!rs) return 0
  const a = new Date(rs.start), b = new Date(rs.end)
  return Math.round((b.getTime() - a.getTime()) / 86400000)
}
function rangeCreateOrder() {
  const rs = rangeSelected.value!
  rangePopVisible.value = false
  const single = rs.start === rs.end
  createPrefill.value = {
    checkInDate: rs.start,
    // 多日选房：第二次点击即离店日；单日选房：仅入住日，离店由弹窗默认次日
    checkOutDate: single ? '' : rs.end,
    roomTypeId: rs.room.roomTypeId,
    assignedRoomId: rs.room.id,
    roomNumber: rs.room.number,
  }
  createVisible.value = true
}
/** 关闭/打开房间：已关闭房 → 整房打开(rent_status=OPEN)；打开房 → 按选中日期范围封房。 */
function rangeCloseRoom() {
  const rs = rangeSelected.value!
  rangePopVisible.value = false
  if (rs.room.rentStatus === 'CLOSED') {
    roomApi.setRentStatus(rs.room.id, 'OPEN').then(() => {
      ElMessage.success('已打开房间')
      load()
    })
    return
  }
  ElMessageBox.prompt('封房原因（可选）', `封房 ${rs.room.number} · ${rs.start} → ${rs.end}`, {
    inputPlaceholder: '如：维修、自用',
    confirmButtonText: '确认封房',
    cancelButtonText: '取消',
    inputValue: '',
  }).then(async ({ value }) => {
    await housekeepingApi.blockRoom(rs.room.id, rs.start, rs.end, value || undefined)
    ElMessage.success('已封房（该日期段关闭）')
    load()
  }).catch(() => {})
}

/** 释放封房：恢复该房所选日期的可售。 */
function onReleaseBlock() {
  const cell = selected.value!.cell
  actionVisible.value = false
  ElMessageBox.confirm(
    `释放封房后，该房 ${cell.checkIn} → ${cell.checkOut} 恢复可售。确认？`,
    '释放封房',
    { type: 'warning', confirmButtonText: '确认释放', cancelButtonText: '取消' },
  ).then(async () => {
    await housekeepingApi.releaseBlock(cell.id)
    ElMessage.success('已释放封房')
    load()
  }).catch(() => {})
}
function rangeRemark() {
  const r = rangeSelected.value!.room
  rangePopVisible.value = false
  ElMessageBox.prompt('房间备注', '备注', { inputValue: r.remark || '' })
    .then(async ({ value }) => {
      await roomApi.setRemark(r.id, value)
      ElMessage.success('备注已保存')
      load()
    }).catch(() => {})
}
function cancelRange() {
  rangeStart.value = null; rangeHover.value = null; rangePopVisible.value = false
}

function openCell(r: any, d: string) {
  cancelRange()
  selected.value = { room: r, date: d, cell: cellOf(r, d) }
  actionVisible.value = true
}

// ---- 空房操作 ----
function onOrderCreated() { load() }

function openHourly() {
  actionVisible.value = false
  const r = selected.value!.room
  hourlyForm.roomId = r.id
  hourlyForm.roomNumber = r.number
  hourlyForm.guestName = ''
  hourlyForm.guestPhone = ''
  hourlyForm.hours = 3
  hourlyForm.hourlyRate = roomTypeMap.value[r.roomTypeId]?.baseRate ? Number(roomTypeMap.value[r.roomTypeId].baseRate) : 50
  hourlyForm.method = 'CASH'
  hourlyForm.amount = 0
  hourlyVisible.value = true
}
async function submitHourly() {
  if (!hourlyForm.guestName) { ElMessage.warning('请填写客人姓名'); return }
  saving.value = true
  try {
    await stayApi.hourly({
      room_id: hourlyForm.roomId,
      guest_name: hourlyForm.guestName,
      guest_phone: hourlyForm.guestPhone,
      hours: hourlyForm.hours,
      hourly_rate: hourlyForm.hourlyRate,
      method: hourlyForm.method,
      amount: hourlyForm.amount || undefined,
    })
    ElMessage.success('钟点房已入住')
    hourlyVisible.value = false
    load()
  } finally { saving.value = false }
}

function onRemark() {
  const r = selected.value!.room
  actionVisible.value = false
  ElMessageBox.prompt('房间备注', '备注', { inputValue: r.remark || '' })
    .then(async ({ value }) => {
      await roomApi.setRemark(r.id, value)
      ElMessage.success('备注已保存')
      load()
    }).catch(() => {})
}

async function onToggleRent() {
  const r = selected.value!.room
  const target = r.rentStatus === 'CLOSED' ? 'OPEN' : 'CLOSED'
  actionVisible.value = false
  await roomApi.setRentStatus(r.id, target)
  ElMessage.success(target === 'CLOSED' ? '已关闭' : '已打开')
  load()
}

/** 卫生状态切换：净房 / 脏房（空房与在住房均可改）。 */
async function onSetClean(status: string) {
  const r = selected.value!.room
  if (r.cleanStatus === status) { actionVisible.value = false; return }
  actionVisible.value = false
  await roomApi.setCleanStatus(r.id, status)
  ElMessage.success(status === 'CLEAN' ? '已标记净房' : '已标记脏房')
  load()
}

// ---- 已排房(预订)操作 ----
async function onCheckin() {
  const cell = selected.value!.cell
  const r = selected.value!.room
  actionVisible.value = false
  const detail: any = await reservationApi.get(cell.id)
  const line = detail.rooms?.[0]
  if (!line) { ElMessage.warning('无预订房型明细'); return }
  await stayApi.checkin({
    reservationId: cell.id,
    stayItems: [{ reservationRoomId: line.id, roomId: r.id }],
    deposit: 500, depositMethod: 'CASH',
  })
  ElMessage.success('已入住')
  load()
}

// ---- 在住操作 ----
function openExtend() {
  extendForm.stayId = selected.value!.cell.id
  extendForm.days = 1
  extendForm.method = 'CASH'
  extendForm.amount = 0
  extendForm.remark = ''
  actionVisible.value = false
  extendVisible.value = true
}
async function submitExtend() {
  saving.value = true
  try {
    await stayApi.extend(extendForm.stayId, {
      days: extendForm.days,
      amount: extendForm.amount || undefined,
      method: extendForm.method,
      remark: extendForm.remark || undefined,
    })
    ElMessage.success('已续住')
    extendVisible.value = false
    load()
  } finally { saving.value = false }
}

function onTransfer() {
  const cell = selected.value!.cell
  actionVisible.value = false
  ElMessageBox.prompt('目标房号（如 103）', '换房', {})
    .then(async ({ value }) => {
      const target = board.rooms.find((x: any) => x.number === value.trim())
      if (!target) { ElMessage.warning('房号不存在'); return }
      await stayApi.transferRoom(cell.id, target.id, '看板换房')
      ElMessage.success('已换房')
      load()
    }).catch(() => {})
}

function onCheckout() {
  const cell = selected.value!.cell
  actionVisible.value = false
  ElMessageBox.prompt('实收金额', '退房收款', { inputPattern: /^\d+(\.\d+)?$/, inputErrorMessage: '请输入金额' })
    .then(async ({ value }) => {
      await stayApi.checkout(cell.id, 'WECHAT', Number(value))
      ElMessage.success('已退房，房间转为脏房')
      load()
    }).catch(() => {})
}

/** 撤销入住（已入住 → 待入住）：回滚在住+账单，房态恢复净房，预订回 CONFIRMED。 */
function onUndoCheckin() {
  const cell = selected.value!.cell
  actionVisible.value = false
  ElMessageBox.confirm(
    '撤销入住将删除该在住及全部账单（含押金收款），房态恢复净房，预订回退为待入住。\n已有续住/换房的在住不可撤销，请改用退房。',
    '撤销入住确认',
    { type: 'warning', confirmButtonText: '确认撤销', cancelButtonText: '取消' },
  ).then(async () => {
    await stayApi.undoCheckin(cell.id)
    ElMessage.success('已撤销入住，房间回到待入住（净房）')
    load()
  }).catch(() => {})
}

// ---- WS 实时刷新 ----
let ws: WebSocket | null = null
let reconnectTimer: any = null
function connectWs() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  ws = new WebSocket(`${proto}://${location.host}/api/v1/ws/room-status`)
  ws.onmessage = (ev) => {
    try { const msg = JSON.parse(ev.data); if (msg.type === 'room-status') load() } catch {}
  }
  ws.onclose = () => { reconnectTimer = setTimeout(connectWs, 3000) }
}

function onKeydown(e: KeyboardEvent) { if (e.key === 'Escape') cancelRange() }

onMounted(() => { load(); connectWs(); window.addEventListener('keydown', onKeydown) })
onUnmounted(() => {
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (ws) ws.close()
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.cal-wrap { overflow: auto; max-height: 70vh; border: 1px solid #ebeef5; }
.cal { border-collapse: collapse; font-size: 12px; }
.cal th, .cal td { border: 1px solid #ebeef5; padding: 4px; }
.cal thead th { position: sticky; top: 0; z-index: 2; background: #f5f7fa; }
.cal .corner { position: sticky; left: 0; top: 0; z-index: 3; background: #f5f7fa; min-width: 110px; }
.cal .date-head { min-width: 64px; text-align: center; }
.cal .date-head.today { background: #ecf5ff; }
.cal .wd { font-weight: 600; }
.cal .md { color: #888; font-size: 11px; }
.cal .room-head { position: sticky; left: 0; z-index: 1; background: #fff; min-width: 110px; }
.cal .cell { height: 46px; vertical-align: top; cursor: pointer; position: relative; }
.cal .cell:hover { outline: 2px solid #409eff; outline-offset: -2px; }
.cal .cell-stay { background: #d9ecff; }
.cal .cell-res { background: #d1edc4; }
.cal .cell-clean { background: #ffffff; }
.cal .cell-dirty { background: #fdf6ec; }
.cal .cell-closed { background: #909399; color: #c0c4cc; cursor: pointer; }
/* 关闭房斜杠：对角线；选中(cell-range)时隐藏，让蓝色选中样式干净 */
.cal .cell-closed::after {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(to top right, transparent calc(50% - 1px), #c0c4cc 50%, transparent calc(50% + 1px));
}
.cal .cell-closed.cell-range::after { display: none; }
/* 范围选房高亮 */
.cal .cell-range { background: #ecf5ff !important; box-shadow: inset 0 0 0 2px #409eff; }
.cal .guest { font-weight: 600; font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cal .ch { font-size: 10px; color: #555; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: flex; align-items: center; gap: 2px; }
.status-row { display: flex; align-items: center; gap: 8px; margin-top: 4px; }
.status-label { font-size: 12px; color: #888; white-space: nowrap; }
.range-tip { font-size: 12px; color: #e6a23c; background: #fdf6ec; border: 1px solid #faecd8; border-radius: 4px; padding: 6px 8px; }
.cal .ch-icon { display: inline-block; width: 10px; height: 10px; flex: 0 0 10px; border-radius: 2px; background: #c0c4cc; opacity: .5; }
/* 空格右上角 ⋯ 按钮 */
.cal .more-btn {
  position: absolute; top: 0; right: 2px; border: none; background: transparent;
  color: #909399; font-size: 12px; line-height: 1; cursor: pointer; padding: 0 2px; opacity: .35;
}
.cal .cell:hover .more-btn { opacity: 1; }
.cal .cell-closed .more-btn { color: #fff; opacity: .7; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-left: 4px; vertical-align: middle; }
.rh-badges { display: flex; align-items: center; gap: 4px; margin-top: 2px; }
.badge { font-size: 10px; line-height: 14px; padding: 0 4px; border-radius: 2px; color: #fff; }
.badge-clean { background: #e6a23c; }
.badge-fix { background: #f56c6c; }
.room-head { cursor: pointer; }
.room-head:hover { background: #ecf5ff; }
.panel-block { margin-top: 14px; border-top: 1px dashed #ebeef5; padding-top: 10px; }
.panel-block-title { font-weight: 600; font-size: 13px; margin-bottom: 6px; }
.panel-line { font-size: 12px; color: #606266; margin-bottom: 8px; }
.panel-muted { font-size: 12px; color: #c0c4cc; }
.panel-actions { display: flex; gap: 10px; margin-top: 16px; }
.tip-line { font-size: 12px; color: #e6a23c; margin-top: 4px; }
.dot.c-clean { background: #67c23a; }
.dot.c-dirty { background: #e6a23c; }
.dot.c-closed { background: #909399; }
.legend { margin-top: 10px; color: #666; }
.lg { display: inline-block; width: 14px; height: 14px; border: 1px solid #ddd; margin: 0 4px 0 12px; vertical-align: middle; }
.lg.cell-stay { background: #d9ecff; }
.lg.cell-res { background: #d1edc4; }
.lg.cell-clean { background: #ffffff; }
.lg.cell-dirty { background: #fdf6ec; }
.lg.cell-closed { background: #909399; position: relative; }
.lg.cell-closed::after { content:''; position:absolute; inset:0; background: linear-gradient(to top right, transparent calc(50% - 1px), #c0c4cc 50%, transparent calc(50% + 1px)); }

/* 范围选房悬浮浮层 */
:global(.range-pop) {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,.12);
  padding: 8px 10px;
  min-width: 160px;
  pointer-events: auto;
}
.rp-header { font-size: 12px; color: #606266; margin-bottom: 6px; display: flex; align-items: center; gap: 4px; }
.rp-header strong { color: #303133; }
.rp-nights { font-size: 11px; color: #409eff; margin-left: auto; }
.rp-actions { display: flex; gap: 4px; }
.rp-actions .el-button { font-size: 12px; }
</style>
