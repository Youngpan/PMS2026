<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>房型与房号</span>
        <div>
          <template v-if="tab === 'types'">
            <el-button type="primary" @click="openTypeCreate">新增房型</el-button>
          </template>
          <template v-else>
            <el-button @click="openBatch">批量生成房号</el-button>
            <el-button type="primary" @click="openRoomCreate">新增房号</el-button>
          </template>
        </div>
      </div>
    </template>

    <el-tabs v-model="tab">
      <!-- ---------- 房型 ---------- -->
      <el-tab-pane label="房型" name="types">
        <el-table :data="types" border stripe>
          <el-table-column prop="code" label="编码" width="120" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="bedType" label="床型" width="110" />
          <el-table-column prop="maxOccupancy" label="最大入住" width="100" align="center" />
          <el-table-column prop="area" label="面积(㎡)" width="100" align="center" />
          <el-table-column label="基础价" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.baseRate) }}</template>
          </el-table-column>
          <el-table-column label="门市价" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.rackRate) }}</template>
          </el-table-column>
          <el-table-column label="房间数" width="90" align="center">
            <template #default="{ row }">{{ roomCount(row.id) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button size="small" @click="openTypeEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ---------- 房号 ---------- -->
      <el-tab-pane label="房号" name="rooms">
        <div class="filters">
          <el-select v-model="filter.typeId" placeholder="全部房型" clearable style="width:180px" @change="loadRooms">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
          <el-select v-model="filter.rentStatus" placeholder="可售状态" clearable style="width:150px" @change="loadRooms">
            <el-option label="打开" value="OPEN" />
            <el-option label="关闭" value="CLOSED" />
            <el-option label="已排房(在住)" value="ASSIGNED" />
          </el-select>
          <el-select v-model="filter.cleanStatus" placeholder="卫生状态" clearable style="width:150px" @change="loadRooms">
            <el-option label="净房" value="CLEAN" />
            <el-option label="脏房" value="DIRTY" />
          </el-select>
          <span class="summary">共 {{ rooms.length }} 间</span>
        </div>

        <el-table :data="rooms" border stripe v-loading="loading">
          <el-table-column prop="number" label="房号" width="100" />
          <el-table-column label="房型" min-width="140">
            <template #default="{ row }">{{ typeName(row.roomTypeId) }}</template>
          </el-table-column>
          <el-table-column prop="floor" label="楼层" width="80" align="center" />
          <el-table-column label="可售状态" width="130">
            <template #default="{ row }">
              <el-tag :type="rentTag(row.rentStatus)">{{ rentText(row.rentStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="卫生状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.cleanStatus === 'CLEAN' ? 'success' : 'warning'" effect="plain">
                {{ row.cleanStatus === 'CLEAN' ? '净房' : '脏房' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button size="small" @click="openRoomEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 房型弹窗 -->
    <el-dialog v-model="typeDialog" :title="typeForm.id ? '编辑房型' : '新增房型'" width="520px">
      <el-form :model="typeForm" label-width="90px">
        <el-form-item label="编码"><el-input v-model="typeForm.code" placeholder="如 STD" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="typeForm.name" placeholder="如 标准大床房" /></el-form-item>
        <el-form-item label="床型"><el-input v-model="typeForm.bed_type" placeholder="如 1.8m大床" /></el-form-item>
        <el-form-item label="最大入住">
          <el-input-number v-model="typeForm.max_occupancy" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="面积(㎡)"><el-input-number v-model="typeForm.area" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="基础价"><el-input-number v-model="typeForm.base_rate" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="门市价"><el-input-number v-model="typeForm.rack_rate" :min="0" :precision="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 房号弹窗 -->
    <el-dialog v-model="roomDialog" :title="roomForm.id ? '编辑房号' : '新增房号'" width="480px">
      <el-form :model="roomForm" label-width="80px">
        <el-form-item label="房号"><el-input v-model="roomForm.number" placeholder="如 301" /></el-form-item>
        <el-form-item label="房型">
          <el-select v-model="roomForm.room_type_id" placeholder="选择房型" style="width:100%">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层">
          <el-input-number v-model="roomForm.floor" :min="-3" :max="99" />
          <span class="hint">留空按房号自动推断</span>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="roomForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRoom">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量生成房号 -->
    <el-dialog v-model="batchDialog" title="批量生成房号" width="520px">
      <el-form label-width="90px">
        <el-form-item label="房型">
          <el-select v-model="batchForm.room_type_id" placeholder="选择房型" style="width:100%">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层">
          <el-input-number v-model="batchForm.floor" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="起始序号">
          <el-input-number v-model="batchForm.start" :min="1" :max="98" />
          <span class="hint">房号 = 楼层 × 100 + 序号</span>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="batchForm.count" :min="1" :max="120" />
        </el-form-item>
        <el-form-item label="预览">
          <span class="preview">{{ previewText }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!batchNumbers.length" @click="saveBatch">生成 {{ batchNumbers.length }} 间</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { roomApi } from '@/api/modules'

const tab = ref<'types' | 'rooms'>('rooms')
const loading = ref(false)
const types = ref<any[]>([])
const rooms = ref<any[]>([])
const filter = reactive<{ typeId?: number; rentStatus?: string; cleanStatus?: string }>({})

const typeDialog = ref(false)
const typeForm = reactive<any>(blankType())

const roomDialog = ref(false)
const roomForm = reactive<any>(blankRoom())

const batchDialog = ref(false)
const batchForm = reactive({ room_type_id: undefined as number | undefined, floor: 1, start: 1, count: 10 })

function blankType() {
  return { id: undefined, code: '', name: '', bed_type: '', max_occupancy: 2, area: 25, base_rate: 0, rack_rate: 0 }
}
function blankRoom() {
  return { id: undefined, number: '', room_type_id: undefined as number | undefined, floor: undefined as number | undefined, remark: '' }
}

async function loadTypes() {
  types.value = (await roomApi.types()) as any[]
}

async function loadRooms() {
  loading.value = true
  try {
    const res: any = await roomApi.page({
      page: 1,
      page_size: 500,
      room_type_id: filter.typeId,
      rent_status: filter.rentStatus,
      clean_status: filter.cleanStatus,
    })
    rooms.value = res.list || []
  } finally {
    loading.value = false
  }
}

async function load() {
  await Promise.all([loadTypes(), loadRooms()])
}

function typeName(id: number) {
  return types.value.find((t) => t.id === id)?.name || `#${id}`
}
function roomCount(typeId: number) {
  return rooms.value.filter((r) => r.roomTypeId === typeId).length
}
function money(v: any) {
  return v == null ? '-' : Number(v).toFixed(2)
}
function rentText(s: string) {
  return { OPEN: '打开', CLOSED: '关闭', ASSIGNED: '已排房' }[s] || s
}
function rentTag(s: string): any {
  return { OPEN: 'success', CLOSED: 'info', ASSIGNED: 'warning' }[s] || ''
}

// ---------- 房型 ----------
function openTypeCreate() {
  Object.assign(typeForm, blankType())
  typeDialog.value = true
}
function openTypeEdit(row: any) {
  Object.assign(typeForm, {
    id: row.id, code: row.code, name: row.name, bed_type: row.bedType,
    max_occupancy: row.maxOccupancy ?? 2, area: Number(row.area ?? 0),
    base_rate: Number(row.baseRate ?? 0), rack_rate: Number(row.rackRate ?? 0),
  })
  typeDialog.value = true
}
async function saveType() {
  if (!typeForm.code || !typeForm.name) { ElMessage.warning('编码和名称必填'); return }
  const payload = {
    code: typeForm.code, name: typeForm.name, bedType: typeForm.bed_type,
    maxOccupancy: typeForm.max_occupancy, area: typeForm.area,
    baseRate: typeForm.base_rate, rackRate: typeForm.rack_rate,
  }
  if (typeForm.id) {
    await roomApi.updateType(typeForm.id, payload)
  } else {
    await roomApi.createType(payload)
  }
  ElMessage.success('已保存')
  typeDialog.value = false
  await load()
}

// ---------- 房号 ----------
function openRoomCreate() {
  Object.assign(roomForm, blankRoom())
  roomForm.room_type_id = types.value[0]?.id
  roomDialog.value = true
}
function openRoomEdit(row: any) {
  Object.assign(roomForm, {
    id: row.id, number: row.number, room_type_id: row.roomTypeId,
    floor: row.floor ?? undefined, remark: row.remark || '',
  })
  roomDialog.value = true
}
async function saveRoom() {
  if (!roomForm.number) { ElMessage.warning('房号必填'); return }
  if (!roomForm.room_type_id) { ElMessage.warning('请选择房型'); return }
  const payload = {
    number: roomForm.number,
    roomTypeId: roomForm.room_type_id,
    floor: roomForm.floor ?? null,
    remark: roomForm.remark,
  }
  if (roomForm.id) {
    await roomApi.update(roomForm.id, payload)
  } else {
    await roomApi.create(payload)
  }
  ElMessage.success('已保存')
  roomDialog.value = false
  await load()
}

// ---------- 批量 ----------
const batchNumbers = computed(() => {
  const { floor, start, count } = batchForm
  if (!floor || !start || !count) return []
  return Array.from({ length: count }, (_, i) => String(floor * 100 + start + i))
})
const previewText = computed(() => {
  const n = batchNumbers.value
  if (!n.length) return '—'
  return n.length > 6 ? `${n[0]} … ${n[n.length - 1]}（共 ${n.length} 间）` : n.join('、')
})

function openBatch() {
  batchForm.room_type_id = types.value[0]?.id
  batchForm.floor = 1
  batchForm.start = 1
  batchForm.count = 10
  batchDialog.value = true
}
async function saveBatch() {
  if (!batchForm.room_type_id) { ElMessage.warning('请选择房型'); return }
  await roomApi.batchCreate({
    roomTypeId: batchForm.room_type_id,
    floor: batchForm.floor,
    numbers: batchNumbers.value,
  })
  ElMessage.success(`已生成 ${batchNumbers.value.length} 间房号`)
  batchDialog.value = false
  await load()
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
.summary { color: #909399; font-size: 13px; }
.hint { margin-left: 10px; color: #909399; font-size: 12px; }
.preview { color: #303133; font-family: Consolas, monospace; }
</style>
