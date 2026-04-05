<template>
  <section class="publish-page">
    <div class="publish-hero motion-fade-up">
      <p class="hero-kicker">CREATE CONTENT</p>
      <h1>{{ isEditMode ? '打磨你的内容细节' : '发布一条有质感的生活内容' }}</h1>
      <p>支持图文上传、自动压缩与标签管理，尽量让你的创作体验更流畅。</p>
    </div>

    <el-card class="publish-card motion-fade-soft">
      <template #header>
        <div class="card-header">
          <h2>{{ isEditMode ? '编辑内容' : '发布内容' }}</h2>
          <span class="header-tip">图片上传上限 5MB，建议使用平衡压缩档位</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入标题" />
        </el-form-item>

        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            default-first-option
            :reserve-keyword="false"
            placeholder="请选择标签（最多5个）"
            style="width: 100%"
          >
            <el-option
              v-for="tag in tagOptions"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :autosize="{ minRows: 8, maxRows: 16 }" placeholder="分享一下你的生活吧..." />
        </el-form-item>

        <el-form-item label="压缩">
          <div class="compress-wrap">
            <el-switch v-model="autoCompress" active-text="上传前自动压缩（推荐）" />
            <div class="compress-level">
              <span class="compress-label">压缩档位：</span>
              <el-radio-group v-model="compressionLevel" size="small">
                <el-radio-button label="high">高画质</el-radio-button>
                <el-radio-button label="balanced">平衡</el-radio-button>
                <el-radio-button label="aggressive">高压缩</el-radio-button>
              </el-radio-group>
            </div>
            <p class="compress-tip">JPEG/PNG/WEBP 会自动压缩，GIF 保持原图。你可以切换档位观察压缩效果差异。</p>
            <div v-if="compressionRecords.length > 0" class="compress-records">
              <div class="compress-record-header">最近压缩记录</div>
              <div v-for="record in compressionRecords" :key="record.uid" class="compress-record-item">
                <span class="record-name">{{ record.name }}</span>
                <span class="record-size">{{ record.beforeText }} -> {{ record.afterText }}</span>
                <span class="record-status">{{ record.message }}</span>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="图片">
          <el-upload
            action="/api/content/upload"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :on-change="handleUploadChange"
            :on-preview="handlePreview"
            :before-upload="beforeUpload"
            :on-remove="handleRemove"
            v-model:file-list="fileList"
            list-type="picture-card"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :limit="9"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="视频">
          <el-upload
            class="video-upload-trigger"
            action="/api/content/upload/video"
            :headers="uploadHeaders"
            :on-success="handleVideoUploadSuccess"
            :on-error="handleVideoUploadError"
            :on-change="handleVideoUploadChange"
            :before-upload="beforeVideoUpload"
            :on-remove="handleVideoRemove"
            v-model:file-list="videoFileList"
            accept="video/mp4,video/webm,video/quicktime,.mp4,.webm,.mov"
            :limit="1"
          >
            <el-button class="video-upload-btn" type="primary">
              <el-icon><VideoPlay /></el-icon>
              <span>上传视频</span>
            </el-button>
          </el-upload>
          <div class="video-tip">支持 MP4/WEBM/MOV，单文件最大 80MB。</div>
          <div v-if="videoPosterLoading" class="video-poster-card">
            <span class="video-poster-loading">正在提取视频封面...</span>
          </div>
          <div v-else-if="videoPosterPreview" class="video-poster-card">
            <img :src="videoPosterPreview" alt="video-cover" class="video-poster-image" />
            <span class="video-poster-tip">已自动提取封面（仅预览，便于确认内容）</span>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">{{ isEditMode ? '保存修改' : '立即发布' }}</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog
      v-model="previewVisible"
      title="图片预览"
      width="70%"
      top="6vh"
      append-to-body
    >
      <div class="preview-dialog-body">
        <img v-if="previewImageUrl" :src="previewImageUrl" alt="preview" class="preview-image" />
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCategories, getContentDetail, getTags, publishContent, updateContent } from '../api/content'
import { extractVideoPoster } from '../utils/media'

/**
 * 发布页职责：
 * 1. 承载“新建 + 编辑”统一表单，保持字段口径一致。
 * 2. 对媒体上传做前置校验（类型/大小）与可选压缩，减少后端失败重试成本。
 * 3. 在编辑场景过滤禁用标签，避免历史脏数据继续写回。
 */
