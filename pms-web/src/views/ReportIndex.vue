<template>
  <div>
    <el-card style="margin-bottom:16px">
      <template #header>营收趋势</template>
      <el-form inline>
        <el-form-item label="区间">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadRevenue">查询营收</el-button>
          <el-button @click="loadOccupancy" :disabled="!range?.length">出租率</el-button>
          <el-button @click="loadBalance">客账余额</el-button>
        </el-form-item>
      </el-form>
      <div ref="chartEl" style="width:100%; height:320px"></div>
    </el-card>

    <el-card style="margin-bottom:16px" v-if="revenue">
      <template #header>营收汇总</template>
      <el-statistic title="营收合计" :value="revenue.total" prefix="¥" />
      <el-table :data="revenue.byMethod" border style="margin-top:12px">
        <el-table-column prop="method" label="支付方式" />
        <el-table-column prop="amount" label="金额" />
      </el-table>
    </el-card>

    <el-card style="margin-bottom:16px" v-if="occ">
      <template #header>出租率 / ADR / RevPAR</template>
      <div style="display:flex; gap:24px">
        <el-statistic title="出租率" :value="occ.occupancy * 100" :precision="1" suffix="%" />
        <el-statistic title="ADR" :value="occ.adr" prefix="¥" />
        <el-statistic title="RevPAR" :value="occ.revpar" prefix="¥" />
        <el-statistic title="已售间夜" :value="occ.soldRoomNights" />
      </div>
    </el-card>

    <el-card v-if="balances">
      <template #header>客账余额表</template>
      <el-table :data="balances" border>
        <el-table-column prop="id" label="账单ID" width="100" />
        <el-table-column prop="stayId" label="在住ID" width="100" />
        <el-table-column prop="guestId" label="宾客ID" width="100" />
        <el-table-column prop="balance" label="余额" />
        <el-table-column prop="status" label="状态" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import * as echarts from 'echarts'
import { reportApi } from '@/api/modules'

const range = ref<string[]>([])
const revenue = ref<any>(null)
const occ = ref<any>(null)
const balances = ref<any[] | null>(null)
const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

async function loadRevenue() {
  if (!range.value?.length) return
  const [from, to] = range.value
  revenue.value = await reportApi.revenue(from, to)
  const daily: any[] = await reportApi.revenueDaily(from, to)
  renderChart(daily)
}

async function loadOccupancy() {
  if (!range.value?.length) return
  occ.value = await reportApi.occupancy(range.value[0], range.value[1])
}

async function loadBalance() {
  balances.value = await reportApi.folioBalance()
}

function renderChart(daily: any[]) {
  nextTick(() => {
    if (!chartEl.value) return
    chart = chart || echarts.init(chartEl.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: daily.map(d => d.date), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', name: '元' },
      series: [{
        name: '每日营收', type: 'line', smooth: true, areaStyle: {},
        data: daily.map(d => Number(d.amount)),
        itemStyle: { color: '#409eff' },
      }],
    })
  })
}
</script>
