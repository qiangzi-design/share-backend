const VIEWER_KEY_STORAGE_KEY = 'viewer_key'

export const getViewerKey = () => {
  let viewerKey = localStorage.getItem(VIEWER_KEY_STORAGE_KEY)
  if (!viewerKey) {
    viewerKey = `v_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`
    localStorage.setItem(VIEWER_KEY_STORAGE_KEY, viewerKey)
  }
  return viewerKey
}

