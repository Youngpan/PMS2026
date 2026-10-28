<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ audit.checkins }}</div><div>今日入住</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ audit.checkouts }}</div><div>今日退房</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ audit.inHouse }}</div><div>在住数</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num">¥{{ audit.revenue || 0 }}</div><div>今日营收</div></div></el-card></el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <template #header>夜审草稿（{{ date }}）</template>
      <el-button @click="load">刷新</el-button>
      <el-button type="primary" @click="confirm">确认夜审</el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { reportApi } from '@/api/modules'

const today = new Date().toISOString().slice(0, 10)
const date = ref(today)
const audit = ref<any>({ checkins: 0, checkouts: 0, inHouse: 0, revenue: 0 })

async function load() {
  audit.value = await reportApi.nightAudit(date.value)
}
async function confirm() {
  await reportApi.confirmNightAudit(date.value)
  ElMessage.success('夜审已确认')
  load()
}
onMounted(load)
</script>

<style scoped>
.stat { text-align: center; }
.stat .num { font-size: 28px; font-weight: 700; color: #409eff; }
</style>
