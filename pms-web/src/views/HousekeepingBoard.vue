<template>
  <div class="hk-wrap">
    <!-- Stats row -->
    <el-row :gutter="12" class="stats-row">
      <el-col :span="6"><div class="stat-box"><div class="stat-num">{{ stats.occupied }}</div><div class="stat-lbl">在住</div></div></el-col>
      <el-col :span="6"><div class="stat-box"><div class="stat-num" style="color:#e6a23c">{{ stats.dirty }}</div><div class="stat-lbl">脏房</div></div></el-col>
      <el-col :span="6"><div class="stat-box"><div class="stat-num" style="color:#f56c6c">{{ stats.fixing }}</div><div class="stat-lbl">维修</div></div></el-col>
      <el-col :span="6"><div class="stat-box"><div class="stat-num" style="color:#67c23a">{{ stats.clean }}</div><div class="stat-lbl">可售</div></div></el-col>
    </el-row>

    <!-- Filters -->
    <div class="filter-bar">
      <el-select v-model="filterFloor" clearable placeholder="全部楼层" style="width:140px" @change="applyFilter">
        <el-option v-for="f in floorList" :key="f" :label="f + ' 楼'" :value="f" />
      </el-select>
      <el-select v-model="filterType" clearable placeholder="全部房型" style="width:160px" @change="applyFilter">
        <el-option v-for="t in roomTypes" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <el-select v-model="filterStatus" clearable placeholder="全部状态" style="width:150px" @change="applyFilter">
        <el-option label="净房" value="CLEAN" />
        <el-option label="脏房" value="DIRTY" />
        <el-option label="在住" value="IN" />
        <el-option label="已排房" value="ASSIGNED" />
        <el-option label="关闭" value="CLOSED" />
        <el-option label="维修中" value="MAINT" />
      </el-select>
      <span class="filter-result">{{ filteredFloors.reduce((s, f) => s + f.rooms.length, 0) }} 间</span>
      <div style="flex:1" />
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <!-- Legend -->
    <div class="legend">
      <span class="lg cell-in"></span>在住
      <span class="lg cell-reserve"></span>预订占用
      <span class="lg cell-clean"></span>净房
      <span class="lg cell-dirty"></span>脏房
      <span class="lg cell-closed"></span>关闭
      <span class="lg cell-maint"></span>维修
      <span style="margin-left:12px; color:#e6a23c">⬡ 待清扫</span>
      <span style="margin-left:12px; color:#f56c6c">⬡ 报修中</span>
    </div>

    <!-- Room grid -->
    <div class="grid-wrap">
      <template v-for="fb in filteredFloors" :key="fb.floor">
        <div class="floor-title"><el-tag size="small" effect="plain">{{ fb.floor }} F</el-tag></div>
        <div class="room-row">
          <div v-for="r in fb.rooms" :key="r.id" class="room-card"
            :class="cardClass(r)" @click="onCardClick(r)"
            :title="cardTitle(r)">
            <div class="rc-number">{{ r.number }}</div>
            <div v-if="guestMap[r.id]" class="rc-guest">{{ guestMap[r.id].slice(0, 2) }}</div>
            <div class="rc-type">{{ rtName(r.roomTypeId) }}</div>
            <div class="rc-badges">
              <span v-if="pendingTasks[r.id]" class="b b-task">清</span>
              <span v-if="openFixes[r.id]" class="b b-fix">修</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>

  <!-- ─── 以下所有对话框保持不变 ─── -->

  <!-- 房务面板：点卡片打开 -->
  <el-dialog v-model="panelVisible" :title="panelTitle" width="470px">
    <template v-if="panelRoom">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="房型">{{ rtName(panelRoom.roomTypeId) }}</el-descriptions-item>
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
          <div class="panel-line">{{ taskTypeText(panelTask.type) }} · 派单 {{ fmtTime(panelTask.createdAt) }}</div>
          <el-button size="small" type="success" @click="doFinishTask">完成清扫（转净房）</el-button>
        </template>
        <div v-else class="panel-muted">无进行中任务</div>
      </div>

      <div class="panel-block">
        <div class="panel-block-title">维修单</div>
        <template v-if="panelFix">
          <div class="panel-line">{{ panelFix.issue }} · 报修 {{ fmtTime(panelFix.createdAt) }}</div>
          <el-button size="small" type="success" @click="doFixMaintenance">修复（恢复可售）</el-button>
        </template>
        <div v-else class="panel-muted">无未完成报修</div>
      </div>

      <div class="panel-actions">
        <el-button type="primary" :disabled="!!panelTask" @click="openTaskDialog">派清扫单</el-button>
        <el-button type="danger" :disabled="!!panelFix" @click="openRepairDialog">报修</el-button>
        <el-button v-if="!guestMap[panelRoom.id] && panelRoom.rentStatus !== 'CLOSED'" type="success" @click="openHourly(panelRoom)">钟点房</el-button>
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
    <template #footer><el-button @click="taskVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitTask">派单</el-button></template>
  </el-dialog>

  <!-- 报修 -->
  <el-dialog v-model="repairVisible" title="报修" width="440px">
    <el-form label-width="80px">
      <el-form-item label="房号"><el-tag>{{ panelRoom?.number }}</el-tag></el-form-item>
      <el-form-item label="故障描述"><el-input v-model="repairForm.issue" type="textarea" :rows="3" placeholder="如：空调不制冷、马桶漏水" /></el-form-item>
    </el-form>
    <div class="tip-line">报修后该房置为「关闭」不可售，修复后自动恢复可售。</div>
    <template #footer><el-button @click="repairVisible = false">取消</el-button><el-button type="danger" :loading="saving" @click="submitRepair">提交报修</el-button></template>
  </el-dialog>

  <!-- 钟点房 -->
  <el-dialog v-model="hourlyVisible" title="钟点房" width="420px">
    <el-form :model="hourlyForm" label-width="90px">
      <el-form-item label="房号"><el-tag>{{ hourlyForm.roomNumber }}</el-tag></el-form-item>
      <el-form-item label="客人姓名"><el-input v-model="hourlyForm.guestName" /></el-form-item>
      <el-form-item label="电话"><el-input v-model="hourlyForm.guestPhone" /></el-form-item>
      <el-form-item label="时长(小时)"><el-input-number v-model="hourlyForm.hours" :min="1" /></el-form-item>
      <el-form-item label="小时单价"><el-input-number v-model="hourlyForm.hourlyRate" :min="0" :precision="2" /></el-form-item>
      <el-form-item label="收款方式"><el-select v-model="hourlyForm.method"><el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" /><el-option label="银行卡" value="CARD" /><el-option label="支付宝" value="ALIPAY" /></el-select></el-form-item>
      <el-form-item label="实收金额"><el-input-number v-model="hourlyForm.amount" :min="0" :precision="2" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="hourlyVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitHourly">确认入住</el-button></template>
  </el-dialog>

  <!-- 续住 -->
  <el-dialog v-model="extendVisible" title="续住" width="420px">
    <el-form :model="extendForm" label-width="90px">
      <el-form-item label="续住天数"><el-input-number v-model="extendForm.days" :min="1" /></el-form-item>
      <el-form-item label="收款方式"><el-select v-model="extendForm.method"><el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" /><el-option label="银行卡" value="CARD" /><el-option label="支付宝" value="ALIPAY" /></el-select></el-form-item>
      <el-form-item label="实收金额"><el-input-number v-model="extendForm.amount" :min="0" :precision="2" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="extendForm.remark" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="extendVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitExtend">确认续住</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { housekeepingApi, roomApi, stayApi, systemApi } from '@/api/modules'

