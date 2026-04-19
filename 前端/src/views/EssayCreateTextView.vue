<template>
  <div class="fs-grid">
    <div class="fs-grid fs-grid-two">
      <div class="fs-card">
        <div class="fs-card-header">
          <div class="fs-card-title">文本创建草稿</div>
        </div>
        <div class="fs-card-body">
          <form class="fs-form" @submit.prevent="onSubmitText">
            <div class="fs-form-item">
              <label class="fs-form-label">标题</label>
              <input v-model="title" class="fs-input" placeholder="请输入作文标题" required />
            </div>
            <div class="fs-form-item">
              <label class="fs-form-label">作文内容</label>
              <textarea
                v-model="content"
                class="fs-textarea create-textarea"
                placeholder="在这里粘贴或输入完整作文内容"
                required
              />
            </div>
            <div class="word-count">当前字数 {{ content.length }}</div>
            <button type="submit" class="fs-btn fs-btn-primary btn-ripple shine-effect" :disabled="loadingText">
              <span v-if="loadingText" class="loading-spinner"></span>
              {{ loadingText ? '提交中...' : '保存文本草稿' }}
            </button>
          </form>
        </div>
      </div>

      <div class="fs-stack">
        <div class="fs-card">
          <div class="fs-card-header">
            <div class="fs-card-title">图片上传创建</div>
          </div>
          <div class="fs-card-body">
            <form class="fs-form" @submit.prevent="onSubmitImage">
              <div class="fs-form-item">
                <label class="fs-form-label">图片标题</label>
                <input v-model="imageTitle" class="fs-input" placeholder="请输入作文标题" required />
              </div>
              <div class="fs-form-item">
                <label class="fs-form-label">上传作文图片</label>
                <input
                  id="essay-image-files"
                  type="file"
                  accept=".jpg,.jpeg,.png,.gif,.bmp"
                  multiple
                  @change="onImageChange"
                  class="fs-file-input"
                />
                <div class="upload-card">
                  <label for="essay-image-files" class="upload-card__header" title="选择作文图片">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M7 10V9C7 6.23858 9.23858 4 12 4C14.7614 4 17 6.23858 17 9V10C19.2091 10 21 11.7909 21 14C21 15.4806 20.1956 16.8084 19 17.5M7 10C4.79086 10 3 11.7909 3 14C3 15.4806 3.8044 16.8084 5 17.5M7 10C7.43285 10 7.84965 10.0688 8.24006 10.1959M12 12V21M12 12L15 15M12 12L9 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path>
                    </svg>
                    <p>Browse File to upload!</p>
                  </label>
                  <div class="upload-card__footer">
                    <label for="essay-image-files" class="upload-card__footer-main">
                      <svg viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
                        <path d="M15.331 6H8.5v20h15V14.154h-8.169z"></path>
                        <path d="M18.153 6h-.009v5.342H23.5v-.002z"></path>
                      </svg>
                      <p>{{ imagePreview.length ? `已选择 ${imagePreview.length} 张图片` : 'Not selected file' }}</p>
                    </label>
                    <button v-if="imagePreview.length" type="button" class="upload-card__clear" @click="clearImages" title="清空已选图片">
                      <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M5.16565 10.1534C5.07629 8.99181 5.99473 8 7.15975 8H16.8402C18.0053 8 18.9237 8.9918 18.8344 10.1534L18.142 19.1534C18.0619 20.1954 17.193 21 16.1479 21H7.85206C6.80699 21 5.93811 20.1954 5.85795 19.1534L5.16565 10.1534Z" stroke="currentColor" stroke-width="2"></path>
                        <path d="M19.5 5H4.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"></path>
                        <path d="M10 3C10 2.44772 10.4477 2 11 2H13C13.5523 2 14 2.44772 14 3V5H10V3Z" stroke="currentColor" stroke-width="2"></path>
                      </svg>
                    </button>
                  </div>
                </div>
                <div class="upload-hint">建议上传竖版拍摄图片，避免横向或倒置导致识别不稳定。</div>
              </div>
              <div v-if="imagePreview.length" class="preview-grid">
                <div
                  v-for="(preview, index) in imagePreview"
                  :key="preview.id"
                  class="preview-card stagger-item hover-lift"
                  :style="{ animationDelay: `${index * 0.05}s` }"
                  draggable="true"
                  @dragstart="onDragStart(index)"
                  @dragover="onDragOver"
                  @drop="onDrop(index)"
                  @dragenter="onDragEnter(index)"
                  @dragleave="onDragLeave"
                  @dragend="onDragEnd"
                  :class="{ dragging: draggedIndex === index, 'drag-over': dragOverIndex === index }"
                >
                  <div class="preview-index">{{ index + 1 }}</div>
                  <img :src="preview.url" :alt="`预览 ${index + 1}`" @dblclick="openImageEditor(index)" />
                  <div class="preview-meta">
                    {{ preview.width }} × {{ preview.height }}
                    <span :class="preview.isPortrait ? 'ok' : 'warn'">
                      {{ preview.isPortrait ? '竖版' : '横版' }}
                    </span>
                  </div>
                  <button type="button" class="remove-btn" @click="removeImage(index)">删除</button>
                </div>
              </div>
              <button type="submit" class="fs-btn fs-btn-primary btn-ripple shine-effect" :disabled="loadingImage">
                <span v-if="loadingImage" class="loading-spinner"></span>
                {{ loadingImage ? 'OCR识别中，请稍候...' : '通过图片创建草稿' }}
              </button>
            </form>
          </div>
        </div>

        <div class="fs-card">
          <div class="fs-card-header">
            <div class="fs-card-title">文档上传创建</div>
          </div>
          <div class="fs-card-body">
            <form class="fs-form" @submit.prevent="onSubmitDocument">
              <div class="fs-form-item">
                <label class="fs-form-label">文档标题</label>
                <input v-model="docTitle" class="fs-input" placeholder="请输入作文标题" required />
              </div>
              <div class="fs-form-item">
                <label class="fs-form-label">上传作文文档</label>
                <input
                  id="essay-doc-file"
                  type="file"
                  accept=".doc,.docx,.pdf,.txt"
                  @change="onDocChange"
                  class="fs-file-input"
                />
                <div class="upload-card upload-card--compact">
                  <label for="essay-doc-file" class="upload-card__header" title="选择作文文档">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M7 10V9C7 6.23858 9.23858 4 12 4C14.7614 4 17 6.23858 17 9V10C19.2091 10 21 11.7909 21 14C21 15.4806 20.1956 16.8084 19 17.5M7 10C4.79086 10 3 11.7909 3 14C3 15.4806 3.8044 16.8084 5 17.5M7 10C7.43285 10 7.84965 10.0688 8.24006 10.1959M12 12V21M12 12L15 15M12 12L9 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path>
                    </svg>
                    <p>Browse File to upload!</p>
                  </label>
                  <div class="upload-card__footer">
                    <label for="essay-doc-file" class="upload-card__footer-main">
                      <svg viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
                        <path d="M15.331 6H8.5v20h15V14.154h-8.169z"></path>
                        <path d="M18.153 6h-.009v5.342H23.5v-.002z"></path>
                      </svg>
                      <p>{{ docFile?.name || 'Not selected file' }}</p>
                    </label>
                    <button v-if="docFile" type="button" class="upload-card__clear" @click="clearDocument" title="清空已选文档">
                      <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M5.16565 10.1534C5.07629 8.99181 5.99473 8 7.15975 8H16.8402C18.0053 8 18.9237 8.9918 18.8344 10.1534L18.142 19.1534C18.0619 20.1954 17.193 21 16.1479 21H7.85206C6.80699 21 5.93811 20.1954 5.85795 19.1534L5.16565 10.1534Z" stroke="currentColor" stroke-width="2"></path>
                        <path d="M19.5 5H4.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"></path>
                        <path d="M10 3C10 2.44772 10.4477 2 11 2H13C13.5523 2 14 2.44772 14 3V5H10V3Z" stroke="currentColor" stroke-width="2"></path>
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
              <button type="submit" class="fs-btn fs-btn-outline btn-ripple" :disabled="loadingDoc">
                <span v-if="loadingDoc" class="loading-spinner"></span>
                {{ loadingDoc ? '上传中...' : '通过文档创建草稿' }}
              </button>
            </form>
          </div>
        </div>

        <div v-if="message" class="fs-note success-note">
          <p>{{ message }}</p>
        </div>
        <div v-if="error" class="fs-form-error">{{ error }}</div>
      </div>
    </div>

    <div v-if="showCropModal" class="crop-modal-overlay" @click.self="closeCropModal">
      <div class="crop-modal">
        <div class="crop-modal-header">
          <h3>编辑图片，第 {{ cropImageIndex + 1 }} 张</h3>
          <button type="button" @click="closeCropModal" class="crop-close-btn">✕</button>
        </div>
        <div class="crop-modal-body" v-progressive-blur-scroll>
          <div class="crop-controls">
            <template v-if="!showOriginalImage">
              <button type="button" @click="rotateCropImage(-90)" class="fs-btn fs-btn-outline">逆时针90°</button>
              <button type="button" @click="rotateCropImage(90)" class="fs-btn fs-btn-outline">顺时针90°</button>
              <button type="button" @click="resetCropSelection" class="fs-btn fs-btn-outline">重置选区</button>
              <button type="button" @click="toggleOriginalView" class="fs-btn fs-btn-outline">查看原图</button>
            </template>
            <template v-else>
              <button type="button" @click="toggleOriginalView" class="fs-btn fs-btn-outline">返回编辑</button>
              <button type="button" @click="imageZoomScale = 1" class="fs-btn fs-btn-outline">重置缩放</button>
            </template>
          </div>

          <div
            v-show="showOriginalImage"
            @wheel.prevent="onImageZoom"
            class="origin-preview"
            v-progressive-blur-scroll
          >
            <img
              :src="cropImageSrc"
              alt="原图"
              :style="{ width: imageZoomScale * 100 + '%' }"
            />
          </div>

          <div v-show="!showOriginalImage" style="text-align: center">
            <canvas
              id="cropCanvas"
              @mousedown="onCanvasMouseDown"
              @mousemove="onCanvasMouseMove"
              @mouseup="onCanvasMouseUp"
              @mouseleave="onCanvasMouseUp"
              @dblclick="toggleOriginalView"
              class="crop-canvas"
            ></canvas>
          </div>
        </div>
        <div class="crop-modal-footer">
          <button type="button" @click="closeCropModal" class="fs-btn fs-btn-outline">取消</button>
          <button type="button" @click="applyCrop" class="fs-btn fs-btn-primary">确认裁切</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()

