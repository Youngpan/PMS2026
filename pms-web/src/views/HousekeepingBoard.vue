<template>
  <el-card>
    <template #header><div style="display:flex; justify-content:space-between; align-items:center"><span>房态看板</span><el-button @click="load">刷新</el-button></div></template>
    <div v-for="floor in floors" :key="floor.floor" style="margin-bottom:16px">
      <div style="font-weight:600; margin-bottom:8px">楼层 {{ floor.floor }}</div>
      <el-row :gutter="8">
        <el-col v-for="r in floor.rooms" :key="r.id" :span="3">
          <el-tooltip :content="`房号 ${r.number} · ${statusText(r.status)}`">
            <div :class="['room', r.status]" @click="onRoom(r)">
              <div style="font-weight:700">{{ r.number }}</div>
              <div style="font-size:12px">{{ statusText(r.status) }}</div>
            </div>
          </el-tooltip>
        </el-col>
      </el-row>
    </div>
    <div class="legend">
      <span class="room VC small"></span>净房
      <span class="room VD small"></span>脏房
      <span class="room OCC small"></span>在住
      <span class="room OOO small"></span>维修
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { housekeepingApi } from '@/api/modules'

const floors = ref<any[]>([])

async function load() { floors.value = await housekeepingApi.dashboard() }

function onRoom(r: any) {
  ElMessageBox.confirm(`房号 ${r.number}（${statusText(r.status)}）`, '房务操作', {
    confirmButtonText: '报修', cancelButtonText: '派清扫', distinguishCancelAndClose: true,
  }).then(async () => {
    await housekeepingApi.reportMaintenance({ room_id: r.id, issue: '前台报修' })
    ElMessage.success('已报修')
    load()
  }).catch(async (action) => {
    if (action === 'cancel') {
      await housekeepingApi.createTask({ room_id: r.id, type: 'CHECKOUT_CLEAN', assignee_id: null })
      ElMessage.success('已派清扫')
      load()
    }
  })
}
function statusText(s: string) {
  return { VC: '净房', VD: '脏房', OCC: '在住', OOO: '维修', BLOCK: '预留' }[s] || s
}

// 房态实时推送
let ws: WebSocket | null = null
let reconnectTimer: any = null
function connectWs() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  ws = new WebSocket(`${proto}://${location.host}/api/v1/ws/room-status`)
  ws.onmessage = (ev) => {
    try {
      const msg = JSON.parse(ev.data)
      if (msg.type === 'room-status') load()
    } catch {}
  }
  ws.onclose = () => {
    reconnectTimer = setTimeout(connectWs, 3000)
  }
}

onMounted(() => { load(); connectWs() })
onUnmounted(() => {
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (ws) ws.close()
})
</script>

<style scoped>
.room { padding: 10px; border-radius: 6px; text-align: center; color: #fff; cursor: pointer; margin-bottom: 8px; }
.room.VC { background: #67c23a; }
.room.VD { background: #e6a23c; }
.room.OCC { background: #409eff; }
.room.OOO { background: #909399; }
.room.small { display: inline-block; width: 16px; height: 16px; vertical-align: middle; margin: 0 4px 0 12px; }
.legend { margin-top: 12px; color: #666; }
</style>
