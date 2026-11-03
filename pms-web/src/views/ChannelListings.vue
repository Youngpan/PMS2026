<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>渠道房源关联</span>
        <el-button type="primary" @click="openBind">新建关联</el-button>
      </div>
    </template>

    <el-alert type="info" :closable="false" style="margin-bottom:12px">
      将渠道房源关联到百居易的房间或房型。<strong>独立房源</strong>关联到房间，<strong>多库存房源</strong>关联到房型。
      关联后以本地房态为权威源，自动同步库存与关房。
    </el-alert>

    <el-table :data="listings" border stripe v-loading="loading" row-key="id">
      <el-table-column label="平台" width="100">
        <template #default="{ row }">
          <el-tag :type="platformTag(row.platform)" size="small" style="width:100%;text-align:center;">
            {{ platformText(row.platform) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="渠道房源" min-width="180">
        <template #default="{ row }">
          <div>{{ row.channelListingName || row.channelListingId }}</div>
          <div style="font-size:12px;color:#909399">ID: {{ row.channelListingId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="关联到" width="200">
        <template #default="{ row }">
          <el-tag v-if="row.targetType === 'ROOM'" type="success" size="small">房间</el-tag>
          <el-tag v-else type="warning" size="small">房型</el-tag>
          <span style="margin-left:6px">{{ row.targetType === 'ROOM' ? roomText(row.roomId) : roomTypeText(row.roomTypeId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="库存类型" width="120">
        <template #default="{ row }">
          {{ row.inventoryType === 'INDEPENDENT' ? '独立房源' : '多库存' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '已关联' : '已解除' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-popconfirm title="确认解除该关联？解除后房态不再自动同步。" @confirm="unbind(row)">
            <template #reference>
              <el-button size="small" type="danger" :disabled="row.status !== 'ACTIVE'">解除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建关联弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建渠道房源关联" width="520px">
      <el-form :model="form" label-width="130px">
        <el-form-item label="OTA 账号" required>
          <el-select v-model="form.otaAccountId" placeholder="选择 OTA 账号" style="width:100%">
            <el-option v-for="a in otaAccounts" :key="a.id"
              :label="platformText(a.platform) + ' - ' + a.accountName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道房源ID" required>
          <el-input v-model="form.channelListingId" placeholder="渠道侧的房源/产品ID" />
        </el-form-item>
        <el-form-item label="渠道房源名称">
          <el-input v-model="form.channelListingName" placeholder="渠道侧的房源名称（可选）" />
        </el-form-item>
        <el-form-item label="关联类型" required>
          <el-radio-group v-model="form.targetType">
            <el-radio value="ROOM">关联到房间（独立房源）</el-radio>
            <el-radio value="ROOM_TYPE">关联到房型（多库存房源）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'ROOM'" label="选择房间" required>
          <el-select v-model="form.roomId" placeholder="选择房间" filterable style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="r.number" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'ROOM_TYPE'" label="选择房型" required>
          <el-select v-model="form.roomTypeId" placeholder="选择房型" style="width:100%">
            <el-option v-for="t in roomTypes" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存类型">
          <el-radio-group v-model="form.inventoryType">
            <el-radio value="INDEPENDENT">独立房源（库存=1）</el-radio>
            <el-radio value="MULTI_STOCK">多库存房源（库存>1）</el-radio>
          </el-radio-group>
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
import { ElMessage } from 'element-plus'
import { listingApi, otaApi, roomApi } from '@/api/modules'

const listings = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const otaAccounts = ref<any[]>([])
const rooms = ref<any[]>([])
const roomTypes = ref<any[]>([])

const form = reactive({
  otaAccountId: null as number | null,
  channelListingId: '',
  channelListingName: '',
  targetType: 'ROOM',
  roomId: null as number | null,
  roomTypeId: null as number | null,
  inventoryType: 'INDEPENDENT',
})

function platformText(p: string) {
  return { CTRIP: '携程', MEITUAN: '美团', DOUYIN: '抖音' }[p] || p
}
function platformTag(p: string): any {
  return { CTRIP: 'primary', MEITUAN: 'warning', DOUYIN: 'danger' }[p] || ''
}

function formatTime(t: string) {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 16)
}

function roomText(id: number) {
  const r = rooms.value.find(x => x.id === id)
  return r ? r.number : `房间#${id}`
}
function roomTypeText(id: number) {
  const t = roomTypes.value.find(x => x.id === id)
  return t ? t.name : `房型#${id}`
}

async function load() {
  loading.value = true
  try {
    const [listingData, accountData, roomData, typeData] = await Promise.all([
      listingApi.listAll(),
      otaApi.list(),
      roomApi.page({ page: 1, page_size: 100 }).then((r: any) => r.list || []),
      roomApi.types(),
    ])
    listings.value = listingData || []
    otaAccounts.value = accountData || []
    rooms.value = roomData
    roomTypes.value = typeData || []
  } finally {
    loading.value = false
  }
}

function openBind() {
  Object.assign(form, {
    otaAccountId: null,
    channelListingId: '',
    channelListingName: '',
    targetType: 'ROOM',
    roomId: null,
    roomTypeId: null,
    inventoryType: 'INDEPENDENT',
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.otaAccountId) { ElMessage.warning('请选择 OTA 账号'); return }
  if (!form.channelListingId) { ElMessage.warning('请输入渠道房源ID'); return }
  if (form.targetType === 'ROOM' && !form.roomId) { ElMessage.warning('请选择房间'); return }
  if (form.targetType === 'ROOM_TYPE' && !form.roomTypeId) { ElMessage.warning('请选择房型'); return }
  saving.value = true
  try {
    await listingApi.bind({ ...form })
    ElMessage.success('关联已建立')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function unbind(row: any) {
  await listingApi.unbind(row.id)
  ElMessage.success('关联已解除')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