const title = ref('')
const content = ref('')
const loadingText = ref(false)
const loadingImage = ref(false)
const loadingDoc = ref(false)
const message = ref('')
const error = ref('')

const imageTitle = ref('')
const docTitle = ref('')
const imageFiles = ref([])
const imagePreview = ref([])
const docFile = ref(null)
const draggedIndex = ref(null)
const dragOverIndex = ref(null)
let nextId = 0

const onSubmitText = async () => {
  if (!title.value || !content.value) return
  loadingText.value = true
  message.value = ''
  error.value = ''
  try {
    const res = await http.post('/essay/text', {
      title: title.value,
      content: content.value,
    })
    const essayId = res.data
    message.value = `保存成功，作文编号为 ${essayId}，正在返回作文列表。`
    setTimeout(() => router.push('/essays'), 800)
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    loadingText.value = false
  }
}

const onImageChange = (e) => {
  const files = e.target.files
  const newFiles = files && files.length ? Array.from(files) : []
  imageFiles.value = [...imageFiles.value, ...newFiles]

  if (newFiles.length) {
    newFiles.forEach((file) => {
      const reader = new FileReader()
      reader.onload = (event) => {
        const img = new Image()
        img.onload = () => {
          imagePreview.value.push({
            id: nextId++,
            url: event.target.result,
            width: img.width,
            height: img.height,
            isPortrait: img.height > img.width,
            file,
          })
        }
        img.src = event.target.result
      }
      reader.readAsDataURL(file)
    })
  }
}