const saving = ref(false)
const roomTypes = ref<any[]>([])
const floorFloors = ref<any[]>([])   // raw dashboard data
const pendingTasks = ref<Record<number, any>>({})
const openFixes = ref<Record<number, any>>({})
const guestMap = ref<Record<number, string>>({})
const roomTypeMap = ref<Record<number, any>>({})

// Filters
const filterFloor = ref<number | undefined>()
const filterType = ref<number | undefined>()
const filterStatus = ref<string>()

const floorList = computed(() => [...new Set(floorFloors.value.map((f) => f.floor))].sort())
const filteredFloors = computed(() => {
  let list = floorFloors.value
  if (filterFloor.value != null) list = list.filter((f) => f.floor === filterFloor.value)
  return list.map((fb: any) => ({
    floor: fb.floor,
    rooms: fb.rooms.filter((r: any) => {
      if (filterType.value != null && r.roomTypeId !== filterType.value) return false
      if (filterStatus.value === 'IN' && !guestMap.value[r.id]) return false
      if (filterStatus.value === 'ASSIGNED' && guestMap.value[r.id]) return false
      if (filterStatus.value === 'CLEAN' && !(r.rentStatus === 'OPEN' && r.cleanStatus === 'CLEAN' && !guestMap.value[r.id])) return false
      if (filterStatus.value === 'DIRTY' && !(r.rentStatus === 'OPEN' && r.cleanStatus === 'DIRTY' && !guestMap.value[r.id])) return false
      if (filterStatus.value === 'CLOSED' && !(r.rentStatus === 'CLOSED' && !openFixes.value[r.id])) return false
      if (filterStatus.value === 'MAINT' && !openFixes.value[r.id]) return false
      return true
    }),
  })).filter((fb) => fb.rooms.length > 0)
})

