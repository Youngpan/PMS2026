<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" title="录入订单" width="480px">
    <el-form :model="form" label-width="80px">
      <el-form-item v-if="form.assignedRoomId" label="房号">
        <el-tag type="info">{{ form.roomNumber || ('#' + form.assignedRoomId) }}</el-tag>
      </el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.guestName" /></el-form-item>
      <el-form-item label="电话"><el-input v-model="form.guestPhone" /></el-form-item>
      <el-form-item label="入住"><el-date-picker v-model="form.checkInDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="离店"><el-date-picker v-model="form.checkOutDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="房型">
        <el-select v-model="form.roomTypeId" placeholder="选择房型" @change="onTypeChange">
          <el-option v-for="t in roomTypes" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="房价"><el-input-number v-model="form.rate" :min="0" :precision="2" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reservationApi, roomApi } from '@/api/modules'

const props = defineProps<{
  modelValue: boolean
  /** 预填：从房态日历空格发起时，预填房号/房型/入住日期 */
  prefill?: { checkInDate?: string; roomTypeId?: number; assignedRoomId?: number; roomNumber?: string }
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'success'): void }>()

const roomTypes = ref<any[]>([])
const saving = ref(false)
const form = reactive<any>({
  guestName: '', guestPhone: '', checkInDate: '', checkOutDate: '',
  roomTypeId: null, rate: 0, remark: '',
  assignedRoomId: null as number | null, roomNumber: '' as string,
})

function reset() {
  const p = props.prefill || {}
  Object.assign(form, {
    guestName: '', guestPhone: '',
    checkInDate: p.checkInDate || today(),
    checkOutDate: p.checkInDate ? addDays(p.checkInDate, 1) : '',
    roomTypeId: p.roomTypeId ?? null,
    rate: 0, remark: '',
    assignedRoomId: p.assignedRoomId ?? null,
    roomNumber: p.roomNumber || '',
  })
  if (form.roomTypeId) onTypeChange()
}

function today() { return new Date().toISOString().slice(0, 10) }
function addDays(d: string, n: number) {
  const dt = new Date(d); dt.setDate(dt.getDate() + n); return dt.toISOString().slice(0, 10)
}

function onTypeChange() {
  const t = roomTypes.value.find(x => x.id === form.roomTypeId)
  if (t) form.rate = Number(t.baseRate)
}

watch(() => props.modelValue, (v) => { if (v) reset() })

async function save() {
  if (!form.roomTypeId) { ElMessage.warning('请选择房型'); return }
  if (!form.checkInDate || !form.checkOutDate) { ElMessage.warning('请选择入住/离店日期'); return }
  saving.value = true
  try {
    await reservationApi.create({
      guestName: form.guestName,
      guestPhone: form.guestPhone,
      checkInDate: form.checkInDate,
      checkOutDate: form.checkOutDate,
      channelId: 1,
      remark: form.remark,
      rooms: [{ roomTypeId: form.roomTypeId, roomCount: 1, rate: form.rate, assignedRoomId: form.assignedRoomId || undefined }],
    })
    ElMessage.success('订单已录入')
    emit('update:modelValue', false)
    emit('success')
  } finally {
    saving.value = false
  }
}

onMounted(async () => { roomTypes.value = await roomApi.types() })
</script>