const onDragStart = (index) => {
  draggedIndex.value = index
  event.dataTransfer.effectAllowed = 'move'
}

const onDragEnter = (index) => {
  if (draggedIndex.value !== null && draggedIndex.value !== index) {
    dragOverIndex.value = index
  }
}

const onDragLeave = (e) => {
  if (e.currentTarget === e.target) {
    dragOverIndex.value = null
  }
}

const onDragOver = (e) => {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}

const onDrop = (dropIndex) => {
  event.preventDefault()

  if (draggedIndex.value === null || draggedIndex.value === dropIndex) {
    draggedIndex.value = null
    dragOverIndex.value = null
    return
  }

  const newPreview = [...imagePreview.value]
  const draggedItem = newPreview[draggedIndex.value]
  newPreview.splice(draggedIndex.value, 1)
  newPreview.splice(dropIndex, 0, draggedItem)
  imagePreview.value = newPreview

  const newFiles = [...imageFiles.value]
  const draggedFile = newFiles[draggedIndex.value]
  newFiles.splice(draggedIndex.value, 1)
  newFiles.splice(dropIndex, 0, draggedFile)
  imageFiles.value = newFiles

  draggedIndex.value = null
  dragOverIndex.value = null
}

const onDragEnd = () => {
  draggedIndex.value = null
  dragOverIndex.value = null
}