// Stats
const stats = computed(() => {
  const all = floorFloors.value.flatMap((f: any) => f.rooms)
  return {
    occupied: all.filter((r: any) => guestMap.value[r.id]).length,
    dirty: all.filter((r: any) => r.rentStatus === 'OPEN' && r.cleanStatus === 'DIRTY' && !guestMap.value[r.id]).length,
    fixing: all.filter((r: any) => openFixes.value[r.id]).length,
    clean: all.filter((r: any) => r.rentStatus === 'OPEN' && r.cleanStatus === 'CLEAN' && !guestMap.value[r.id]).length,
  }
})

function rtName(id: number) { return roomTypeMap.value[id]?.name || '' }

/** 房间卡片的 CSS class */
function cardClass(r: any) {
  if (openFixes.value[r.id]) return 'cc-maint'
  if (guestMap.value[r.id]) return 'cc-in'
  if (r.rentStatus === 'CLOSED') return 'cc-closed'
  if (r.cleanStatus === 'DIRTY') return 'cc-dirty'
  return 'cc-clean'
}
function cardTitle(r: any) {
  const parts = [r.number, rtName(r.roomTypeId)]
  if (guestMap.value[r.id]) parts.push(`在住：${guestMap.value[r.id]}`)
  if (pendingTasks.value[r.id]) parts.push('· 待清扫')
  return parts.join(' ')
}

function applyFilter() {}

async function load() {
  const [dash, types, stays, tasks, fixes] = await Promise.all([
    housekeepingApi.dashboard(),
    roomApi.types(),
    stayApi.inHouse(),
    housekeepingApi.tasks({ status: 'PENDING' }),
    housekeepingApi.maintenances({ status: 'OPEN' }),
  ])
  floorFloors.value = (dash as any[]) || []
  roomTypes.value = (types as any[]) || []
  roomTypeMap.value = Object.fromEntries(roomTypes.value.map((t: any) => [t.id, t]))
  pendingTasks.value = Object.fromEntries((tasks as any[]).map((t: any) => [t.roomId, t]))
  openFixes.value = Object.fromEntries((fixes as any[]).map((m: any) => [m.roomId, m]))
  guestMap.value = Object.fromEntries(
    (stays as any[]).filter((s: any) => s.status === 'IN').map((s: any) => [s.roomId, s.guestName || '']),
  )
}

// ── 房务面板 ──
const panelVisible = ref(false)
const panelRoom = ref<any>(null)
const panelTask = computed(() => (panelRoom.value ? pendingTasks.value[panelRoom.value.id] : null))
const panelFix = computed(() => (panelRoom.value ? openFixes.value[panelRoom.value.id] : null))
const panelTitle = computed(() => (panelRoom.value ? `房号 ${panelRoom.value.number} · ${rtName(panelRoom.value.roomTypeId)}` : ''))

