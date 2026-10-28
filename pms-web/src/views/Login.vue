<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 style="text-align:center; margin:0 0 24px">酒店 PMS 登录</h2>
      <el-form :model="form" label-width="0" @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">登录</el-button>
      </el-form>
      <p style="text-align:center; color:#999; margin-top:16px">默认账号 admin / admin</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/modules'
import { useAuthStore } from '@/stores/auth'

const form = reactive({ username: 'admin', password: 'admin' })
const loading = ref(false)
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

async function onLogin() {
  loading.value = true
  try {
    const res: any = await authApi.login(form.username, form.password)
    auth.set(res)
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.login-card { width: 360px; }
</style>
