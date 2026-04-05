import { getAdminMe } from '../api/admin'

const ADMIN_ME_KEY = 'admin_me_cache'
const ADMIN_PERMISSIONS_KEY = 'admin_permissions'

export const clearAdminCache = () => {
  sessionStorage.removeItem(ADMIN_ME_KEY)
  sessionStorage.removeItem(ADMIN_PERMISSIONS_KEY)
}

export const getAdminPermissionsFromCache = () => {
  try {
    const value = sessionStorage.getItem(ADMIN_PERMISSIONS_KEY)
    const parsed = value ? JSON.parse(value) : []
    return Array.isArray(parsed) ? parsed : []
  } catch (_) {
    return []
  }
}

export const getAdminMeFromCache = () => {
  try {
    const value = sessionStorage.getItem(ADMIN_ME_KEY)
    return value ? JSON.parse(value) : null
  } catch (_) {
    return null
  }
}

export const refreshAdminMe = async () => {
  const response = await getAdminMe()
  if (response.data?.code !== 200 || !response.data?.data) {
    throw new Error(response.data?.message || 'Failed to load admin profile')
  }
  const data = response.data.data
  const permissions = Array.isArray(data.permissions) ? data.permissions : []
  sessionStorage.setItem(ADMIN_ME_KEY, JSON.stringify(data))
  sessionStorage.setItem(ADMIN_PERMISSIONS_KEY, JSON.stringify(permissions))
  window.dispatchEvent(new Event('admin-state-changed'))
  return data
}

export const ensureAdminMe = async () => {
  const cached = getAdminMeFromCache()
  if (cached && Array.isArray(cached.permissions) && cached.permissions.length > 0) {
    return cached
  }
  return refreshAdminMe()
}

export const hasAdminPermission = (permissionCode) => {
  if (!permissionCode) return true
  return getAdminPermissionsFromCache().includes(permissionCode)
}
