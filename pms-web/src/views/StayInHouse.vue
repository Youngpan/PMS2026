<template>
  <div>
    <!-- 统计行 -->
    <el-row :gutter="12" class="stats-row">
      <el-col :span="6"><el-card shadow="never" class="stat-card"><div class="stat-val">{{ list.length }}</div><div class="stat-lbl">当前在住</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never" class="stat-card"><div class="stat-val">{{ todayIn }}</div><div class="stat-lbl">今日入住</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never" class="stat-card"><div class="stat-val">{{ todayOut }}</div><div class="stat-lbl">今日退房</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never" class="stat-card"><div class="stat-val">{{ todayExtend }}</div><div class="stat-lbl">今日续住</div></el-card></el-col>
    </el-row>

    <!-- 在住卡片 -->
    <el-row :gutter="12" class="card-grid">
      <el-col v-for="s in list" :key="s.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="stay-card" :class="{ hourly: s.type === 'HOURLY' }">
          <div class="sc-header" @click="openDetail(s)">
            <span class="sc-room">{{ s.roomNumber || '?' }}</span>
            <el-tag size="small" :type="s.type === 'HOURLY' ? 'warning' : 'success'" effect="plain">
              {{ s.type === 'HOURLY' ? '钟点房' : '在住' }}
            </el-tag>
          </div>
          <div class="sc-body">
            <div class="sc-guest">{{ s.guestName || '—' }}</div>
            <div class="sc-meta">
              房价 ¥{{ s.rate }} · {{ nightsSince(s.checkInAt) }} 晚
              <br>入住 {{ fmt(s.checkInAt) }}
            </div>
          </div>
          <div class="sc-actions">
            <el-button size="small" @click.stop="doExtend(s)">续住</el-button>
            <el-button size="small" @click.stop="doTransfer(s)">换房</el-button>
            <el-button size="small" @click.stop="doRate(s)">调价</el-button>
            <el-button size="small" type="warning" @click.stop="openFolio(s)">账单</el-button>
            <el-button size="small" type="danger" @click.stop="doCheckout(s)">退房</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col v-if="!list.length" :span="24">
        <el-empty description="当前无在住房间" />
      </el-col>
    </el-row>

    <!-- 在住详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="detailTitle" size="400px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 16px">
          <el-descriptions-item label="房号">{{ detail.roomNumber }}</el-descriptions-item>
          <el-descriptions-item label="宾客">{{ detail.guestName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detail.guestPhone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="房价">¥{{ detail.rate }}/晚</el-descriptions-item>
          <el-descriptions-item label="入住时间">{{ fmt(detail.checkInAt) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.type === 'HOURLY' ? '钟点房 ' + detail.hours + '小时' : '普通入住' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs>
          <el-tab-pane label="账单">
            <el-button size="small" @click="openFolio(detail)" style="margin-bottom:8px">查看完整账单</el-button>
            <el-table :data="folioItems" border stripe size="small" v-loading="folioLoading" max-height="280">
              <el-table-column prop="type" label="类型" width="80" />
              <el-table-column prop="category" label="项目" width="100" />
              <el-table-column prop="amount" label="金额" width="90" align="right" />
              <el-table-column prop="createdAt" label="时间" width="150" />
            </el-table>
            <div v-if="folioBalance !== null" class="folio-bal">
              余额：<span :style="{ color: folioBalance > 0 ? '#e6a23c' : '#67c23a' }">¥{{ folioBalance }}</span>
            </div>
          </el-tab-pane>
          <el-tab-pane label="续住记录">
            <el-table :data="detail.extensions || []" border stripe size="small">
              <el-table-column prop="extendToDate" label="续住至" width="130" />
              <el-table-column prop="remark" label="备注" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="换房记录">
            <el-table :data="detail.transfers || []" border stripe size="small">
              <el-table-column prop="createdAt" label="时间" width="150" />
              <el-table-column label="操作" width="200">
                <template #default="{ row }">{{ row.fromRoomId }} → {{ row.toRoomId }}</template>
              </el-table-column>
              <el-table-column prop="reason" label="原因" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 续住 dialog -->
    <el-dialog v-model="extendVisible" title="续住" width="420px">
      <el-form :model="extendForm" label-width="90px">
        <el-form-item label="房号"><el-tag>{{ extendTarget?.roomNumber }}</el-tag></el-form-item>
        <el-form-item label="续住天数"><el-input-number v-model="extendForm.days" :min="1" :max="30" /></el-form-item>
        <el-form-item label="收款方式">
          <el-select v-model="extendForm.method">
            <el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" /><el-option label="银行卡" value="CARD" />
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

    <!-- 换房 dialog -->
    <el-dialog v-model="transferVisible" title="换房" width="420px">
      <el-form :model="transferForm" label-width="90px">
        <el-form-item label="当前房号"><el-tag>{{ transferTarget?.roomNumber }}</el-tag></el-form-item>
        <el-form-item label="目标房号">
          <el-select v-model="transferForm.to_room_number" filterable placeholder="输入房号搜索" style="width:100%">
            <el-option v-for="r in vacantRooms" :key="r.id" :label="r.number + '（' + roomTypeName(r.roomTypeId) + '）'"
                       :value="r.number" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="transferForm.reason" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTransfer">确认换房</el-button>
      </template>
    </el-dialog>

    <!-- 调价 dialog -->
    <el-dialog v-model="rateVisible" title="调整房价" width="360px">
      <el-form label-width="80px">
        <el-form-item label="房号"><el-tag>{{ rateTarget?.roomNumber }}</el-tag></el-form-item>
        <el-form-item label="当前房价">¥{{ rateTarget?.rate }}</el-form-item>
        <el-form-item label="新房价"><el-input-number v-model="rateForm.rate" :min="0" :precision="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rateVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRate">确认调价</el-button>
      </template>
    </el-dialog>

    <!-- 退房 dialog -->
    <el-dialog v-model="checkoutVisible" title="退房结账" width="480px">
      <el-form label-width="100px">
        <el-form-item label="在住房号"><el-tag>{{ checkoutTarget?.roomNumber }}</el-tag></el-form-item>
        <el-form-item label="宾客姓名">{{ checkoutTarget?.guestName || '—' }}</el-form-item>
        <el-form-item label="已住晚数">{{ nightsSince(checkoutTarget?.checkInAt) }}</el-form-item>
        <el-form-item label="账单余额">
          <span :style="{ color: checkoutBalance > 0 ? '#e6a23c' : '#67c23a', fontWeight: 600 }">
            ¥{{ checkoutBalance }}
          </span>
        </el-form-item>
        <el-form-item label="收款方式">
          <el-select v-model="checkoutForm.method">
            <el-option label="现金" value="CASH" /><el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" /><el-option label="银行卡" value="CARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="实收金额">
          <el-input-number v-model="checkoutForm.amount" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="submitCheckout">确认退房</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { stayApi, folioApi, roomApi, housekeepingApi } from '@/api/modules'

const router = useRouter()
const list = ref<any[]>([])
const saving = ref(false)

// 房间类型映射
const roomTypeMap = ref<Record<number, any>>({})
const vacantRooms = ref<any[]>([])

async function loadRoomTypes() {
  const types: any[] = await roomApi.types()
  roomTypeMap.value = Object.fromEntries(types.map((t) => [t.id, t]))
}
function roomTypeName(id: number) { return roomTypeMap.value[id]?.name || '' }

async function load() {
  list.value = (await stayApi.inHouse()) as any[]
  await loadRoomTypes()
  try {
    vacantRooms.value = (await roomApi.vacant()) as any[]
  } catch {}
}

// ---- 统计 ----
const todayStr = new Date().toISOString().slice(0, 10)
const todayIn = computed(() => list.value.filter((s) => String(s.checkInAt).startsWith(todayStr)).length)
const todayOut = computed(() => 0) // checkout 后就被移除出 list，无法统计。实际可访问接口
const todayExtend = computed(() => 0)

function fmt(s: string) { return s ? String(s).replace('T', ' ').slice(0, 16) : '—' }
function nightsSince(s: string) {
  if (!s) return 0
  const ci = new Date(s), now = new Date()
  return Math.max(1, Math.round((now.getTime() - ci.getTime()) / 86400000))
}

// ---- 详情抽屉 ----
const detailVisible = ref(false)
const detail = ref<any>(null)
const detailTitle = computed(() => detail.value ? `房号 ${detail.value.roomNumber} · 在住详情` : '')

const folioItems = ref<any[]>([])
const folioBalance = ref<number | null>(null)
const folioLoading = ref(false)

async function openDetail(s: any) {
  detail.value = s
  detailVisible.value = true
  await loadFolio(s)
  // 加载续住/换房记录（若无后端返回则忽略）
  if (s.extensions === undefined) s.extensions = []
  if (s.transfers === undefined) s.transfers = []
}

async function loadFolio(s: any) {
  folioLoading.value = true
  try {
    const f: any = await folioApi.byStay(s.id)
    folioItems.value = (f as any).items || []
    folioBalance.value = (f as any).folio?.balance ?? null
  } catch {
    folioItems.value = []
    folioBalance.value = null
  } finally { folioLoading.value = false }
}

// ---- 打开完整账单 ----
async function openFolio(s: any) {
  try {
    const f: any = await folioApi.byStay(s.id)
    const folioId = (f as any).folio?.id
    if (folioId) router.push(`/folios/${folioId}`)
    else ElMessage.warning('无账单')
  } catch { ElMessage.warning('加载账单失败') }
}

// ---- 续住 ----
const extendVisible = ref(false)
const extendTarget = ref<any>(null)
const extendForm = reactive({ days: 1, method: 'CASH', amount: 0, remark: '' })

function doExtend(s: any) {
  extendTarget.value = s
  extendForm.days = 1
  extendForm.method = 'CASH'
  extendForm.amount = 0
  extendForm.remark = ''
  // 关闭详情
  detailVisible.value = false
  extendVisible.value = true
}
async function submitExtend() {
  saving.value = true
  try {
    await stayApi.extend(extendTarget.value!.id, {
      days: extendForm.days,
      method: extendForm.method,
      amount: extendForm.amount || undefined,
      remark: extendForm.remark || undefined,
    })
    ElMessage.success('已续住')
    extendVisible.value = false
    await load()
  } finally { saving.value = false }
}

// ---- 换房 ----
const transferVisible = ref(false)
const transferTarget = ref<any>(null)
const transferForm = reactive({ to_room_number: '', reason: '' })

async function doTransfer(s: any) {
  transferTarget.value = s
  transferForm.to_room_number = ''
  transferForm.reason = ''
  // 刷新可换房列表（搬空净房）
  try { vacantRooms.value = (await roomApi.vacant()) as any[] } catch {}
  detailVisible.value = false
  transferVisible.value = true
}
async function submitTransfer() {
  const target = vacantRooms.value.find((r: any) => r.number === transferForm.to_room_number)
  if (!target) { ElMessage.warning('房号不存在或不在可选列表'); return }
  saving.value = true
  try {
    await stayApi.transferRoom(transferTarget.value!.id, target.id, transferForm.reason || '前台换房')
    ElMessage.success('已换房')
    transferVisible.value = false
    await load()
  } finally { saving.value = false }
}

// ---- 调价 ----
const rateVisible = ref(false)
const rateTarget = ref<any>(null)
const rateForm = reactive({ rate: 0 })

function doRate(s: any) {
  rateTarget.value = s
  rateForm.rate = Number(s.rate ?? 0)
  detailVisible.value = false
  rateVisible.value = true
}
async function submitRate() {
  if (rateForm.rate < 0) { ElMessage.warning('房价不能为负'); return }
  saving.value = true
  try {
    await stayApi.updateRate(rateTarget.value!.id, rateForm.rate)
    ElMessage.success('房价已调整')
    rateVisible.value = false
    await load()
  } finally { saving.value = false }
}

// ---- 退房 ----
const checkoutVisible = ref(false)
const checkoutTarget = ref<any>(null)
const checkoutForm = reactive({ method: 'WECHAT', amount: 0 })
const checkoutBalance = ref(0)

async function doCheckout(s: any) {
  checkoutTarget.value = s
  checkoutForm.method = 'WECHAT'
  checkoutForm.amount = 0
  // 获取当前余额
  try {
    const f: any = await folioApi.byStay(s.id)
    checkoutBalance.value = (f as any).folio?.balance ?? 0
  } catch { checkoutBalance.value = 0 }
  detailVisible.value = false
  checkoutVisible.value = true
}
async function submitCheckout() {
  if (!checkoutForm.amount && checkoutBalance.value > 0) {
    ElMessage.warning('账单余额不为零，请填写实收金额')
    return
  }
  saving.value = true
  try {
    await stayApi.checkout(checkoutTarget.value!.id, checkoutForm.method, checkoutForm.amount || undefined)
    ElMessage.success('已退房' + (checkoutBalance.value > 0 ? '，账单已结清' : ''))
    checkoutVisible.value = false
    await load()
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-val { font-size: 26px; font-weight: 700; color: #409eff; }
.stat-lbl { font-size: 12px; color: #909399; margin-top: 2px; }
.card-grid .el-card { margin-bottom: 12px; }
.stay-card { cursor: default; }
.stay-card.hourly { border-left: 3px solid #e6a23c; }
.sc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; cursor: pointer; }
.sc-header:hover .sc-room { color: #409eff; }
.sc-room { font-size: 22px; font-weight: 700; color: #303133; transition: color 0.15s; }
.sc-guest { font-size: 14px; font-weight: 600; margin-bottom: 4px; }
.sc-meta { font-size: 12px; color: #909399; line-height: 1.6; }
.sc-actions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; border-top: 1px solid #ebeef5; padding-top: 10px; }
.sc-actions .el-button { flex: 1 0 auto; min-width: 50px; }
.folio-bal { margin-top: 8px; font-size: 14px; font-weight: 600; text-align: right; }
</style>
