import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
const routes = [
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    {
        path: '/',
        component: () => import('@/layout/Layout.vue'),
        redirect: '/dashboard',
        children: [
            { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '仪表盘' } },
            { path: 'reservations', name: 'reservations', component: () => import('@/views/ReservationList.vue'), meta: { title: '预订管理' } },
            { path: 'stays', name: 'stays', component: () => import('@/views/StayInHouse.vue'), meta: { title: '在住看板' } },
            { path: 'housekeeping', name: 'housekeeping', component: () => import('@/views/HousekeepingBoard.vue'), meta: { title: '房态看板' } },
            { path: 'folios/:id', name: 'folio', component: () => import('@/views/FolioDetail.vue'), meta: { title: '账单' } },
            { path: 'reports', name: 'reports', component: () => import('@/views/ReportIndex.vue'), meta: { title: '夜审与报表' } },
            { path: 'system/staff', name: 'system-staff', component: () => import('@/views/SystemStaff.vue'), meta: { title: '员工管理' } },
        ],
    },
];
const router = createRouter({ history: createWebHistory(), routes });
router.beforeEach((to) => {
    const auth = useAuthStore();
    if (to.name !== 'login' && !auth.token) {
        return { name: 'login', query: { redirect: to.fullPath } };
    }
});
export default router;