const removeImage = (index) => {
  imagePreview.value.splice(index, 1)
  imageFiles.value.splice(index, 1)
}

const clearImages = () => {
  imagePreview.value = []
  imageFiles.value = []
  const input = document.getElementById('essay-image-files')
  if (input) input.value = ''
}

const showCropModal = ref(false)
const cropImageIndex = ref(null)
const cropImageSrc = ref('')
const cropCanvas = ref(null)
const cropCtx = ref(null)
const cropImageElement = ref(null)
const cropRotation = ref(0)
const cropSelection = ref({ x: 0, y: 0, width: 0, height: 0 })
const isDraggingCrop = ref(false)
const isResizing = ref(false)
const resizeHandle = ref('')
const dragStartPos = ref({ x: 0, y: 0 })
const selectionStart = ref({ x: 0, y: 0, width: 0, height: 0 })
const showOriginalImage = ref(false)
const imageZoomScale = ref(1)

const openImageEditor = (index) => {
  cropImageIndex.value = index
  cropImageSrc.value = imagePreview.value[index].url
  cropRotation.value = 0
  showCropModal.value = true
  showOriginalImage.value = false
  imageZoomScale.value = 1
  setTimeout(() => initCropCanvas(), 100)
}

const toggleOriginalView = () => {
  showOriginalImage.value = !showOriginalImage.value
  if (showOriginalImage.value) {
    imageZoomScale.value = 1
  } else {
    setTimeout(() => {
      if (cropCanvas.value && cropImageElement.value) drawCropCanvas()
    }, 50)
  }
}

const onImageZoom = (e) => {
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  let newScale = imageZoomScale.value + delta
  newScale = Math.max(0.5, Math.min(5, newScale))
  imageZoomScale.value = newScale
}

const initCropCanvas = () => {
  const canvas = document.getElementById('cropCanvas')
  if (!canvas) return

  cropCanvas.value = canvas
  cropCtx.value = canvas.getContext('2d')

  const img = new Image()
  img.onload = () => {
    cropImageElement.value = img
    const maxWidth = Math.min(window.innerWidth * 0.8, 1200)
    const maxHeight = Math.min(window.innerHeight * 0.6, 800)
    let width = img.width
    let height = img.height

    if (width > maxWidth) {
      height = (height * maxWidth) / width
      width = maxWidth
    }
    if (height > maxHeight) {
      width = (width * maxHeight) / height
      height = maxHeight
    }

    canvas.width = width
    canvas.height = height
    cropSelection.value = { x: 0, y: 0, width, height }
    drawCropCanvas()
  }
  img.src = cropImageSrc.value
}

