<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>OTA 房型管理</span>
        <div>
          <el-select v-model="filterAccountId" placeholder="按OTA账号筛选" clearable @change="load" style="width:200px;margin-right:12px">
            <el-option v-for="a in otaAccounts" :key="a.id" :label="platformText(a.platform) + ' - ' + a.accountName" :value="a.id" />
          </el-select>
          <el-button type="primary" @click="openCreate">新建房型</el-button>
        </div>
      </div>
    </template>

    <el-table :data="products" border stripe v-loading="loading" row-key="id">
      <el-table-column label="平台" width="100">
        <template #default="{ row }">
          <el-tag :type="platformTag(row.platform)" size="small" style="width:100%;text-align:center">
            {{ platformText(row.platform) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="渠道房源ID" width="140">
        <template #default="{ row }">{{ row.channelListingId }}</template>
      </el-table-column>
      <el-table-column label="房型名称" min-width="140">
        <template #default="{ row }">{{ row.listingName || '—' }}</template>
      </el-table-column>
      <el-table-column label="关联类型" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.targetType === 'ROOM'" type="success" size="small">房间</el-tag>
          <el-tag v-else type="warning" size="small">房型</el-tag>
          <span style="margin-left:6px">{{ row.targetType === 'ROOM' ? roomText(row.roomId) : roomTypeText(row.roomTypeId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.productStatus === 'ON_SALE'" type="success">出售中</el-tag>
          <el-tag v-else-if="row.productStatus === 'SOLD_OUT'" type="warning">售罄</el-tag>
          <el-tag v-else type="info">草稿</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="早餐" width="80">
        <template #default="{ row }">{{ row.breakfast ? row.breakfastFee + '元' : '无' }}</template>
      </el-table-column>
      <el-table-column label="入住方式" width="100">
        <template #default="{ row }">{{ checkinMethodText(row.checkinMethod) }}</template>
      </el-table-column>
      <el-table-column label="付款方式" width="100">
        <template #default="{ row }">{{ paymentMethodText(row.paymentMethod) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确认删除该房型？" @confirm="remove(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 OTA 房型' : '新建 OTA 房型'" width="720px">
      <el-form :model="form" label-width="130px">

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="OTA 账号" required>
              <el-select v-model="form.otaAccountId" placeholder="选择 OTA 账号" style="width:100%" :disabled="isEdit">
                <el-option v-for="a in otaAccounts" :key="a.id" :label="platformText(a.platform) + ' - ' + a.accountName" :value="a.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" required>
              <el-select v-model="form.productStatus" placeholder="选择状态" style="width:100%">
                <el-option label="出售中" value="ON_SALE" />
                <el-option label="售罄" value="SOLD_OUT" />
                <el-option label="草稿" value="DRAFT" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="渠道房源ID" required>
              <el-input v-model="form.channelListingId" placeholder="渠道侧的房源/产品ID" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房型名称" required>
              <el-input v-model="form.listingName" placeholder="渠道房型名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联类型" required>
              <el-radio-group v-model="form.targetType" :disabled="isEdit">
                <el-radio value="ROOM">关联到房间</el-radio>
                <el-radio value="ROOM_TYPE">关联到房型</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="库存类型">
              <el-select v-model="form.inventoryType" :disabled="isEdit">
                <el-option label="独立房源" value="INDEPENDENT" />
                <el-option label="多库存" value="MULTI_STOCK" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="直接预订" prop="isDirect">
              <el-switch v-model="form.isDirect" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="容量" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" :max="20" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="床型">
              <el-select v-model="form.bedType" placeholder="选择床型" filterable clearable>
                <el-option label="大床" value="大床" />
                <el-option label="双床" value="双床" />
                <el-option label="单人床" value="单人床" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="早餐">
              <el-switch v-model="form.breakfast" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8" v-if="form.breakfast">
            <el-form-item label="早餐费用">
              <el-input-number v-model="form.breakfastFee" :min="0" :step="5" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="入住方式">
              <el-select v-model="form.checkinMethod" placeholder="选择入住方式" clearable>
                <el-option label="前台入住" value="FRONT_DESK" />
                <el-option label="自助入住" value="SELF_CHECKIN" />
                <el-option label="智能门锁" value="SMART_LOCK" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方式">
              <el-select v-model="form.paymentMethod" placeholder="选择付款方式" clearable>
                <el-option label="在线支付" value="ONLINE" />
                <el-option label="到店支付" value="AT_STORE" />
                <el-option label="混合支付" value="HYBRID" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="发票类型">
              <el-select v-model="form.invoiceType" placeholder="选择发票类型" clearable>
                <el-option label="默认" value="DEFAULT" />
                <el-option label="增值税普票" value="VAT_COMMON" />
                <el-option label="增值税专票" value="VAT_SPECIAL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="税率(%)">
              <el-input-number v-model="form.invoiceTaxRate" :min="0" :step="1" :precision="2" :max="50" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="加床">
              <el-switch v-model="form.extraBed" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="加人">
              <el-switch v-model="form.extraPerson" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

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
import { productApi, otaApi, roomApi } from '@/api/modules'

const products = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const otaAccounts = ref<any[]>([])
const rooms = ref<any[]>([])
const roomTypes = ref<any[]>([])
const filterAccountId = ref<number | null>(null)

const form = reactive({
  id: null as number | null,
  otaAccountId: null as number | null,
  channelListingId: '',
  listingName: '',
  targetType: 'ROOM',
  roomId: null as number | null,
  roomTypeId: null as number | null,
  inventoryType: 'INDEPENDENT',
  productStatus: 'ON_SALE',
  isDirect: 1,
  capacity: 2,
  bedType: '',
  breakfast: 1,
  breakfastFee: 38,
  checkinMethod: 'FRONT_DESK',
  paymentMethod: 'ONLINE',
  invoiceType: 'DEFAULT',
  invoiceTaxRate: 0,
  extraBed: 0,
  extraPerson: 0,
})

function platformText(p: string) {
  return { CTRIP: '携程', MEITUAN: '美团', DOUYIN: '抖音' }[p] || p
}
function platformTag(p: string): any {
  return { CTRIP: 'primary', MEITUAN: 'warning', DOUYIN: 'danger' }[p] || ''
}
function checkinMethodText(m: string) {
  return { FRONT_DESK: '前台入住', SELF_CHECKIN: '自助入住', SMART_LOCK: '智能门锁' }[m] || m || '—'
}
function paymentMethodText(m: string) {
  return { ONLINE: '在线支付', AT_STORE: '到店支付', HYBRID: '混合支付' }[m] || m || '—'
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
    const [productData, accountData, roomData, typeData] = await Promise.all([
      productApi.list(filterAccountId.value),
      otaApi.list(),
      roomApi.page({ page: 1, page_size: 100 }).then((r: any) => r.list || []),
      roomApi.types(),
    ])
    products.value = productData || []
    otaAccounts.value = accountData || []
    rooms.value = roomData
    roomTypes.value = typeData || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    otaAccountId: null,
    channelListingId: '',
    listingName: '',
    targetType: 'ROOM',
    roomId: null,
    roomTypeId: null,
    inventoryType: 'INDEPENDENT',
    productStatus: 'ON_SALE',
    isDirect: 1,
    capacity: 2,
    bedType: '',
    breakfast: 1,
    breakfastFee: 38,
    checkinMethod: 'FRONT_DESK',
    paymentMethod: 'ONLINE',
    invoiceType: 'DEFAULT',
    invoiceTaxRate: 0,
    extraBed: 0,
    extraPerson: 0,
  })
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    otaAccountId: row.otaAccountId,
    channelListingId: row.channelListingId,
    listingName: row.listingName,
    targetType: row.targetType,
    roomId: row.roomId,
    roomTypeId: row.roomTypeId,
    inventoryType: row.inventoryType,
    productStatus: row.productStatus,
    isDirect: row.isDirect ?? 1,
    capacity: row.capacity ?? 2,
    bedType: row.bedType ?? '',
    breakfast: row.breakfast ?? 1,
    breakfastFee: row.breakfastFee ?? 38,
    checkinMethod: row.checkinMethod ?? 'FRONT_DESK',
    paymentMethod: row.paymentMethod ?? 'ONLINE',
    invoiceType: row.invoiceType ?? 'DEFAULT',
    invoiceTaxRate: row.invoiceTaxRate ?? 0,
    extraBed: row.extraBed ?? 0,
    extraPerson: row.extraPerson ?? 0,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.otaAccountId) { ElMessage.warning('请选择 OTA 账号'); return }
  if (!form.channelListingId) { ElMessage.warning('请输入渠道房源ID'); return }
  if (!form.listingName) { ElMessage.warning('请输入房型名称'); return }
  if (form.targetType === 'ROOM' && !form.roomId) { ElMessage.warning('请选择房间'); return }
  if (form.targetType === 'ROOM_TYPE' && !form.roomTypeId) { ElMessage.warning('请选择房型'); return }
  saving.value = true
  try {
    if (isEdit.value && form.id) {
      await productApi.update(form.id, { ...form, id: undefined })
      ElMessage.success('房型已更新')
    } else {
      await productApi.create({ ...form, id: undefined })
      ElMessage.success('房型已创建')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  await productApi.remove(id)
  ElMessage.success('房型已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
