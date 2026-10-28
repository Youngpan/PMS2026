<template>
  <el-card>
    <template #header>
      <div style="display:flex; justify-content:space-between; align-items:center">
        <span>员工管理</span>
        <el-button type="primary" @click="dialog = true">新增员工</el-button>
      </div>
    </template>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="phone" label="电话" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="resetPwd(row)">重置密码</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="新增员工" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.real_name" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.plain_password" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="create">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemApi } from '@/api/modules'

const list = ref<any[]>([])
const dialog = ref(false)
const form = reactive<any>({ username: '', real_name: '', phone: '', plain_password: '' })

async function load() { list.value = await systemApi.staff() }
async function create() {
  await systemApi.createStaff(form)
  ElMessage.success('已新增')
  dialog.value = false
  load()
}
function resetPwd(row: any) {
  ElMessageBox.prompt('请输入新密码', '重置密码', { inputPattern: /.+/, inputErrorMessage: '不能为空' })
    .then(async ({ value }) => {
      await systemApi.resetPassword(row.id, value)
      ElMessage.success('已重置')
    }).catch(() => {})
}
async function toggle(row: any) {
  await systemApi.toggleStaffStatus(row.id, row.status === 1 ? 0 : 1)
  load()
}
onMounted(load)
</script>