const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const categories = ref([])
const tagOptions = ref([])
const fileList = ref([])
const videoFileList = ref([])
const autoCompress = ref(true)
const compressionLevel = ref('balanced')
const compressionRecords = ref([])
const previewVisible = ref(false)
const previewImageUrl = ref('')
const videoPosterPreview = ref('')
const videoPosterLoading = ref(false)

const MAX_UPLOAD_BYTES = 5 * 1024 * 1024
const MAX_VIDEO_UPLOAD_BYTES = 80 * 1024 * 1024
// 三档压缩策略用于平衡“清晰度、体积、上传成功率”，只影响上传前临时文件。
const compressionLevelConfig = {
  high: {
    triggerBytes: 1 * 1024 * 1024,
    targetBytes: Math.floor(1.8 * 1024 * 1024),
    maxDimension: 1920,
    qualities: [0.9, 0.86, 0.82, 0.78],
    pngWebpQuality: 0.86
  },
  balanced: {
    triggerBytes: 1 * 1024 * 1024,
    targetBytes: Math.floor(1.2 * 1024 * 1024),
    maxDimension: 1600,
    qualities: [0.84, 0.78, 0.72, 0.66],
    pngWebpQuality: 0.8
  },
  aggressive: {
    triggerBytes: 512 * 1024,
    targetBytes: Math.floor(0.8 * 1024 * 1024),
    maxDimension: 1280,
    qualities: [0.72, 0.64, 0.56, 0.5],
    pngWebpQuality: 0.72
  }
}

const isEditMode = computed(() => Boolean(route.params.id))

const form = reactive({
  title: '',
  content: '',
  categoryId: null,
  tags: [],
  images: '',
  videos: ''
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 100, message: '标题长度在 1-100 字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${sessionStorage.getItem('token') || ''}`
}))

// 上传前强校验：仅允许白名单格式，并把最终大小控制在后端限制以内。
const beforeUpload = async (file) => {
  const validType = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  if (!validType) {
    ElMessage.error('仅支持 JPG、PNG、GIF、WEBP')
    return false
  }

  const processedFile = await compressImageIfNeeded(file)
  const validSize = processedFile.size <= MAX_UPLOAD_BYTES
  if (!validSize) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return processedFile
}

// 后端返回路径后统一回填 fileList，并同步 form.images 供提交使用。
const handleUploadSuccess = (response, uploadFile) => {
  if (response.code !== 200) {
    ElMessage.error(response.message || '上传失败')
    return
  }
  uploadFile.url = normalizeFileUrl(response.data)
  syncImagesFromFileList()
  ElMessage.success('上传成功')
}

const handleUploadError = (error) => {
  let message = error?.message || '上传失败，请稍后重试'
  const status = error?.status
  if (status === 413) {
    message = '图片超过上传限制（最大 5MB）'
  } else if (status === 400) {
    message = '图片上传失败，请检查格式与大小限制'
  }
  ElMessage.error(message)
}

const handleUploadChange = (_, uploadFiles) => {
  fileList.value = uploadFiles
  syncImagesFromFileList()
}

const handlePreview = (uploadFile) => {
  const url = uploadFile.url || uploadFile.response?.data
  if (!url) {
    ElMessage.warning('当前图片暂不可预览')
    return
  }
  previewImageUrl.value = normalizeFileUrl(url)
  previewVisible.value = true
}

const handleRemove = (_, uploadFiles) => {
  fileList.value = uploadFiles || []
  syncImagesFromFileList()
}

// 视频上传只保留 1 条，防止单条内容承载过多媒体导致审核与展示复杂度上升。
const beforeVideoUpload = (file) => {
  const allowedTypes = ['video/mp4', 'video/webm', 'video/quicktime']
  const fileName = String(file.name || '').toLowerCase()
  const allowedExtensions = ['.mp4', '.webm', '.mov']
  const validType = allowedTypes.includes(file.type) || allowedExtensions.some((ext) => fileName.endsWith(ext))
  if (!validType) {
    ElMessage.error('仅支持 MP4、WEBM、MOV 视频')
    return false
  }
  if (file.size > MAX_VIDEO_UPLOAD_BYTES) {
    ElMessage.error('视频大小不能超过 80MB')
    return false
  }
  return true
}

const handleVideoUploadSuccess = (response, uploadFile) => {
  if (response.code !== 200) {
    ElMessage.error(response.message || '视频上传失败')
    return
  }
  uploadFile.url = normalizeFileUrl(response.data)
  syncVideosFromFileList()
  void refreshVideoPosterPreview()
  ElMessage.success('视频上传成功')
}

