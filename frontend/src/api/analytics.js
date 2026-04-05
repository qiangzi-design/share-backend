import apiClient from './client'
import { getViewerKey } from '../utils/viewerKey'

export const reportSiteUvPing = () =>
  apiClient.post('/analytics/uv/ping', null, {
    headers: {
      'X-Viewer-Id': getViewerKey()
    }
  })

