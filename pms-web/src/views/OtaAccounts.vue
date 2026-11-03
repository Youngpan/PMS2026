<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>OTA 账号直连</span>
        <el-button type="primary" @click="openCreate">新增 OTA 账号</el-button>
      </div>
    </template>

    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      支持携程、美团、抖音账号密码登录。添加账号后点击"测试连接"验证，连接成功后可"同步订单"。
      <span style="color:#909399">（当前为框架对接，真实 API 待各平台开放授权后接入）</span>
    </el-alert>

    <el-table :data="accounts" border stripe v-loading="loading">
      <el-table-column label="平台" width="120">
        <template #default="{ row }">
          <el-tag :type="platformTag(row.platform)">{{ platformText(row.platform) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="accountName" label="账号" min-width="140" />
      <el-table-column prop="merchantId" label="商家ID" width="140" />
      <el-table-column label="连接状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近同步" width="170">
        <template #default="{ row }">
          <span v-if="row.lastSyncAt">{{ formatTime(row.lastSyncAt) }}</span>
          <span v-else style="color:#c0c4cc">—</span>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled === 1" @change="(v: any) => toggleEnabled(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="testConnection(row)" :loading="row._testing">测试连接</el-button>
          <el-button size="small" type="primary" @click="sync(row)" :loading="row._syncing">同步</el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑 OTA 账号' : '新增 OTA 账号'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="平台" required>
          <el-select v-model="form.platform" placeholder="选择平台" style="width:100%">
            <el-option label="携程" value="CTRIP" />
            <el-option label="美团" value="MEITUAN" />
            <el-option label="抖音" value="DOUYIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录账号" required>
          <el-input v-model="form.accountName" placeholder="OTA 平台登录账号" />
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
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { otaApi } from '@/api/modules'

const accounts = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
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
  return { DISCONNECTED: '未连接', CONNECTED: '已连接', ERROR: '连接异常' }[s] || s
}
function statusTag(s: string): any {
  return { DISCONNECTED: 'info', CONNECTED: 'success', ERROR: 'danger' }[s] || 'info'
}
function formatTime(t: string) {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    const data = await otaApi.list()
    accounts.value = (data || []).map((a: any) => ({ ...a, _testing: false, _syncing: false }))
  } finally {
    loading.value = false
  }
}

function openCreate() {
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
      await otaApi.create({ ...form })
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除 ${platformText(row.platform)} 账号 ${row.accountName}？`, '删除', { type: 'warning' })
  await otaApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

async function toggleEnabled(row: any, v: any) {
  await otaApi.toggleEnabled(row.id, v ? 1 : 0)
  row.enabled = v ? 1 : 0
}

async function testConnection(row: any) {
  row._testing = true
  try {
    const data = await otaApi.testConnection(row.id)
    Object.assign(row, data)
    if (data.status === 'CONNECTED') {
      ElMessage.success(`${platformText(row.platform)} 连接成功`)
    } else {
      ElMessage.error(`连接失败：${data.lastError || '未知错误'}`)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '测试连接失败')
  } finally {
    row._testing = false
  }
}

async function sync(row: any) {
  if (row.status !== 'CONNECTED') {
    ElMessage.warning('请先测试连接')
    return
  }
  row._syncing = true
  try {
    const data = await otaApi.sync(row.id)
    Object.assign(row, data)
    ElMessage.success('同步完成')
  } catch (e: any) {
    ElMessage.error(e.message || '同步失败')
  } finally {
    row._syncing = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