const drawCropCanvas = () => {
  if (!cropCanvas.value || !cropImageElement.value || !cropCtx.value) return
  const canvas = cropCanvas.value
  const ctx = cropCtx.value
  const img = cropImageElement.value
  const sel = cropSelection.value

  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.save()

  if (cropRotation.value !== 0) {
    ctx.translate(canvas.width / 2, canvas.height / 2)
    ctx.rotate((cropRotation.value * Math.PI) / 180)
    ctx.translate(-canvas.width / 2, -canvas.height / 2)
  }

  ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
  ctx.restore()

  ctx.fillStyle = 'rgba(0, 0, 0, 0.45)'
  ctx.fillRect(0, 0, canvas.width, sel.y)
  ctx.fillRect(0, sel.y + sel.height, canvas.width, canvas.height - sel.y - sel.height)
  ctx.fillRect(0, sel.y, sel.x, sel.height)
  ctx.fillRect(sel.x + sel.width, sel.y, canvas.width - sel.x - sel.width, sel.height)

  ctx.strokeStyle = '#224d69'
  ctx.lineWidth = 2
  ctx.strokeRect(sel.x, sel.y, sel.width, sel.height)

  const handleSize = 10
  ctx.fillStyle = '#224d69'
  ctx.fillRect(sel.x - handleSize / 2, sel.y - handleSize / 2, handleSize, handleSize)
  ctx.fillRect(sel.x + sel.width - handleSize / 2, sel.y - handleSize / 2, handleSize, handleSize)
  ctx.fillRect(sel.x - handleSize / 2, sel.y + sel.height - handleSize / 2, handleSize, handleSize)
  ctx.fillRect(sel.x + sel.width - handleSize / 2, sel.y + sel.height - handleSize / 2, handleSize, handleSize)
  ctx.fillRect(sel.x + sel.width / 2 - handleSize / 2, sel.y - handleSize / 2, handleSize, handleSize)
  ctx.fillRect(sel.x + sel.width / 2 - handleSize / 2, sel.y + sel.height - handleSize / 2, handleSize, handleSize)
  ctx.fillRect(sel.x - handleSize / 2, sel.y + sel.height / 2 - handleSize / 2, handleSize, handleSize)
  ctx.fillRect(sel.x + sel.width - handleSize / 2, sel.y + sel.height / 2 - handleSize / 2, handleSize, handleSize)
}

const onCanvasMouseDown = (e) => {
  const canvas = cropCanvas.value
  if (!canvas) return

  const rect = canvas.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const sel = cropSelection.value
  const handleSize = 10

  const handles = {
    nw: { x: sel.x, y: sel.y },
    ne: { x: sel.x + sel.width, y: sel.y },
    sw: { x: sel.x, y: sel.y + sel.height },
    se: { x: sel.x + sel.width, y: sel.y + sel.height },
    n: { x: sel.x + sel.width / 2, y: sel.y },
    s: { x: sel.x + sel.width / 2, y: sel.y + sel.height },
    w: { x: sel.x, y: sel.y + sel.height / 2 },
    e: { x: sel.x + sel.width, y: sel.y + sel.height / 2 },
  }

  for (const [handle, pos] of Object.entries(handles)) {
    if (Math.abs(x - pos.x) <= handleSize && Math.abs(y - pos.y) <= handleSize) {
      isResizing.value = true
      resizeHandle.value = handle
      dragStartPos.value = { x, y }
      selectionStart.value = { ...sel }
      return
    }
  }

  if (x >= sel.x && x <= sel.x + sel.width && y >= sel.y && y <= sel.y + sel.height) {
    isDraggingCrop.value = true
    dragStartPos.value = { x, y }
    selectionStart.value = { ...sel }
  }
}

