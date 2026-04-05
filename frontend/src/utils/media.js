const DEFAULT_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

const readImageElement = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const image = new Image()
      image.onload = () => resolve(image)
      image.onerror = reject
      image.src = reader.result
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })

const canvasToBlob = (canvas, type, quality) =>
  new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob), type, quality)
  })

const resizeByMaxDimension = (width, height, maxDimension) => {
  if (width <= maxDimension && height <= maxDimension) {
    return { width, height }
  }
  if (width >= height) {
    const targetWidth = maxDimension
    const targetHeight = Math.max(1, Math.round((height / width) * maxDimension))
    return { width: targetWidth, height: targetHeight }
  }
  const targetHeight = maxDimension
  const targetWidth = Math.max(1, Math.round((width / height) * maxDimension))
  return { width: targetWidth, height: targetHeight }
}

const replaceExtension = (fileName, extension) => {
  const hasExtension = /\.[^./\\]+$/.test(fileName)
  if (hasExtension) {
    return fileName.replace(/\.[^./\\]+$/, extension)
  }
  return `${fileName}${extension}`
}

export const formatBytes = (bytes) => {
  const size = Number(bytes || 0)
  if (size <= 0) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / (1024 * 1024)).toFixed(2)} MB`
  return `${(size / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

export const compressImageFile = async (
  file,
  options = {}
) => {
  const {
    minBytesToCompress = 900 * 1024,
    targetBytes = 1.2 * 1024 * 1024,
    maxDimension = 1600,
    qualityList = [0.84, 0.76, 0.68, 0.6],
    pngToWebpQuality = 0.8,
    allowedTypes = DEFAULT_IMAGE_TYPES
  } = options

  const fileType = String(file?.type || '').toLowerCase()
  const beforeBytes = Number(file?.size || 0)
  if (!file || !allowedTypes.has(fileType) || beforeBytes < minBytesToCompress) {
    return {
      file,
      compressed: false,
      beforeBytes,
      afterBytes: beforeBytes
    }
  }

  const image = await readImageElement(file)
  const size = resizeByMaxDimension(image.width, image.height, maxDimension)
  const canvas = document.createElement('canvas')
  canvas.width = size.width
  canvas.height = size.height
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return {
      file,
      compressed: false,
      beforeBytes,
      afterBytes: beforeBytes
    }
  }
  ctx.drawImage(image, 0, 0, size.width, size.height)

  let outputType = fileType
  let bestBlob = null

  if (fileType === 'image/png') {
    const pngBlob = await canvasToBlob(canvas, 'image/png')
    bestBlob = pngBlob
    if (pngBlob && pngBlob.size > targetBytes) {
      const webpBlob = await canvasToBlob(canvas, 'image/webp', pngToWebpQuality)
      if (webpBlob && (!bestBlob || webpBlob.size < bestBlob.size)) {
        bestBlob = webpBlob
        outputType = 'image/webp'
      }
    }
  } else {
    for (const quality of qualityList) {
      const blob = await canvasToBlob(canvas, outputType, quality)
      if (!blob) continue
      if (!bestBlob || blob.size < bestBlob.size) {
        bestBlob = blob
      }
      if (blob.size <= targetBytes) break
    }
  }

  if (!bestBlob || bestBlob.size >= beforeBytes) {
    return {
      file,
      compressed: false,
      beforeBytes,
      afterBytes: beforeBytes
    }
  }

  let outputName = file.name || `image-${Date.now()}`
  if (outputType === 'image/webp') {
    outputName = replaceExtension(outputName, '.webp')
  }

  const compressedFile = new File([bestBlob], outputName, {
    type: outputType,
    lastModified: Date.now()
  })
  compressedFile.uid = file.uid

  return {
    file: compressedFile,
    compressed: true,
    beforeBytes,
    afterBytes: compressedFile.size
  }
}

const posterPromiseCache = new Map()

const capturePosterFromSource = (source, options) =>
  new Promise((resolve) => {
    const { seekSeconds, maxWidth, quality, timeoutMs } = options
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.muted = true
    video.playsInline = true
    video.crossOrigin = 'anonymous'

    let objectUrl = ''
    const isFile = source instanceof File
    if (isFile) {
      objectUrl = URL.createObjectURL(source)
      video.src = objectUrl
    } else {
      video.src = String(source || '')
    }

    let settled = false
    const settle = (poster) => {
      if (settled) return
      settled = true
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
      resolve(poster || '')
    }

    const timer = window.setTimeout(() => settle(''), timeoutMs)

    const tryCapture = () => {
      const width = video.videoWidth
      const height = video.videoHeight
      if (!width || !height) {
        window.clearTimeout(timer)
        settle('')
        return
      }
      const scale = width > maxWidth ? maxWidth / width : 1
      const canvas = document.createElement('canvas')
      canvas.width = Math.max(1, Math.round(width * scale))
      canvas.height = Math.max(1, Math.round(height * scale))
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        window.clearTimeout(timer)
        settle('')
        return
      }
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
      window.clearTimeout(timer)
      settle(canvas.toDataURL('image/jpeg', quality))
    }

    video.addEventListener('error', () => {
      window.clearTimeout(timer)
      settle('')
    })

    video.addEventListener('loadeddata', () => {
      const targetTime = Number.isFinite(video.duration) && video.duration > seekSeconds
        ? seekSeconds
        : 0
      if (targetTime <= 0) {
        tryCapture()
        return
      }
      const onSeeked = () => {
        video.removeEventListener('seeked', onSeeked)
        tryCapture()
      }
      video.addEventListener('seeked', onSeeked)
      try {
        video.currentTime = targetTime
      } catch (_) {
        video.removeEventListener('seeked', onSeeked)
        tryCapture()
      }
    })
  })

export const extractVideoPoster = async (source, options = {}) => {
  if (!source) return ''

  const mergedOptions = {
    seekSeconds: 0.6,
    maxWidth: 960,
    quality: 0.84,
    timeoutMs: 12000,
    ...options
  }

  const cacheKey = typeof source === 'string' ? source : null
  if (cacheKey && posterPromiseCache.has(cacheKey)) {
    return posterPromiseCache.get(cacheKey)
  }

  const promise = capturePosterFromSource(source, mergedOptions)
  if (cacheKey) {
    posterPromiseCache.set(cacheKey, promise)
  }

  const poster = await promise
  if (!poster && cacheKey) {
    posterPromiseCache.delete(cacheKey)
  }
  return poster
}