const handleVideoUploadError = (error) => {
  let message = error?.message || '视频上传失败，请稍后重试'
  const status = error?.status
  if (status === 413) {
    message = '视频超过上传限制（最大 80MB）'
  } else if (status === 400) {
    message = '视频上传失败，请检查格式与大小限制'
  }
  ElMessage.error(message)
}

const handleVideoUploadChange = (_, uploadFiles) => {
  videoFileList.value = (uploadFiles || []).slice(0, 1)
  syncVideosFromFileList()
  void refreshVideoPosterPreview()
}

const handleVideoRemove = (_, uploadFiles) => {
  videoFileList.value = uploadFiles || []
  syncVideosFromFileList()
  void refreshVideoPosterPreview()
}

const normalizeFileUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return url
  return `/${url}`
}

const formatSize = (bytes) => `${(bytes / 1024 / 1024).toFixed(2)}MB`

// 压缩记录用于向用户解释“为什么体积变化”，避免压缩行为黑盒化。
const addCompressionRecord = (record) => {
  compressionRecords.value = [
    record,
    ...compressionRecords.value.filter((item) => item.uid !== record.uid)
  ].slice(0, 8)
}

const readImageFromFile = (file) =>
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
    const targetHeight = Math.round((height / width) * maxDimension)
    return { width: targetWidth, height: targetHeight }
  }
  const targetHeight = maxDimension
  const targetWidth = Math.round((width / height) * maxDimension)
  return { width: targetWidth, height: targetHeight }
}

const compressImageIfNeeded = async (file) => {
  const compressibleTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!autoCompress.value || !compressibleTypes.includes(file.type)) {
    return file
  }

  const currentLevel = compressionLevel.value
  const config = compressionLevelConfig[currentLevel]
  if (!config || file.size < config.triggerBytes) {
    addCompressionRecord({
      uid: file.uid,
      name: file.name,
      beforeText: formatSize(file.size),
      afterText: formatSize(file.size),
      message: '文件较小，未压缩'
    })
    return file
  }

  try {
    const image = await readImageFromFile(file)
    const size = resizeByMaxDimension(image.width, image.height, config.maxDimension)
    const canvas = document.createElement('canvas')
    canvas.width = size.width
    canvas.height = size.height
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      return file
    }
    ctx.drawImage(image, 0, 0, size.width, size.height)

    let outputType = file.type
    let bestBlob = null

    // PNG 先保真导出，超目标体积时再尝试转 WEBP，兼顾质量与收益。
    if (file.type === 'image/png') {
      bestBlob = await canvasToBlob(canvas, 'image/png')
      if (bestBlob && bestBlob.size > config.targetBytes) {
        const webpBlob = await canvasToBlob(canvas, 'image/webp', config.pngWebpQuality)
        if (webpBlob && webpBlob.size < bestBlob.size) {
          bestBlob = webpBlob
          outputType = 'image/webp'
        }
      }
    } else {
      for (const quality of config.qualities) {
        const blob = await canvasToBlob(canvas, outputType, quality)
        if (!blob) {
          continue
        }
        if (!bestBlob || blob.size < bestBlob.size) {
          bestBlob = blob
        }
        if (blob.size <= config.targetBytes) {
          bestBlob = blob
          break
        }
      }
    }

    if (!bestBlob || bestBlob.size >= file.size) {
      addCompressionRecord({
        uid: file.uid,
        name: file.name,
        beforeText: formatSize(file.size),
        afterText: formatSize(file.size),
        message: '压缩收益较小，保留原图'
      })
      return file
    }

    let fileName = file.name
    if (outputType === 'image/webp' && !fileName.toLowerCase().endsWith('.webp')) {
      fileName = fileName.replace(/\.[^.]+$/, '.webp')
    }

    const compressedFile = new File([bestBlob], fileName, {
      type: outputType,
      lastModified: Date.now()
    })
    compressedFile.uid = file.uid

    const savedPercent = Math.max(0, ((file.size - compressedFile.size) / file.size) * 100)
    addCompressionRecord({
      uid: file.uid,
      name: file.name,
      beforeText: formatSize(file.size),
      afterText: formatSize(compressedFile.size),
      message: `已压缩，节省 ${savedPercent.toFixed(1)}%`
    })
    ElMessage.success(`已压缩：${formatSize(file.size)} -> ${formatSize(compressedFile.size)}（-${savedPercent.toFixed(1)}%）`)
    return compressedFile
  } catch (_) {
    addCompressionRecord({
      uid: file.uid,
      name: file.name,
      beforeText: formatSize(file.size),
      afterText: formatSize(file.size),
      message: '压缩失败，使用原图'
    })
    ElMessage.warning('图片压缩失败，已使用原图上传')
    return file
  }
}