const onCanvasMouseMove = (e) => {
  const canvas = cropCanvas.value
  if (!canvas) return

  const rect = canvas.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top

  if (isResizing.value) {
    const dx = x - dragStartPos.value.x
    const dy = y - dragStartPos.value.y
    const start = selectionStart.value
    let newSel = { ...start }
    const handle = resizeHandle.value

    if (handle.includes('n')) {
      newSel.y = Math.max(0, Math.min(start.y + dy, start.y + start.height - 20))
      newSel.height = start.height - (newSel.y - start.y)
    }
    if (handle.includes('s')) {
      newSel.height = Math.max(20, Math.min(start.height + dy, canvas.height - start.y))
    }
    if (handle.includes('w')) {
      newSel.x = Math.max(0, Math.min(start.x + dx, start.x + start.width - 20))
      newSel.width = start.width - (newSel.x - start.x)
    }
    if (handle.includes('e')) {
      newSel.width = Math.max(20, Math.min(start.width + dx, canvas.width - start.x))
    }

    cropSelection.value = newSel
    drawCropCanvas()
  } else if (isDraggingCrop.value) {
    const dx = x - dragStartPos.value.x
    const dy = y - dragStartPos.value.y
    const start = selectionStart.value
    let newX = start.x + dx
    let newY = start.y + dy
    newX = Math.max(0, Math.min(newX, canvas.width - start.width))
    newY = Math.max(0, Math.min(newY, canvas.height - start.height))
    cropSelection.value = { ...start, x: newX, y: newY }
    drawCropCanvas()
  }
}

const onCanvasMouseUp = () => {
  isDraggingCrop.value = false
  isResizing.value = false
  resizeHandle.value = ''
}

const rotateCropImage = (degrees) => {
  cropRotation.value = (cropRotation.value + degrees) % 360
  if (!cropImageElement.value || !cropCanvas.value) return
  const img = cropImageElement.value
  const canvas = cropCanvas.value
  const isVerticalRotation = Math.abs(cropRotation.value) === 90 || Math.abs(cropRotation.value) === 270
  const maxWidth = Math.min(window.innerWidth * 0.8, 1200)
  const maxHeight = Math.min(window.innerHeight * 0.6, 800)
  let width = isVerticalRotation ? img.height : img.width
  let height = isVerticalRotation ? img.width : img.height

  if (width > maxWidth) {
    height = (height * maxWidth) / width
    width = maxWidth
  }
  if (height > maxHeight) {
    width = (width * maxHeight) / height
    height = maxHeight
  }

  canvas.width = width
  canvas.height = height
  cropSelection.value = { x: 0, y: 0, width, height }
  drawCropCanvas()
}

const resetCropSelection = () => {
  if (!cropCanvas.value) return
  cropSelection.value = {
    x: 0,
    y: 0,
    width: cropCanvas.value.width,
    height: cropCanvas.value.height,
  }
  drawCropCanvas()
}

const applyCrop = async () => {
  if (!cropCanvas.value || !cropImageElement.value || cropImageIndex.value === null) return

  const img = cropImageElement.value
  const sel = cropSelection.value
  const canvas = cropCanvas.value

  if (sel.width <= 0 || sel.height <= 0) {
    alert('选区无效，请重新选择')
    return
  }

  const tempCanvas = document.createElement('canvas')
  const tempCtx = tempCanvas.getContext('2d')
  const isVerticalRotation = Math.abs(cropRotation.value) === 90 || Math.abs(cropRotation.value) === 270
  tempCanvas.width = isVerticalRotation ? img.height : img.width
  tempCanvas.height = isVerticalRotation ? img.width : img.height

  tempCtx.save()
  tempCtx.translate(tempCanvas.width / 2, tempCanvas.height / 2)
  tempCtx.rotate((cropRotation.value * Math.PI) / 180)
  tempCtx.drawImage(img, -img.width / 2, -img.height / 2)
  tempCtx.restore()

  const resultCanvas = document.createElement('canvas')
  const resultCtx = resultCanvas.getContext('2d')
  const scaleX = tempCanvas.width / canvas.width
  const scaleY = tempCanvas.height / canvas.height
  const cropX = sel.x * scaleX
  const cropY = sel.y * scaleY
  const cropWidth = sel.width * scaleX
  const cropHeight = sel.height * scaleY

  resultCanvas.width = cropWidth
  resultCanvas.height = cropHeight
  resultCtx.drawImage(tempCanvas, cropX, cropY, cropWidth, cropHeight, 0, 0, cropWidth, cropHeight)

  resultCanvas.toBlob(
    (blob) => {
      if (!blob) {
        alert('裁剪失败，请重试')
        return
      }

      const index = cropImageIndex.value
      const originalFile = imageFiles.value[index]
      const fileName = originalFile.name
      const newFile = new File([blob], fileName, { type: 'image/jpeg' })

      const newFiles = [...imageFiles.value]
      newFiles[index] = newFile
      imageFiles.value = newFiles

      const reader = new FileReader()
      reader.onload = (e) => {
        const newImg = new Image()
        newImg.onload = () => {
          const newPreviews = [...imagePreview.value]
          newPreviews[index] = {
            id: nextId++,
            url: e.target.result,
            width: newImg.width,
            height: newImg.height,
            isPortrait: newImg.height > newImg.width,
            file: newFile,
          }
          imagePreview.value = newPreviews
        }
        newImg.src = e.target.result
      }
      reader.readAsDataURL(newFile)
      closeCropModal()
    },
    'image/jpeg',
    0.95
  )
}

