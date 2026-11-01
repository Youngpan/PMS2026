<template>
  <div class="layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="logo">
        <span class="logo-icon">🏨</span>
        <span class="logo-text">云 PMS</span>
      </div>
      <el-menu :default-active="route.path" router class="side-menu">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>经营概览</span>
        </el-menu-item>
        <el-menu-item index="/housekeeping">
          <el-icon><Brush /></el-icon>
          <span>房态</span>
        </el-menu-item>
        <el-menu-item index="/reservations">
          <el-icon><Calendar /></el-icon>
          <span>预订</span>
        </el-menu-item>
        <el-menu-item index="/stays">
          <el-icon><House /></el-icon>
          <span>在住</span>
        </el-menu-item>
        <el-menu-item index="/rooms">
          <el-icon><Grid /></el-icon>
          <span>房型/房号</span>
        </el-menu-item>
        <el-menu-item index="/reports">
          <el-icon><DataAnalysis /></el-icon>
          <span>报表</span>
        </el-menu-item>
        <el-menu-item index="/system/staff">
          <el-icon><User /></el-icon>
          <span>系统</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- Main area -->
    <div class="main-area">
      <header class="topbar">
        <div class="topbar-title">{{ route.meta.title }}</div>
        <div class="topbar-right">
          <el-dropdown @command="onCommand">
            <span class="user-btn">{{ auth.realName || '管理员' }} ▾</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content"><router-view /></main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/modules'
import { Odometer, Brush, Calendar, House, Grid, DataAnalysis, User } from '@element-plus/icons-vue'

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
.layout { display: flex; height: 100vh; background: #f0f2f5; }

/* ---------- Sidebar ---------- */
.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1a2332 0%, #0f1729 100%);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255,255,255,.06);
}
.logo-icon { font-size: 22px; }
.logo-text { font-size: 18px; font-weight: 700; color: #fff; letter-spacing: 1px; }

.side-menu {
  flex: 1;
  background: transparent !important;
  border-right: none !important;
  padding-top: 8px;
}
.side-menu .el-menu-item {
  height: 44px;
  line-height: 44px;
  color: rgba(255,255,255,.65) !important;
  margin: 2px 8px;
  border-radius: 8px;
  transition: all .15s;
}
.side-menu .el-menu-item:hover {
  color: #fff !important;
  background: rgba(255,255,255,.08) !important;
}
.side-menu .el-menu-item.is-active {
  color: #fff !important;
  background: #409eff !important;
}
.side-menu .el-menu-item .el-icon { font-size: 18px; margin-right: 8px; }

/* ---------- Main ---------- */
.main-area { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}
.topbar-title { font-size: 16px; font-weight: 600; color: #1a1a2e; }
.user-btn { cursor: pointer; color: #409eff; font-size: 14px; }

.content {
  flex: 1;
  padding: 20px 24px;
  overflow: auto;
}
</style>