// 以 fileList 为唯一真源，避免 UI 删除图片后 form.images 未同步的问题。
const syncImagesFromFileList = () => {
  form.images = fileList.value
    .map((item) => item.url || item.response?.data)
    .map((url) => normalizeFileUrl(url))
    .filter(Boolean)
    .join(',')
}

const syncVideosFromFileList = () => {
  form.videos = videoFileList.value
    .map((item) => item.url || item.response?.data)
    .map((url) => normalizeFileUrl(url))
    .filter(Boolean)
    .slice(0, 1)
    .join(',')
}

const getFirstVideoSource = () => {
  if (!videoFileList.value.length) return null
  const item = videoFileList.value[0]
  return item.raw || item.url || item.response?.data || null
}

const refreshVideoPosterPreview = async () => {
  const source = getFirstVideoSource()
  if (!source) {
    videoPosterPreview.value = ''
    videoPosterLoading.value = false
    return
  }

  videoPosterLoading.value = true
  try {
    const normalizedSource = typeof source === 'string' ? normalizeFileUrl(source) : source
    const poster = await extractVideoPoster(normalizedSource, {
      seekSeconds: 0.8,
      maxWidth: 900,
      quality: 0.82
    })
    videoPosterPreview.value = poster || ''
  } catch (_) {
    videoPosterPreview.value = ''
  } finally {
    videoPosterLoading.value = false
  }
}

// 编辑模式下会自动剔除禁用标签，确保“历史保留可见、重新发布不可写入禁用标签”规则生效。
const loadCategories = async () => {
  try {
    const response = await getCategories()
    categories.value = response.data.data || []
  } catch (_) {
    ElMessage.error('加载分类失败')
  }
}

const loadTags = async () => {
  try {
    const response = await getTags()
    const tags = response.data.data || []
    tagOptions.value = tags
      .map((item) => item.name)
      .filter(Boolean)
  } catch (_) {
    ElMessage.error('加载标签失败')
  }
}

const loadDetailForEdit = async () => {
  if (!isEditMode.value) return
  try {
    const response = await getContentDetail(route.params.id)
    const data = response.data.data
    form.title = data.title || ''
    form.content = data.content || ''
    form.categoryId = data.categoryId || null
    form.tags = data.tags ? data.tags.split(',').filter(Boolean) : []
    const activeTagSet = new Set(tagOptions.value)
    const beforeCount = form.tags.length
    form.tags = form.tags.filter((tag) => activeTagSet.has(tag))
    if (beforeCount !== form.tags.length) {
      ElMessage.warning('检测到已禁用标签，编辑页已自动移除这些标签')
    }
    form.images = data.images || ''
    form.videos = data.videos || ''
    fileList.value = form.images
      ? form.images.split(',').filter(Boolean).map((url, index) => ({
          name: `image-${index + 1}`,
          url: normalizeFileUrl(url)
        }))
      : []
    videoFileList.value = form.videos
      ? form.videos.split(',').filter(Boolean).slice(0, 1).map((url, index) => ({
          name: `video-${index + 1}`,
          url: normalizeFileUrl(url)
        }))
      : []
    await refreshVideoPosterPreview()
  } catch (_) {
    ElMessage.error('加载内容详情失败')
    router.push('/profile')
  }
}

/**
 * 提交规则：
 * - 提交前强制重算 images/videos，避免界面状态与 payload 脱节；
 * - 编辑后进入“待审核”，新发布提示“审核通过后展示”，与后端审核策略一致。
 */