const closeCropModal = () => {
  showCropModal.value = false
  cropImageIndex.value = null
  cropImageSrc.value = ''
  cropRotation.value = 0
  showOriginalImage.value = false
  imageZoomScale.value = 1
}

const onDocChange = (e) => {
  const files = e.target.files
  docFile.value = files && files.length ? files[0] : null
}

const clearDocument = () => {
  docFile.value = null
  const input = document.getElementById('essay-doc-file')
  if (input) input.value = ''
}

const onSubmitImage = async () => {
  if (!imageTitle.value || !imageFiles.value.length) return
  const hasLandscape = imagePreview.value.some((p) => !p.isPortrait)
  if (hasLandscape) {
    if (!window.confirm('检测到横版图片，可能影响 OCR 识别结果，是否继续上传？')) return
  }

  loadingImage.value = true
  message.value = ''
  error.value = ''
  try {
    const formData = new FormData()
    formData.append('title', imageTitle.value)
    imageFiles.value.forEach((file) => formData.append('file', file))
    const res = await http.post('/essay/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const essayId = res.data
    message.value = `图片识别成功，已创建草稿，作文编号为 ${essayId}。`
    setTimeout(() => router.push('/essays'), 1200)
  } catch (e) {
    error.value = e.message || '图片上传创建失败'
  } finally {
    loadingImage.value = false
  }
}

const onSubmitDocument = async () => {
  if (!docTitle.value || !docFile.value) return
  loadingDoc.value = true
  message.value = ''
  error.value = ''
  try {
    const formData = new FormData()
    formData.append('title', docTitle.value)
    formData.append('file', docFile.value)
    const res = await http.post('/essay/document', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const essayId = res.data
    message.value = `文档上传成功，已创建草稿，作文编号为 ${essayId}。`
    setTimeout(() => router.push('/essays'), 800)
  } catch (e) {
    error.value = e.message || '文档上传创建失败'
  } finally {
    loadingDoc.value = false
  }
}
</script>

<style scoped>
.fs-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.upload-card {
  width: 100%;
  border-radius: 22px;
  box-shadow: 4px 4px 30px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: space-between;
  padding: 12px;
  gap: 10px;
  background:
    linear-gradient(180deg, rgba(226, 234, 245, 0.9), rgba(241, 246, 252, 0.96)),
    rgba(0, 110, 255, 0.041);
  border: 1px solid rgba(34, 77, 105, 0.08);
}

.upload-card__header {
  min-height: 248px;
  width: 100%;
  border: 2px dashed #3867ff;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 22px;
  cursor: pointer;
  color: #111827;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.upload-card--compact .upload-card__header {
  min-height: 192px;
}

.upload-card__header:hover {
  transform: translateY(-1px);
  box-shadow: inset 0 0 0 1px rgba(56, 103, 255, 0.12);
}

.upload-card__header svg {
  width: 96px;
  height: 96px;
}

.upload-card__header p {
  margin: 16px 0 0;
  text-align: center;
  color: #111827;
  font-size: 17px;
}

.upload-card__footer {
  background-color: rgba(0, 110, 255, 0.075);
  width: 100%;
  min-height: 54px;
  padding: 8px 10px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload-card__footer-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.upload-card__footer-main > svg,
.upload-card__clear svg {
  width: 30px;
  height: 30px;
  fill: royalblue;
  color: #111827;
  background-color: rgba(70, 66, 66, 0.103);
  border-radius: 50%;
  padding: 3px;
  box-shadow: 0 2px 30px rgba(0, 0, 0, 0.12);
}

.upload-card__footer-main p {
  flex: 1;
  margin: 0;
  text-align: center;
  color: #111827;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.upload-card__clear {
  border: none;
  background: transparent;
  width: 36px;
  height: 36px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.create-textarea {
  min-height: 360px;
}

.word-count {
  color: var(--muted);
  font-size: 13px;
}

.upload-hint {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.preview-card {
  position: relative;
  border: 1px solid rgba(56, 44, 31, 0.08);
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.78);
  cursor: move;
  transition: 0.2s ease;
}

.preview-card img {
  width: 100%;
  height: 210px;
  object-fit: contain;
  background: #f9fafb;
}

.preview-index {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(34, 77, 105, 0.86);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.preview-meta {
  padding: 10px 12px;
  font-size: 12px;
  color: var(--muted);
}

.preview-meta .ok {
  color: var(--green);
  margin-left: 6px;
}

.preview-meta .warn {
  color: var(--danger);
  margin-left: 6px;
}

.remove-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  border: 0;
  border-radius: 999px;
  padding: 6px 10px;
  background: rgba(139, 75, 75, 0.92);
  color: #fff;
  cursor: pointer;
}

.dragging {
  opacity: 0.5;
  transform: scale(0.95);
}

.drag-over {
  border: 2px dashed #224d69 !important;
  background: rgba(34, 77, 105, 0.05);
}

.success-note {
  color: var(--green);
}

.crop-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.62);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.crop-modal {
  width: min(1100px, 92vw);
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  border-radius: 28px;
  background: #fffaf3;
  overflow: hidden;
}

.crop-modal-header,
.crop-modal-footer {
  padding: 18px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid rgba(56, 44, 31, 0.08);
}

.crop-modal-footer {
  justify-content: flex-end;
  border-bottom: 0;
  border-top: 1px solid rgba(56, 44, 31, 0.08);
}

.crop-modal-header h3 {
  margin: 0;
  font-size: 20px;
}

.crop-close-btn {
  border: 0;
  background: transparent;
  font-size: 22px;
  cursor: pointer;
}

.crop-modal-body {
  padding: 20px;
  overflow: auto;
}

.crop-controls {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.origin-preview {
  text-align: center;
  overflow: auto;
  max-height: 70vh;
  background: #f9fafb;
  border: 1px solid rgba(56, 44, 31, 0.08);
  border-radius: 16px;
  padding: 16px;
}

.crop-canvas {
  cursor: move;
  border: 1px solid rgba(56, 44, 31, 0.08);
  display: inline-block;
  max-width: 100%;
  border-radius: 16px;
}

@media (max-width: 860px) {
  .upload-card__header {
    min-height: 220px;
  }

  .upload-card--compact .upload-card__header {
    min-height: 180px;
  }
}

</style>


.fs-btn {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fs-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.fs-btn:active:not(:disabled) {
  transform: translateY(0);
}

.loading-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 6px;
  vertical-align: middle;
}

.fs-textarea {
  transition: all 0.3s ease;
}

.fs-textarea:focus {
  transform: scale(1.005);
  box-shadow: 0 4px 12px rgba(34, 77, 105, 0.15);
}

.fs-card {
  transition: all 0.3s ease;
}

.fs-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}
