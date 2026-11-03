<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>冲突订单盒子</span>
        <el-tag v-if="pendingCount > 0" type="danger" size="small">{{ pendingCount }} 笔待处理</el-tag>
      </div>
    </template>

    <el-alert type="warning" :closable="false" style="margin-bottom:12px">
      因同步延迟、房态不一致等原因产生的冲突订单暂存于此。请逐笔人工处理，避免超售。
    </el-alert>

    <el-table :data="conflicts" border stripe v-loading="loading">
      <el-table-column label="平台" width="100">
        <template #default="{ row }">
          <el-tag :type="platformTag(row.platform)" size="small" style="width:100%;text-align:center">
            {{ platformText(row.platform) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="客人" width="120">
        <template #default="{ row }">
          <div>{{ row.guestName || '—' }}</div>
          <div style="font-size:12px;color:#909399">{{ row.guestPhone || '' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="入住" width="100">
        <template #default="{ row }">{{ row.checkInDate }}</template>
      </el-table-column>
      <el-table-column label="离店" width="100">
        <template #default="{ row }">{{ row.checkOutDate }}</template>
      </el-table-column>
      <el-table-column label="冲突类型" width="140">
        <template #default="{ row }">
          <el-tag :type="conflictTypeTag(row.conflictType)" size="small">
            {{ conflictTypeText(row.conflictType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="冲突详情" min-width="180">
        <template #default="{ row }">
          <div style="font-size:13px">{{ row.conflictDetail || '—' }}</div>
          <div style="font-size:12px;color:#909399">渠道单号: {{ row.channelOrderId || '—' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PENDING' ? 'danger' : row.status === 'RESOLVED' ? 'success' : 'info'">
            {{ row.status === 'PENDING' ? '待处理' : row.status === 'RESOLVED' ? '已解决' : '已取消' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="primary" @click="resolve(row)">解决</el-button>
            <el-button size="small" @click="cancel(row)">取消订单</el-button>
          </template>
          <span v-else style="color:#c0c4cc">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && conflicts.length === 0" description="暂无冲突订单 🎉" />

    <!-- 处理弹窗 -->
    <el-dialog v-model="dialogVisible" title="处理冲突订单" width="480px">
      <p style="margin-bottom:8px">
        <el-tag :type="conflictTypeTag(activeRow?.conflictType)" size="small">
          {{ conflictTypeText(activeRow?.conflictType) }}
        </el-tag>
        {{ activeRow?.guestName }} · {{ activeRow?.checkInDate }} ~ {{ activeRow?.checkOutDate }}
      </p>
      <el-form>
        <el-form-item label="处理结果">
          <el-select v-model="resolveStatus" style="width:100%">
            <el-option label="已解决（订单保留）" value="RESOLVED" />
            <el-option label="已取消（释放房态）" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input v-model="resolveNote" type="textarea" :rows="3" placeholder="换房/取消/协商等处理说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doResolve" :loading="saving">确认</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { conflictApi } from '@/api/modules'

const conflicts = ref<any[]>([])
const loading = ref(false)
const pendingCount = ref(0)
const dialogVisible = ref(false)
const saving = ref(false)
const activeRow = ref<any>(null)
const resolveStatus = ref('RESOLVED')
const resolveNote = ref('')

function platformText(p: string) {
  return { CTRIP: '携程', MEITUAN: '美团', DOUYIN: '抖音' }[p] || p
}
function platformTag(p: string): any {
  return { CTRIP: 'primary', MEITUAN: 'warning', DOUYIN: 'danger' }[p] || ''
}
function conflictTypeText(t: string) {
  return { SYNC_DELAY: '同步延迟', OVERLAP: '重叠预订', STATUS_MISMATCH: '房态不一致', MULTI_STOCK_PUZZLE: '排房冲突' }[t] || t
}
function conflictTypeTag(t: string): any {
  return { SYNC_DELAY: 'warning', OVERLAP: 'danger', STATUS_MISMATCH: 'danger', MULTI_STOCK_PUZZLE: 'info' }[t] || ''
}

async function load() {
  loading.value = true
  try {
    const data = await conflictApi.listPending()
    conflicts.value = data || []
    pendingCount.value = (data || []).filter((c: any) => c.status === 'PENDING').length
  } finally {
    loading.value = false
  }
}

function resolve(row: any) {
  activeRow.value = row
  resolveStatus.value = 'RESOLVED'
  resolveNote.value = ''
  dialogVisible.value = true
}

function cancel(row: any) {
  activeRow.value = row
  resolveStatus.value = 'CANCELLED'
  resolveNote.value = '人工取消，释放房态'
  dialogVisible.value = true
}

async function doResolve() {
  saving.value = true
  try {
    await conflictApi.resolve(activeRow.value.id, resolveNote.value, resolveStatus.value)
    ElMessage.success('已处理')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