const staffList = ref<any[]>([])
const taskVisible = ref(false)
const taskForm = reactive({ type: 'DAILY', assigneeId: undefined as number | undefined })
const repairVisible = ref(false)
const repairForm = reactive({ issue: '' })

function onCardClick(r: any) {
  panelRoom.value = r
  panelVisible.value = true
}

async function refreshAll() {
  const id = panelRoom.value?.id
  await load()
  if (id) {
    for (const fb of floorFloors.value) {
      const found = fb.rooms.find((x: any) => x.id === id)
      if (found) { panelRoom.value = found; break }
    }
  }
}

function taskTypeText(t: string) { return { DAILY: '日常清扫', CHECKOUT_CLEAN: '退房清扫', DEEP_CLEAN: '深度清洁' }[t] || t }
function rentText(s: string) { return { OPEN: '打开', CLOSED: '关闭', ASSIGNED: '已排房' }[s] || s }
function fmtTime(s: string) { return s ? String(s).replace('T', ' ').slice(0, 16) : '—' }

function openRepairDialog() { repairVisible.value = true }

async function openTaskDialog() {
  if (!staffList.value.length) { try { staffList.value = (await systemApi.staff()) as any[] } catch {} }
  taskForm.type = panelRoom.value?.cleanStatus === 'DIRTY' ? 'CHECKOUT_CLEAN' : 'DAILY'
  taskForm.assigneeId = undefined
  taskVisible.value = true
}
async function submitTask() {
  if (!panelRoom.value) return
  saving.value = true
  try {
    await housekeepingApi.createTask({ room_id: panelRoom.value.id, type: taskForm.type, assignee_id: taskForm.assigneeId ?? null })
    ElMessage.success('已派单'); taskVisible.value = false; await refreshAll()
  } finally { saving.value = false }
}
async function doFinishTask() {
  if (!panelTask.value) return
  await housekeepingApi.finishTask(panelTask.value.id)
  ElMessage.success('已完成清扫，房间转净房'); await refreshAll()
}
async function submitRepair() {
  if (!panelRoom.value) return
  if (!repairForm.issue.trim()) { ElMessage.warning('请填写故障描述'); return }
  saving.value = true
  try {
    await housekeepingApi.reportMaintenance({ room_id: panelRoom.value.id, issue: repairForm.issue.trim() })
    ElMessage.success('已报修，房间已置为不可售'); repairVisible.value = false; await refreshAll()
  } finally { saving.value = false }
}
async function doFixMaintenance() {
  if (!panelFix.value) return
  await housekeepingApi.fixMaintenance(panelFix.value.id)
  ElMessage.success('已修复，房间恢复可售'); await refreshAll()
}

// ── 钟点房 ──
const hourlyVisible = ref(false)
const hourlyForm = reactive({ roomId: 0, roomNumber: '', guestName: '', guestPhone: '', hours: 3, hourlyRate: 50, method: 'CASH', amount: 0 })

function openHourly(r: any) {
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
      room_id: hourlyForm.roomId, guest_name: hourlyForm.guestName, guest_phone: hourlyForm.guestPhone,
      hours: hourlyForm.hours, hourly_rate: hourlyForm.hourlyRate, method: hourlyForm.method, amount: hourlyForm.amount || undefined,
    })
    ElMessage.success('钟点房已入住'); hourlyVisible.value = false; await load()
  } finally { saving.value = false }
}

// ── 续住 ──
const extendVisible = ref(false)
const extendForm = reactive({ stayId: 0, days: 1, method: 'CASH', amount: 0, remark: '' })
async function submitExtend() {
  saving.value = true
  try {
    await stayApi.extend(extendForm.stayId, { days: extendForm.days, amount: extendForm.amount || undefined, method: extendForm.method, remark: extendForm.remark || undefined })
    ElMessage.success('已续住'); extendVisible.value = false; await load()
  } finally { saving.value = false }
}

