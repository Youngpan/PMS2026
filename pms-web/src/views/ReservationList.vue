<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span>预订列表</span>
          <el-button type="primary" @click="createVisible = true">新建预订</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe>
        <el-table-column prop="resNo" label="预订号" width="150" />
        <el-table-column prop="guestName" label="宾客" width="100" />
        <el-table-column prop="guestPhone" label="电话" width="130" />
        <el-table-column prop="checkInDate" label="入住" width="120" />
        <el-table-column prop="checkOutDate" label="离店" width="120" />
        <el-table-column prop="nights" label="晚数" width="80" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }"><el-tag>{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" type="success" :disabled="row.status !== 'PENDING'" @click="confirm(row)">确认</el-button>
            <el-button size="small" type="warning" :disabled="row.status !== 'CONFIRMED'" @click="checkin(row)">入住</el-button>
            <el-button size="small" type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top:12px; justify-content:flex-end; display:flex"
        v-model:current-page="page" v-model:page-size="pageSize"
        :total="total" layout="prev, pager, next, total" @current-change="load"
      />
    </el-card>

    <ReservationCreateDialog v-model="createVisible" @success="onCreated" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reservationApi, roomApi, stayApi } from '@/api/modules'
import ReservationCreateDialog from '@/components/ReservationCreateDialog.vue'

const list = ref<any[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const createVisible = ref(false)

async function load() {
  const res: any = await reservationApi.list({ page: page.value, page_size: pageSize.value })
  list.value = res.list || []
  total.value = res.total || 0
}

function onCreated() { page.value = 1; load() }

async function confirm(row: any) {
  await reservationApi.confirm(row.id)
  ElMessage.success('已确认')
  load()
}

async function cancel(row: any) {
  await reservationApi.cancel(row.id)
  ElMessage.success('已取消')
  load()
}

async function checkin(row: any) {
  const detail: any = await reservationApi.get(row.id)
  const line = detail.rooms?.[0]
  if (!line) { ElMessage.warning('无预订房型明细'); return }
  const vacant: any[] = await roomApi.vacant(line.roomTypeId)
  if (!vacant.length) { ElMessage.warning('该房型无净房'); return }
  ElMessageBox.confirm(`可用净房：${vacant.map(r => r.number).join(', ')}，是否入住 ${vacant[0].number}？`, '办理入住')
    .then(async () => {
      await stayApi.checkin({
        reservationId: row.id,
        stayItems: [{ reservationRoomId: line.id, roomId: vacant[0].id }],
        deposit: 500, depositMethod: 'CASH',
      })
      ElMessage.success('已入住')
      load()
    }).catch(() => {})
}

function statusText(s: string) {
  return { PENDING: '待确认', CONFIRMED: '已确认', CHECKED_IN: '已入住', CHECKED_OUT: '已退房', CANCELLED: '已取消', NO_SHOW: '未到' }[s] || s
}

onMounted(load)
</script>
