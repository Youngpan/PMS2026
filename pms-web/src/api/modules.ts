import request from './request'

// ---------- 鉴权 ----------
export const authApi = {
  login: (username: string, password: string) =>
    request.post('/auth/login', { username, password }),
  logout: () => request.post('/auth/logout'),
}

// ---------- 房型/房号 ----------
export const roomApi = {
  page: (params: any) => request.get('/rooms', { params }),
  create: (data: any) => request.post('/rooms', data),
  batchCreate: (data: any) => request.post('/rooms/batch', data),
  update: (id: number, data: any) => request.put(`/rooms/${id}`, data),
  setRentStatus: (id: number, rent_status: string) =>
    request.put(`/rooms/${id}/rent-status`, { rent_status }),
  setCleanStatus: (id: number, clean_status: string) =>
    request.put(`/rooms/${id}/clean-status`, { clean_status }),
  setRemark: (id: number, remark: string) =>
    request.put(`/rooms/${id}/remark`, { remark }),
  vacant: (room_type_id?: number) =>
    request.get('/rooms/vacant', { params: { room_type_id } }),
  types: () => request.get('/room-types'),
  createType: (data: any) => request.post('/room-types', data),
  updateType: (id: number, data: any) => request.put(`/room-types/${id}`, data),
}

// ---------- 渠道来源 ----------
export const channelApi = {
  list: () => request.get('/channels'),
}

// ---------- 预订 ----------
export const reservationApi = {
  list: (params: any) => request.get('/reservations', { params }),
  get: (id: number) => request.get(`/reservations/${id}`),
  create: (data: any) => request.post('/reservations', data),
  confirm: (id: number) => request.post(`/reservations/${id}/confirm`),
  cancel: (id: number, reason?: string) =>
    request.post(`/reservations/${id}/cancel`, { reason }),
}

// ---------- 前台/在住 ----------
export const stayApi = {
  checkin: (data: any) => request.post('/stays/checkins', data),
  inHouse: () => request.get('/stays'),
  get: (id: number) => request.get(`/stays/${id}`),
  extend: (id: number, data: { days: number; amount?: number; method?: string; remark?: string }) =>
    request.post(`/stays/${id}/extend`, data),
  hourly: (data: any) => request.post('/stays/hourly', data),
  transferRoom: (id: number, to_room_id: number, reason?: string) =>
    request.post(`/stays/${id}/transfer-room`, { to_room_id, reason }),
  updateRate: (id: number, rate: number) =>
    request.put(`/stays/${id}/rate`, { rate }),
  checkout: (id: number, payment_method: string, amount?: number) =>
    request.post(`/stays/${id}/checkout`, { payment_method, amount }),
  undoCheckin: (id: number) =>
    request.post(`/stays/${id}/undo-checkin`),
}

// ---------- 账务 ----------
export const folioApi = {
  detail: (id: number) => request.get(`/folios/${id}`),
  byStay: (stayId: number) => request.get(`/folios/by-stay/${stayId}`),
  addCharge: (id: number, data: any) => request.post(`/folios/${id}/items`, data),
  addDeposit: (id: number, data: any) => request.post(`/folios/${id}/deposits`, data),
  transfer: (id: number, data: any) => request.post(`/folios/${id}/transfer`, data),
  split: (id: number, data: any) => request.post(`/folios/${id}/split`, data),
  pay: (id: number, method: string, amount: number) =>
    request.post(`/folios/${id}/payments`, { method, amount }),
  receipt: (id: number) => request.post(`/folios/${id}/receipt`),
}

// ---------- 房务 ----------
export const housekeepingApi = {
  dashboard: () => request.get('/housekeeping/dashboard'),
  calendar: (from: string, to: string) =>
    request.get('/housekeeping/calendar', { params: { from, to } }),
  tasks: (params: any) => request.get('/housekeeping/tasks', { params }),
  createTask: (data: any) => request.post('/housekeeping/tasks', data),
  finishTask: (id: number) => request.put(`/housekeeping/tasks/${id}/finish`),
  reportMaintenance: (data: any) => request.post('/housekeeping/maintenance', data),
  fixMaintenance: (id: number) => request.put(`/housekeeping/maintenance/${id}/fix`),
  maintenances: (params: any) => request.get('/housekeeping/maintenance', { params }),
  blockRoom: (room_id: number, start: string, end: string, reason?: string) =>
    request.post('/housekeeping/blocks', { room_id, start, end, reason }),
  releaseBlock: (id: number) => request.put(`/housekeeping/blocks/${id}/release`),
}

// ---------- 报表 ----------
export const reportApi = {
  nightAudit: (date: string) => request.get('/reports/night-audit', { params: { date } }),
  confirmNightAudit: (date: string) =>
    request.post('/reports/night-audit/confirm', { date }),
  revenue: (from: string, to: string) =>
    request.get('/reports/revenue', { params: { from, to } }),
  folioBalance: () => request.get('/reports/folio-balance'),
  occupancy: (from: string, to: string) =>
    request.get('/reports/occupancy', { params: { from, to } }),
  revenueDaily: (from: string, to: string) =>
    request.get('/reports/revenue/daily', { params: { from, to } }),
}

// ---------- 系统管理 ----------
export const systemApi = {
  departments: () => request.get('/departments'),
  createDepartment: (data: any) => request.post('/departments', data),
  staff: () => request.get('/staff'),
  createStaff: (data: any) => request.post('/staff', data),
  resetPassword: (id: number, new_password: string) =>
    request.put(`/staff/${id}/password`, { new_password }),
  toggleStaffStatus: (id: number, status: number) =>
    request.put(`/staff/${id}/status`, { status }),
  assignRoles: (id: number, role_ids: number[]) =>
    request.put(`/staff/${id}/roles`, { role_ids }),
  roles: () => request.get('/roles'),
  createRole: (data: any) => request.post('/roles', data),
  permissions: () => request.get('/roles/permissions'),
  rolePermissions: (id: number) => request.get(`/roles/${id}/permissions`),
  assignPermissions: (id: number, permission_ids: number[]) =>
    request.put(`/roles/${id}/permissions`, { permission_ids }),
  operationLogs: (params: any) => request.get('/operation-logs', { params }),
  config: () => request.get('/config'),
  updateConfig: (data: any) => request.put('/config', data),
}