const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 双保险：提交前按当前文件列表重新同步图片字段
  syncImagesFromFileList()
  syncVideosFromFileList()

  const normalizedTags = form.tags
    .map((tag) => String(tag).trim())
    .filter(Boolean)

  if (normalizedTags.length > 5) {
    ElMessage.warning('最多选择 5 个标签')
    return
  }
  if (normalizedTags.some((tag) => tag.length > 15)) {
    ElMessage.warning('单个标签长度不能超过 15 个字符')
    return
  }

  submitting.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      categoryId: form.categoryId,
      tags: normalizedTags.join(','),
      images: form.images,
      videos: form.videos
    }
    if (isEditMode.value) {
      await updateContent(route.params.id, payload)
      ElMessage.success('修改成功，内容已进入待审核')
      router.push('/profile')
      return
    }
    await publishContent(payload)
    ElMessage.success('发布成功，审核通过后会展示在首页')
    resetForm()
    router.push('/')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || (isEditMode.value ? '修改失败' : '发布失败'))
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  form.tags = []
  form.images = ''
  form.videos = ''
  fileList.value = []
  videoFileList.value = []
  videoPosterPreview.value = ''
  videoPosterLoading.value = false
  compressionRecords.value = []
}

onMounted(async () => {
  await loadCategories()
  await loadTags()
  await loadDetailForEdit()
})
</script>

<style scoped>
.publish-page {
  max-width: 940px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.publish-hero {
  border-radius: 18px;
  padding: 20px 22px;
  background:
    radial-gradient(circle at 85% 20%, rgba(255, 255, 255, 0.24), transparent 46%),
    linear-gradient(140deg, #0ea5a4, #14b8a6 52%, #f97316);
  box-shadow: 0 18px 36px rgba(14, 165, 164, 0.24);
  color: #f0fdfa;
}

.hero-kicker {
  font-size: 12px;
  letter-spacing: 1.5px;
  opacity: 0.9;
}

.publish-hero h1 {
  margin-top: 8px;
  font-size: clamp(24px, 3vw, 32px);
  line-height: 1.28;
}

.publish-hero p {
  margin-top: 10px;
  color: rgba(240, 253, 250, 0.92);
  line-height: 1.72;
}

.publish-card {
  border-radius: 18px;
  animation-delay: 0.1s;
}

.card-header h2 {
  margin: 0;
  font-size: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-tip {
  font-size: 12px;
  color: #6d7c91;
}

.compress-wrap {
  width: 100%;
  border-radius: 12px;
  padding: 12px;
  background: rgba(14, 165, 164, 0.06);
  border: 1px dashed rgba(14, 165, 164, 0.24);
}

.compress-tip {
  margin: 6px 0 0;
  color: #7b879b;
  font-size: 12px;
}

.compress-level {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.compress-label {
  color: #57657a;
  font-size: 13px;
}

.compress-records {
  margin-top: 10px;
  border: 1px solid #d5eceb;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.74);
  padding: 8px 10px;
}

.compress-record-header {
  font-size: 12px;
  color: #63738a;
  margin-bottom: 6px;
}

.compress-record-item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 10px;
  font-size: 12px;
  color: #44546b;
  padding: 4px 0;
}

.record-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-size {
  color: #5f6f86;
}

.record-status {
  color: #2f7a3f;
}

.preview-dialog-body {
  width: 100%;
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image {
  max-width: 100%;
  max-height: 72vh;
  object-fit: contain;
}

.video-tip {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.video-upload-trigger {
  display: inline-flex;
}

:deep(.video-upload-btn) {
  height: 40px;
  min-width: 132px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid #0f766e;
  background: linear-gradient(135deg, #0ea5a4 0%, #0f766e 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.2px;
  box-shadow: 0 10px 18px rgba(14, 165, 164, 0.24);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

:deep(.video-upload-btn .el-icon) {
  margin-right: 6px;
  font-size: 16px;
}

:deep(.video-upload-btn:hover),
:deep(.video-upload-btn:focus-visible) {
  transform: translateY(-1px);
  filter: brightness(1.04);
  box-shadow: 0 12px 22px rgba(14, 165, 164, 0.3);
}

:deep(.video-upload-btn.is-disabled),
:deep(.video-upload-btn.is-disabled:hover) {
  background: linear-gradient(135deg, #c7eeeb 0%, #97dbd6 100%);
  border-color: #86cfc9;
  color: rgba(255, 255, 255, 0.96);
  box-shadow: none;
  transform: none;
  filter: none;
}

.video-poster-card {
  margin-top: 10px;
  width: min(280px, 100%);
  border: 1px solid rgba(14, 165, 164, 0.24);
  border-radius: 10px;
  background: rgba(236, 253, 245, 0.4);
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.video-poster-loading {
  color: #5f738a;
  font-size: 12px;
}

.video-poster-image {
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.08);
}

.video-poster-tip {
  color: #4b5f77;
  font-size: 12px;
}

@media (max-width: 760px) {
  .publish-hero {
    padding: 16px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
