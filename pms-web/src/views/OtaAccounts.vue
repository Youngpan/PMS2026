<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>OTA 普通直连 · 登录态管理</span>
        <el-button type="primary" @click="openBind">添加 OTA 账号</el-button>
      </div>
    </template>

    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      支持<strong>携程</strong>、<strong>美团酒店</strong>、<strong>抖音</strong>账号密码直连。
      添加后自动登录，系统定时保活维持连接。
      <el-tag size="small" type="warning" style="margin-left:8px">• 偶发掉线将自动重连（≤5次退避）</el-tag>
      <el-tag size="small" type="success" style="margin-left:4px">• 掉线自动恢复成功率 ≥85%</el-tag>
    </el-alert>

    <el-table :data="accounts" border stripe v-loading="loading">
      <el-table-column label="平台" width="110">
        <template #default="{ row }">
          <el-tag :type="platformTag(row.platform)" style="width:100%;text-align:center;">{{ platformText(row.platform) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="accountName" label="账号" min-width="130" />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small" effect="dark">
            {{ statusText(row.status) }}
          </el-tag>
          <el-tooltip v-if="row.offlineReason" :content="row.offlineReason" placement="top">
            <el-icon style="margin-left:4px;cursor:pointer;color:#e6a23c"><WarningFilled /></el-icon>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="掉线/同步" width="180">
        <template #default="{ row }">
          <div v-if="row.status === 'OFFLINE' && row.offlineAt" style="font-size:12px;color:#f56c6c">
            掉线：{{ formatFullTime(row.offlineAt) }}
          </div>
          <div v-if="row.lastSyncAt" style="font-size:12px;color:#909399">
            上次同步：{{ formatTime(row.lastSyncAt) }}
          </div>
          <div v-else style="font-size:12px;color:#c0c4cc">未同步</div>
        </template>
      </el-table-column>
      <el-table-column label="自动重连" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'OFFLINE' && row.autoReconnect === 1" type="warning" size="small">
            重试 {{ row.retryCount || 0 }}/5
          </el-tag>
          <el-tag v-else-if="row.status === 'CONNECTED'" type="success" size="small">正常</el-tag>
          <span v-else style="color:#c0c4cc">—</span>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="70" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled === 1" @change="(v: any) => toggleEnabled(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340">
        <template #default="{ row }">
          <!-- FR-09：读取订单 -->
          <el-button size="small" type="primary" @click="fetchOrders(row)" :loading="row._fetching"
                     :disabled="row.status !== 'CONNECTED'">
            读取订单
          </el-button>
          <!-- FR-11：重新登录 -->
          <el-button size="small" type="warning" @click="reLogin(row)" :loading="row._relogin"
                     :disabled="row.status === 'DISCONNECTED'">
            重新登录
          </el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <!-- FR-12：解绑 -->
          <el-popconfirm title="确认解绑该 OTA 账号？解除后需重新绑定。" @confirm="unbind(row)">
            <template #reference>
              <el-button size="small" type="danger">解绑</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 绑定/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑 OTA 账号' : '添加 OTA 账号（首次绑定登录）'" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="平台" required>
          <el-select v-model="form.platform" placeholder="选择平台" style="width:100%">
            <el-option label="携程" value="CTRIP" />
            <el-option label="美团酒店" value="MEITUAN" />
            <el-option label="抖音（来客）" value="DOUYIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录账号" required>
          <el-input v-model="form.accountName" placeholder="OTA 平台商家登录账号" />
        </el-form-item>
        <el-form-item :label="editId ? '新密码' : '登录密码'" :required="!editId">
          <el-input v-model="form.password" type="password" show-password
                    :placeholder="editId ? '留空不修改' : 'OTA 平台登录密码'" />
        </el-form-item>
        <el-form-item label="商家ID">
          <el-input v-model="form.merchantId" placeholder="商家ID/门店ID（可选）" />
        </el-form-item>
        <el-form-item label="AppKey">
          <el-input v-model="form.appKey" placeholder="开放平台 AppKey（可选）" />
        </el-form-item>
        <el-form-item :label="editId ? '新AppSecret' : 'AppSecret'">
          <el-input v-model="form.appSecret" type="password" show-password
                    :placeholder="editId ? '留空不修改' : '开放平台 AppSecret（可选）'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 读取订单结果弹窗 -->
    <el-dialog v-model="ordersVisible" title="读取订单结果" width="480px">
      <p style="color:#67c23a">✅ 订单读取成功，已在后台同步更新房态。</p>
      <template #footer>
        <el-button type="primary" @click="ordersVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { otaApi } from '@/api/modules'

const accounts = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const ordersVisible = ref(false)
const editId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({
  platform: '',
  accountName: '',
  password: '',
  merchantId: '',
  appKey: '',
  appSecret: '',
})

function platformText(p: string) {
  return { CTRIP: '携程', MEITUAN: '美团', DOUYIN: '抖音' }[p] || p
}
function platformTag(p: string): any {
  return { CTRIP: 'primary', MEITUAN: 'warning', DOUYIN: 'danger' }[p] || ''
}
function statusText(s: string) {
  return { DISCONNECTED: '未连接', CONNECTED: '已登录', OFFLINE: '掉线', ERROR: '连接异常' }[s] || s
}
function statusTag(s: string): any {
  return { DISCONNECTED: 'info', CONNECTED: 'success', OFFLINE: 'danger', ERROR: 'danger' }[s] || 'info'
}
function formatTime(t: string) {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 16)
}
function formatFullTime(t: string) {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const data = await otaApi.list()
    accounts.value = (data || []).map((a: any) => ({ ...a, _fetching: false, _relogin: false }))
  } finally {
    loading.value = false
  }
}

function openBind() {
  editId.value = null
  Object.assign(form, { platform: '', accountName: '', password: '', merchantId: '', appKey: '', appSecret: '' })
  dialogVisible.value = true
}

function openEdit(row: any) {
  editId.value = row.id
  Object.assign(form, {
    platform: row.platform,
    accountName: row.accountName,
    password: '',
    merchantId: row.merchantId || '',
    appKey: row.appKey || '',
    appSecret: '',
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.platform) { ElMessage.warning('请选择平台'); return }
  if (!form.accountName) { ElMessage.warning('请输入账号'); return }
  if (!editId.value && !form.password) { ElMessage.warning('请输入密码'); return }
  saving.value = true
  try {
    if (editId.value) {
      await otaApi.update(editId.value, { ...form })
      ElMessage.success('已更新')
    } else {
      await otaApi.bind({ ...form })
      ElMessage.success('已添加并登录成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

/** FR-12：解绑 */
async function unbind(row: any) {
  await otaApi.unbind(row.id)
  ElMessage.success('已解绑，凭证已清除')
  load()
}

/** FR-11：重新登录 */
async function reLogin(row: any) {
  row._relogin = true
  try {
    const data = await otaApi.login(row.id)
    Object.assign(row, data)
    ElMessage.success(`${platformText(row.platform)} 重新登录成功`)
  } catch (e: any) {
    ElMessage.error(e.message || '重新登录失败')
  } finally {
    row._relogin = false
  }
}

/** FR-09：读取订单（手动即时同步） */
async function fetchOrders(row: any) {
  row._fetching = true
  try {
    const data = await otaApi.fetchOrders(row.id)
    Object.assign(row, data)
    ordersVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '读取订单失败')
  } finally {
    row._fetching = false
  }
}

async function toggleEnabled(row: any, v: any) {
  await otaApi.toggleEnabled(row.id, v ? 1 : 0)
  row.enabled = v ? 1 : 0
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
