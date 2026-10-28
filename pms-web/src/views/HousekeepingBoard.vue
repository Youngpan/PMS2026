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

    <div class="cal-wrap">
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
            <td class="room-head">
              <div style="font-weight:700">{{ r.number }}</div>
              <div style="font-size:11px; color:#888">{{ roomTypeName(r.roomTypeId) }}</div>
              <div class="dot" :class="'c-' + (r.rentStatus === 'CLOSED' ? 'closed' : r.cleanStatus === 'DIRTY' ? 'dirty' : 'clean')"></div>
            </td>
            <td v-for="d in board.dates" :key="r.id + ':' + d"
                class="cell" :class="cellClass(r, d)"
                @click="openCell(r, d)">
              <template v-if="cellOf(r, d)">
                <div class="guest">{{ cellOf(r, d).guestName || '—' }}</div>
                <div class="tag">{{ cellTag(cellOf(r, d)) }}</div>
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
      <span class="lg cell-closed"></span>关闭
    </div>

    <!-- 格子操作面板 -->
    <el-dialog v-model="actionVisible" :title="actionTitle" width="360px">
      <div v-if="selected" style="display:flex; flex-direction:column; gap:10px">
        <!-- 空房操作 -->
        <template v-if="!selected.cell">
          <el-button type="primary" @click="onCreateOrder">录入订单</el-button>
          <el-button type="success" @click="openHourly">钟点房</el-button>
          <el-button @click="onRemark">备注{{ selected.room.remark ? '（已有）' : '' }}</el-button>
          <el-button :type="selected.room.rentStatus === 'CLOSED' ? 'warning' : 'danger'"
                     @click="onToggleRent">
            {{ selected.room.rentStatus === 'CLOSED' ? '打开房间' : '关闭房间' }}
          </el-button>
        </template>
        <!-- 已排房(预订)操作 -->
        <template v-else-if="selected.cell.type === 'reservation'">
          <el-button type="warning" @click="onCheckin">办理入住（待入住→已入住）</el-button>
          <div style="color:#999; font-size:12px">预订号 #{{ selected.cell.id }} · {{ selected.cell.status }}</div>
        </template>
        <!-- 在住操作 -->
        <template v-else-if="selected.cell.type === 'stay'">
          <el-button type="primary" @click="openExtend">续住</el-button>
          <el-button type="warning" @click="onTransfer">换房</el-button>
          <el-button type="danger" @click="onCheckout">退房</el-button>
          <div style="color:#999; font-size:12px">在住 #{{ selected.cell.id }}{{ selected.cell.hourly ? ' · 钟点房' : '' }}</div>
        </template>
      </div>
    </el-dialog>

    <!-- 录入订单 -->
    <ReservationCreateDialog v-model="createVisible" :prefill="createPrefill" @success="onOrderCreated" />

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
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { housekeepingApi, roomApi, stayApi, reservationApi } from '@/api/modules'
import ReservationCreateDialog from '@/components/ReservationCreateDialog.vue'

const todayStr = new Date().toISOString().slice(0, 10)
const from = ref(addDays(todayStr, -1))
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
  return c ? `房号 ${r.number} · ${selected.value.date}（${c.type === 'stay' ? '在住' : '预订'}）`
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
}

function shift(n: number) { from.value = addDays(from.value, n); to.value = addDays(to.value, n); load() }
function resetRange() { from.value = addDays(todayStr, -1); to.value = addDays(todayStr, 20); load() }

function cellKey(roomId: number, date: string) { return roomId + ':' + date }
function cellOf(r: any, d: string) { return board.cells[cellKey(r.id, d)] }
function cellClass(r: any, d: string) {
  const c = cellOf(r, d)
  if (c) return c.type === 'stay' ? 'cell-stay' : 'cell-res'
  if (r.rentStatus === 'CLOSED') return 'cell-closed'
  return r.cleanStatus === 'DIRTY' ? 'cell-dirty' : 'cell-clean'
}
function cellTag(c: any) {
  if (c.type === 'stay') return c.hourly ? '钟点' : '在住'
  return c.status === 'CONFIRMED' ? '已确认' : c.status
}

function openCell(r: any, d: string) {
  selected.value = { room: r, date: d, cell: cellOf(r, d) }
  actionVisible.value = true
}

// ---- 空房操作 ----
function onCreateOrder() {
  actionVisible.value = false
  createPrefill.value = {
    checkInDate: selected.value!.date,
    roomTypeId: selected.value!.room.roomTypeId,
    assignedRoomId: selected.value!.room.id,
    roomNumber: selected.value!.room.number,
  }
  createVisible.value = true
}
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

onMounted(() => { load(); connectWs() })
onUnmounted(() => {
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (ws) ws.close()
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
.cal .cell { height: 46px; vertical-align: top; cursor: pointer; }
.cal .cell:hover { outline: 2px solid #409eff; outline-offset: -2px; }
.cal .cell-stay { background: #d9ecff; }
.cal .cell-res { background: #d1edc4; }
.cal .cell-clean { background: #f0f9eb; }
.cal .cell-dirty { background: #fdf6ec; }
.cal .cell-closed { background: #f4f4f5; color: #bbb; }
.cal .guest { font-weight: 600; font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cal .tag { font-size: 10px; color: #666; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-left: 4px; vertical-align: middle; }
.dot.c-clean { background: #67c23a; }
.dot.c-dirty { background: #e6a23c; }
.dot.c-closed { background: #909399; }
.legend { margin-top: 10px; color: #666; }
.lg { display: inline-block; width: 14px; height: 14px; border: 1px solid #ddd; margin: 0 4px 0 12px; vertical-align: middle; }
.lg.cell-stay { background: #d9ecff; }
.lg.cell-res { background: #d1edc4; }
.lg.cell-clean { background: #f0f9eb; }
.lg.cell-dirty { background: #fdf6ec; }
.lg.cell-closed { background: #f4f4f5; }
</style>