// ── WS ──
let ws: WebSocket | null = null
let reconnectTimer: any = null
function connectWs() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  ws = new WebSocket(`${proto}://${location.host}/api/v1/ws/room-status`)
  ws.onmessage = (ev) => { try { const msg = JSON.parse(ev.data); if (msg.type === 'room-status') load() } catch {} }
  ws.onclose = () => { reconnectTimer = setTimeout(connectWs, 3000) }
}
onMounted(() => { load(); connectWs() })
onUnmounted(() => { if (reconnectTimer) clearTimeout(reconnectTimer); if (ws) ws.close() })
</script>

<style scoped>
.hk-wrap { min-height: 0; }

/* ── Stats ── */
.stats-row { margin-bottom: 14px; }
.stat-box { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 14px 0; text-align: center; }
.stat-num { font-size: 28px; font-weight: 700; color: #409eff; }
.stat-lbl { font-size: 12px; color: #909399; margin-top: 2px; }

/* ── Filters ── */
.filter-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; background: #fff; border-radius: 8px; padding: 10px 14px; box-shadow: 0 1px 2px rgba(0,0,0,.04); }
.filter-result { font-size: 13px; color: #909399; margin-left: 4px; }

/* ── Legend ── */
.legend { font-size: 12px; color: #666; margin-bottom: 10px; }
.lg { display: inline-block; width: 14px; height: 14px; border-radius: 4px; margin: 0 3px 0 10px; vertical-align: middle; border: 1px solid rgba(0,0,0,.06); }
.lg.cell-in { background: #409eff; }
.lg.cell-reserve { background: #67c23a; }
.lg.cell-clean { background: #fff; border-color: #c0c4cc; }
.lg.cell-dirty { background: #fdf6ec; border-color: #e6a23c; }
.lg.cell-closed { background: #e8eaed; border-color: #c0c4cc; }
.lg.cell-maint { background: #f56c6c; }

/* ── Grid ── */
.grid-wrap { background: #fff; border-radius: 8px; padding: 12px 16px; box-shadow: 0 1px 2px rgba(0,0,0,.04); }
.floor-title { margin: 14px 0 6px; }
.floor-title:first-child { margin-top: 0; }
.room-row { display: flex; flex-wrap: wrap; gap: 10px; }

.room-card {
  width: 110px;
  border-radius: 8px;
  padding: 10px 8px 8px;
  cursor: pointer;
  text-align: center;
  transition: box-shadow .15s, transform .12s;
  position: relative;
  border: 1.5px solid transparent;
}
.room-card:hover { box-shadow: 0 2px 10px rgba(0,0,0,.12); transform: translateY(-2px); }

/* Color variants */
.cc-clean { background: #fff; border-color: #dcdfe6; }
.cc-dirty { background: #fff6e5; border-color: #e6a23c; }
.cc-in { background: #e6f1ff; border-color: #409eff; }
.cc-closed { background: #f5f5f5; border-color: #c0c4cc; }
.cc-closed .rc-number { color: #909399; }
.cc-maint { background: #fef0f0; border-color: #f56c6c; }

.rc-number { font-size: 22px; font-weight: 700; color: #303133; line-height: 1.2; }
.rc-guest { font-size: 14px; font-weight: 600; color: #409eff; margin-top: 2px; }
.rc-type { font-size: 11px; color: #909399; margin-top: 2px; }
.rc-badges { display: flex; justify-content: center; gap: 4px; margin-top: 4px; height: 18px; }

.b {
  display: inline-block; width: 18px; height: 18px; line-height: 18px;
  border-radius: 50%; font-size: 10px; color: #fff; font-weight: 700; text-align: center;
}
.b-task { background: #e6a23c; }
.b-fix { background: #f56c6c; }

/* ── Panel ── */
.panel-block { margin-top: 14px; border-top: 1px dashed #ebeef5; padding-top: 10px; }
.panel-block-title { font-weight: 600; font-size: 13px; margin-bottom: 6px; }
.panel-line { font-size: 12px; color: #606266; margin-bottom: 8px; }
.panel-muted { font-size: 12px; color: #c0c4cc; }
.panel-actions { display: flex; gap: 10px; margin-top: 16px; }
.tip-line { font-size: 12px; color: #e6a23c; margin-top: 4px; }
</style>
