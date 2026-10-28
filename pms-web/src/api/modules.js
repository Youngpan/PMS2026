import request from './request';
// ---------- 鉴权 ----------
export const authApi = {
    login: (username, password) => request.post('/auth/login', { username, password }),
    logout: () => request.post('/auth/logout'),
};
// ---------- 房型/房号 ----------
export const roomApi = {
    page: (params) => request.get('/rooms', { params }),
    create: (data) => request.post('/rooms', data),
    changeStatus: (id, status) => request.put(`/rooms/${id}/status`, { status }),
    vacant: (room_type_id) => request.get('/rooms/vacant', { params: { room_type_id } }),
    types: () => request.get('/room-types'),
    createType: (data) => request.post('/room-types', data),
};
// ---------- 预订 ----------
export const reservationApi = {
    list: (params) => request.get('/reservations', { params }),
    get: (id) => request.get(`/reservations/${id}`),
    create: (data) => request.post('/reservations', data),
    confirm: (id) => request.post(`/reservations/${id}/confirm`),
    cancel: (id, reason) => request.post(`/reservations/${id}/cancel`, { reason }),
};
// ---------- 前台/在住 ----------
export const stayApi = {
    checkin: (data) => request.post('/stays/checkins', data),
    inHouse: () => request.get('/stays'),
    get: (id) => request.get(`/stays/${id}`),
    extend: (id, extend_to_date) => request.post(`/stays/${id}/extend`, { extend_to_date }),
    transferRoom: (id, to_room_id, reason) => request.post(`/stays/${id}/transfer-room`, { to_room_id, reason }),
    updateRate: (id, rate) => request.put(`/stays/${id}/rate`, { rate }),
    checkout: (id, payment_method, amount) => request.post(`/stays/${id}/checkout`, { payment_method, amount }),
};
// ---------- 账务 ----------
export const folioApi = {
    detail: (id) => request.get(`/folios/${id}`),
    byStay: (stayId) => request.get(`/folios/by-stay/${stayId}`),
    addCharge: (id, data) => request.post(`/folios/${id}/items`, data),
    addDeposit: (id, data) => request.post(`/folios/${id}/deposits`, data),
    transfer: (id, data) => request.post(`/folios/${id}/transfer`, data),
    split: (id, data) => request.post(`/folios/${id}/split`, data),
    pay: (id, method, amount) => request.post(`/folios/${id}/payments`, { method, amount }),
    receipt: (id) => request.post(`/folios/${id}/receipt`),
};
// ---------- 房务 ----------
export const housekeepingApi = {
    dashboard: () => request.get('/housekeeping/dashboard'),
    tasks: (params) => request.get('/housekeeping/tasks', { params }),
    createTask: (data) => request.post('/housekeeping/tasks', data),
    finishTask: (id) => request.put(`/housekeeping/tasks/${id}/finish`),
    reportMaintenance: (data) => request.post('/housekeeping/maintenance', data),
    fixMaintenance: (id) => request.put(`/housekeeping/maintenance/${id}/fix`),
    maintenances: (params) => request.get('/housekeeping/maintenance', { params }),
};
// ---------- 报表 ----------
export const reportApi = {
    nightAudit: (date) => request.get('/reports/night-audit', { params: { date } }),
    confirmNightAudit: (date) => request.post('/reports/night-audit/confirm', { date }),
    revenue: (from, to) => request.get('/reports/revenue', { params: { from, to } }),
    folioBalance: () => request.get('/reports/folio-balance'),
    occupancy: (from, to) => request.get('/reports/occupancy', { params: { from, to } }),
    revenueDaily: (from, to) => request.get('/reports/revenue/daily', { params: { from, to } }),
};
// ---------- 系统管理 ----------
export const systemApi = {
    departments: () => request.get('/departments'),
    createDepartment: (data) => request.post('/departments', data),
    staff: () => request.get('/staff'),
    createStaff: (data) => request.post('/staff', data),
    resetPassword: (id, new_password) => request.put(`/staff/${id}/password`, { new_password }),
    toggleStaffStatus: (id, status) => request.put(`/staff/${id}/status`, { status }),
    assignRoles: (id, role_ids) => request.put(`/staff/${id}/roles`, { role_ids }),
    roles: () => request.get('/roles'),
    createRole: (data) => request.post('/roles', data),
    permissions: () => request.get('/roles/permissions'),
    rolePermissions: (id) => request.get(`/roles/${id}/permissions`),
    assignPermissions: (id, permission_ids) => request.put(`/roles/${id}/permissions`, { permission_ids }),
    operationLogs: (params) => request.get('/operation-logs', { params }),
    config: () => request.get('/config'),
    updateConfig: (data) => request.put('/config', data),
};
