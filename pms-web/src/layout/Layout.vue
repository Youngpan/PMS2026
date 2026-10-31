<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">酒店 PMS</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard"><el-icon><Odometer /></el-icon><span>仪表盘</span></el-menu-item>
        <el-menu-item index="/reservations"><el-icon><Calendar /></el-icon><span>预订管理</span></el-menu-item>
        <el-menu-item index="/stays"><el-icon><House /></el-icon><span>在住看板</span></el-menu-item>
        <el-menu-item index="/housekeeping"><el-icon><Brush /></el-icon><span>房态看板</span></el-menu-item>
        <el-menu-item index="/rooms"><el-icon><Grid /></el-icon><span>房型与房号</span></el-menu-item>
        <el-menu-item index="/reports"><el-icon><DataAnalysis /></el-icon><span>夜审与报表</span></el-menu-item>
        <el-menu-item index="/system/staff"><el-icon><User /></el-icon><span>员工管理</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta.title }}</span>
        <el-dropdown @command="onCommand">
          <span class="user">{{ auth.realName || '未登录' }} ▾</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/modules'
import { Odometer, Calendar, House, Brush, Grid, DataAnalysis, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

async function onCommand(cmd: string) {
  if (cmd === 'logout') {
    try { await authApi.logout() } catch {}
    auth.clear()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #001529; color: #fff; }
.logo { height: 56px; line-height: 56px; text-align: center; color: #fff; font-size: 18px; font-weight: 600; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #eee; }
.title { font-size: 16px; font-weight: 600; }
.user { cursor: pointer; color: #409eff; }
:deep(.el-menu) { border-right: none; }
</style>
