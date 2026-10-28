<template>
  <el-card>
    <template #header><div style="display:flex; justify-content:space-between; align-items:center"><span>在住看板</span><el-button @click="load">刷新</el-button></div></template>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="在住ID" width="90" />
      <el-table-column prop="roomId" label="房号ID" width="90" />
      <el-table-column prop="guestId" label="宾客ID" width="90" />
      <el-table-column prop="rate" label="房价" width="100" />
      <el-table-column prop="checkInAt" label="入住时间" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="openFolio(row)">账单</el-button>
          <el-button size="small" type="warning" @click="transfer(row)">换房</el-button>
          <el-button size="small" type="danger" @click="checkout(row)">退房</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { stayApi, folioApi } from '@/api/modules'

const list = ref<any[]>([])
const router = useRouter()

async function load() { list.value = await stayApi.inHouse() }

async function openFolio(row: any) {
  const f: any = await folioApi.byStay(row.id)
  router.push(`/folios/${f.folio.id}`)
}
function transfer(row: any) {
  ElMessageBox.prompt('目标房号ID', '换房', { inputPattern: /^\d+$/, inputErrorMessage: '请输入数字' })
    .then(async ({ value }) => {
      await stayApi.transferRoom(row.id, Number(value), '前台换房')
      ElMessage.success('已换房')
      load()
    }).catch(() => {})
}
function checkout(row: any) {
  ElMessageBox.prompt('实收金额', '退房收款', { inputPattern: /^\d+(\.\d+)?$/, inputErrorMessage: '请输入金额' })
    .then(async ({ value }) => {
      await stayApi.checkout(row.id, 'WECHAT', Number(value))
      ElMessage.success('已退房')
      load()
    }).catch(() => {})
}
onMounted(load)
</script>
