import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('pms_token') || '')
  const staffId = ref<number>(Number(localStorage.getItem('pms_staff_id') || 0))
  const realName = ref<string>(localStorage.getItem('pms_real_name') || '')
  const roles = ref<string[]>(JSON.parse(localStorage.getItem('pms_roles') || '[]'))

  function set(session: { token: string; staffId: number; realName: string; roles: string[] }) {
    token.value = session.token
    staffId.value = session.staffId
    realName.value = session.realName
    roles.value = session.roles
    localStorage.setItem('pms_token', session.token)
    localStorage.setItem('pms_staff_id', String(session.staffId))
    localStorage.setItem('pms_real_name', session.realName)
    localStorage.setItem('pms_roles', JSON.stringify(session.roles))
  }

  function clear() {
    token.value = ''
    staffId.value = 0
    realName.value = ''
    roles.value = []
    localStorage.removeItem('pms_token')
    localStorage.removeItem('pms_staff_id')
    localStorage.removeItem('pms_real_name')
    localStorage.removeItem('pms_roles')
  }

  return { token, staffId, realName, roles, set, clear }
})
