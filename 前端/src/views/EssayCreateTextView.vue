<template>
  <div class="rp-card">
    <div class="rp-card-header">
      <div class="rp-card-title">新建作文</div>
      <button class="rp-btn rp-btn-outline" @click="$router.back()">返回</button>
    </div>

    <!-- 文本创建 -->
    <form class="rp-form" @submit.prevent="onSubmitText">
      <div class="rp-form-row">
        <label class="rp-form-label">标题</label>
        <input v-model="title" class="rp-input" placeholder="请输入作文标题" required />
      </div>

      <div class="rp-form-row">
        <label class="rp-form-label">作文内容</label>
        <textarea
          v-model="content"
          class="rp-textarea"
          placeholder="在这里粘贴或输入完整作文内容"
          required
        />
      </div>

      <div class="rp-form-row">
        <span class="rp-form-label">字数：{{ content.length }}</span>
      </div>

      <button type="submit" class="rp-btn rp-btn-primary" :disabled="loadingText">
        {{ loadingText ? '提交中...' : '保存文本草稿' }}
      </button>

      <div v-if="message" style="margin-top: 8px; font-size: 13px; color: #16a34a">
        {{ message }}
      </div>

      <div v-if="error" class="rp-error-text" style="margin-top: 4px">
        {{ error }}
      </div>
    </form>

    <!-- 图片上传创建 -->
    <form
      class="rp-form"
      style="margin-top: 24px; border-top: 1px solid #e5e7eb; padding-top: 16px"
      @submit.prevent="onSubmitImage"
    >
      <div class="rp-form-row">
        <label class="rp-form-label">图片标题</label>
        <input v-model="imageTitle" class="rp-input" placeholder="请输入作文标题" required />
      </div>
      <div class="rp-form-row">
        <label class="rp-form-label">上传作文图片</label>
        <input
          type="file"
          accept=".jpg,.jpeg,.png,.gif,.bmp"
          multiple
          @change="onImageChange"
          class="rp-input"
          ref="imageInput"
          required
        />
        <div style="font-size: 12px; color: #6b7280; margin-top: 4px">
          ⚠️ 请确保上传的图片方向正确（竖版拍摄），横向或倒置的图片可能导致OCR识别失败
        </div>
      </div>
      <div v-if="imagePreview.length" style="margin-top: 12px">
        <div style="font-size: 13px; font-weight: 600; margin-bottom: 8px">
          图片预览（双击图片可编辑，可拖拽调整顺序）：
        </div>
        <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px">
          <div
            v-for="(preview, index) in imagePreview"
            :key="preview.id"
            draggable="true"
            @dragstart="onDragStart(index)"
            @dragover="onDragOver"
            @drop="onDrop(index)"
            @dragenter="onDragEnter(index)"
            @dragleave="onDragLeave"
            @dragend="onDragEnd"
            :class="{ 'dragging': draggedIndex === index, 'drag-over': dragOverIndex === index }"
            style="
              position: relative;
              border: 1px solid #e5e7eb;
              border-radius: 8px;
              overflow: hidden;
              cursor: move;
              transition: all 0.2s;
              user-select: none;
            "
          >
            <div
              style="
                position: absolute;
                top: 4px;
                left: 4px;
                background: rgba(0, 0, 0, 0.6);
                color: white;
                padding: 2px 8px;
                border-radius: 4px;
                font-size: 12px;
                font-weight: 600;
                z-index: 1;
              "
            >
              {{ index + 1 }}
            </div>
            <img
              :src="preview.url"
              :key="preview.id"
              :alt="`预览 ${index + 1}`"
              @dblclick="openImageEditor(index)"
              style="width: 100%; height: 200px; object-fit: contain; background: #f9fafb; cursor: pointer"
            />
            <div
              style="
                position: absolute;
                bottom: 0;
                left: 0;
                right: 0;
                background: rgba(0, 0, 0, 0.6);
                color: white;
                padding: 4px 8px;
                font-size: 11px;
                text-align: center;
              "
            >
              {{ preview.width }} × {{ preview.height }}
              <span v-if="preview.isPortrait" style="color: #86efac">✓ 竖版</span>
              <span v-else style="color: #fca5a5">⚠ 横版</span>
            </div>
            <div style="position: absolute; top: 4px; right: 4px; display: flex; gap: 4px; z-index: 1">
              <button
                type="button"
                @click="removeImage(index)"
                style="
                  background: rgba(239, 68, 68, 0.9);
                  color: white;
                  border: none;
                  border-radius: 4px;
                  padding: 4px 8px;
                  font-size: 12px;
                  cursor: pointer;
                "
              >
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
      <button type="submit" class="rp-btn rp-btn-primary" :disabled="loadingImage">
        {{ loadingImage ? 'OCR识别中，请稍候...' : '通过图片创建草稿（OCR识别）' }}
      </button>
    </form>

    <!-- 文档上传创建 -->
    <form
      class="rp-form"
      style="margin-top: 24px; border-top: 1px solid #e5e7eb; padding-top: 16px"
      @submit.prevent="onSubmitDocument"
    >
      <div class="rp-form-row">
        <label class="rp-form-label">文档标题</label>
        <input v-model="docTitle" class="rp-input" placeholder="请输入作文标题" required />
      </div>
      <div class="rp-form-row">
        <label class="rp-form-label">上传作文文档</label>
        <input
          type="file"
          accept=".doc,.docx,.pdf,.txt"
          @change="onDocChange"
          class="rp-input"
          required
        />
      </div>
      <button type="submit" class="rp-btn rp-btn-primary" :disabled="loadingDoc">
        {{ loadingDoc ? '上传中...' : '通过文档创建草稿' }}
      </button>
    </form>

    <!-- 图片编辑器模态框 -->
    <div v-if="showCropModal" class="crop-modal-overlay" @click.self="closeCropModal">
      <div class="crop-modal" style="max-width: 90vw; max-height: 95vh">
        <div class="crop-modal-header">
          <h3>编辑图片 - 第 {{ cropImageIndex + 1 }} 张</h3>
          <button type="button" @click="closeCropModal" class="crop-close-btn">✕</button>
        </div>
        <div class="crop-modal-body" style="flex: 1; overflow: auto">
          <div style="font-size: 13px; color: #6b7280; margin-bottom: 12px; text-align: center">
            💡 双击图片可查看原图 | 拖动选区移动位置 | 拖动边角调整大小
          </div>
          
          <!-- 工具栏 -->
          <div class="crop-controls" style="margin-bottom: 16px">
            <template v-if="!showOriginalImage">
              <button type="button" @click="rotateCropImage(-90)" class="rp-btn rp-btn-outline">↶ 逆时针90°</button>
              <button type="button" @click="rotateCropImage(90)" class="rp-btn rp-btn-outline">↷ 顺时针90°</button>
              <button type="button" @click="resetCropSelection" class="rp-btn rp-btn-outline">重置选区</button>
              <button type="button" @click="toggleOriginalView" class="rp-btn rp-btn-outline">查看原图</button>
            </template>
            <template v-else>
              <button type="button" @click="toggleOriginalView" class="rp-btn rp-btn-outline">返回编辑</button>
              <button type="button" @click="imageZoomScale = 1" class="rp-btn rp-btn-outline">重置缩放</button>
              <span style="color: #6b7280; font-size: 13px; margin-left: 12px">
                💡 滚动鼠标滚轮缩放 | 当前: {{ Math.round(imageZoomScale * 100) }}%
              </span>
            </template>
          </div>
          
          <!-- 原图查看模式 -->
          <div 
            v-show="showOriginalImage" 
            @wheel.prevent="onImageZoom"
            style="text-align: center; overflow: auto; max-height: 70vh; position: relative; background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 4px; padding: 16px"
          >
            <img 
              :src="cropImageSrc" 
              alt="原图" 
              :style="{
                width: imageZoomScale * 100 + '%',
                transition: 'width 0.1s ease',
                cursor: imageZoomScale < 5 ? 'zoom-in' : 'zoom-out'
              }"
            />
          </div>
          
          <!-- 裁剪编辑模式 -->
          <div v-show="!showOriginalImage" style="text-align: center">
            <canvas
              id="cropCanvas"
              @mousedown="onCanvasMouseDown"
              @mousemove="onCanvasMouseMove"
              @mouseup="onCanvasMouseUp"
              @mouseleave="onCanvasMouseUp"
              @dblclick="toggleOriginalView"
              style="cursor: move; border: 1px solid #e5e7eb; display: inline-block; max-width: 100%"
            ></canvas>
          </div>
        </div>
        <div class="crop-modal-footer">
          <button type="button" @click="closeCropModal" class="rp-btn rp-btn-outline">取消</button>
          <button type="button" @click="applyCrop" class="rp-btn rp-btn-primary">确认裁切</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import http from '../api/http';

const router = useRouter();

const title = ref('');
const content = ref('');
const loadingText = ref(false);
const loadingImage = ref(false);
const loadingDoc = ref(false);
const message = ref('');
const error = ref('');

const imageTitle = ref('');
const docTitle = ref('');
const imageFiles = ref([]);
const imagePreview = ref([]);
const docFile = ref(null);
const draggedIndex = ref(null);
const dragOverIndex = ref(null);
let nextId = 0;

const onSubmitText = async () => {
  if (!title.value || !content.value) return;
  loadingText.value = true;
  message.value = '';
  error.value = '';
  try {
    // POST /essay/text  ==> Result<Long> essayId
    const res = await http.post('/essay/text', {
      title: title.value,
      content: content.value,
    });
    const essayId = res.data;
    message.value = `保存成功，作文ID：${essayId}`;
    setTimeout(() => {
      router.push('/essays');
    }, 800);
  } catch (e) {
    error.value = e.message || '保存失败';
  } finally {
    loadingText.value = false;
  }
};

const onImageChange = (e) => {
  const files = e.target.files;
  const newFiles = files && files.length ? Array.from(files) : [];
  
  // 追加新文件到现有文件列表
  imageFiles.value = [...imageFiles.value, ...newFiles];

  // 生成预览并检测图片方向
  if (newFiles.length) {
    newFiles.forEach((file) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.onload = () => {
          imagePreview.value.push({
            id: nextId++, // 唯一ID用于拖拽
            url: event.target.result,
            width: img.width,
            height: img.height,
            isPortrait: img.height > img.width, // 竖版：高度大于宽度
            file: file,
          });
        };
        img.src = event.target.result;
      };
      reader.readAsDataURL(file);
    });
  }
};

const onDragStart = (index) => {
  draggedIndex.value = index;
  event.dataTransfer.effectAllowed = 'move';
};

const onDragEnter = (index) => {
  if (draggedIndex.value !== null && draggedIndex.value !== index) {
    dragOverIndex.value = index;
  }
};

const onDragLeave = (e) => {
  // 只在真正离开元素时清除
  if (e.currentTarget === e.target) {
    dragOverIndex.value = null;
  }
};

const onDragOver = (e) => {
  e.preventDefault();
  e.dataTransfer.dropEffect = 'move';
};

const onDrop = (dropIndex) => {
  event.preventDefault();
  
  if (draggedIndex.value === null || draggedIndex.value === dropIndex) {
    draggedIndex.value = null;
    dragOverIndex.value = null;
    return;
  }

  // 交换预览数组中的位置
  const newPreview = [...imagePreview.value];
  const draggedItem = newPreview[draggedIndex.value];
  newPreview.splice(draggedIndex.value, 1);
  newPreview.splice(dropIndex, 0, draggedItem);
  imagePreview.value = newPreview;

  // 同步更新文件数组
  const newFiles = [...imageFiles.value];
  const draggedFile = newFiles[draggedIndex.value];
  newFiles.splice(draggedIndex.value, 1);
  newFiles.splice(dropIndex, 0, draggedFile);
  imageFiles.value = newFiles;

  draggedIndex.value = null;
  dragOverIndex.value = null;
};

const onDragEnd = () => {
  draggedIndex.value = null;
  dragOverIndex.value = null;
};

const removeImage = (index) => {
  imagePreview.value.splice(index, 1);
  imageFiles.value.splice(index, 1);
};

// 裁切相关状态
const showCropModal = ref(false);
const cropImageIndex = ref(null);
const cropImageSrc = ref('');
const cropCanvas = ref(null);
const cropCtx = ref(null);
const cropImageElement = ref(null);
const cropRotation = ref(0);
const cropSelection = ref({ x: 0, y: 0, width: 0, height: 0 });
const isDraggingCrop = ref(false);
const isResizing = ref(false);
const resizeHandle = ref('');
const dragStartPos = ref({ x: 0, y: 0 });
const selectionStart = ref({ x: 0, y: 0, width: 0, height: 0 });
const showOriginalImage = ref(false);
const imageZoomScale = ref(1);

const openImageEditor = (index) => {
  cropImageIndex.value = index;
  cropImageSrc.value = imagePreview.value[index].url;
  cropRotation.value = 0;
  showCropModal.value = true;
  showOriginalImage.value = false;
  imageZoomScale.value = 1;
  
  // 等待DOM更新后初始化canvas
  setTimeout(() => {
    initCropCanvas();
  }, 100);
};

const toggleOriginalView = () => {
  showOriginalImage.value = !showOriginalImage.value;
  // 切换到原图模式时重置缩放
  if (showOriginalImage.value) {
    imageZoomScale.value = 1;
  } else {
    // 返回编辑模式时重新绘制canvas
    setTimeout(() => {
      if (cropCanvas.value && cropImageElement.value) {
        drawCropCanvas();
      }
    }, 50);
  }
};

const onImageZoom = (e) => {
  // 滚轮向上放大，向下缩小
  const delta = e.deltaY > 0 ? -0.1 : 0.1;
  let newScale = imageZoomScale.value + delta;
  
  // 限制缩放范围：0.5倍到5倍
  newScale = Math.max(0.5, Math.min(5, newScale));
  
  imageZoomScale.value = newScale;
};

const initCropCanvas = () => {
  const canvas = document.getElementById('cropCanvas');
  if (!canvas) return;
  
  cropCanvas.value = canvas;
  cropCtx.value = canvas.getContext('2d');
  
  const img = new Image();
  img.onload = () => {
    cropImageElement.value = img;
    
    // 设置canvas大小以适应图片，使用更大的显示区域
    const maxWidth = Math.min(window.innerWidth * 0.8, 1200);
    const maxHeight = Math.min(window.innerHeight * 0.6, 800);
    let width = img.width;
    let height = img.height;
    
    if (width > maxWidth) {
      height = (height * maxWidth) / width;
      width = maxWidth;
    }
    if (height > maxHeight) {
      width = (width * maxHeight) / height;
      height = maxHeight;
    }
    
    canvas.width = width;
    canvas.height = height;
    
    // 初始化选区为整个图片
    cropSelection.value = { x: 0, y: 0, width: width, height: height };
    
    drawCropCanvas();
  };
  img.src = cropImageSrc.value;
};

const drawCropCanvas = () => {
  if (!cropCanvas.value || !cropImageElement.value || !cropCtx.value) return;
  
  const canvas = cropCanvas.value;
  const ctx = cropCtx.value;
  const img = cropImageElement.value;
  const sel = cropSelection.value;
  
  // 清空画布
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  
  // 保存状态
  ctx.save();
  
  // 应用旋转
  if (cropRotation.value !== 0) {
    ctx.translate(canvas.width / 2, canvas.height / 2);
    ctx.rotate((cropRotation.value * Math.PI) / 180);
    ctx.translate(-canvas.width / 2, -canvas.height / 2);
  }
  
  // 绘制完整图片
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
  
  ctx.restore();
  
  // 绘制半透明遮罩（选区外的部分）
  ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
  
  // 上方遮罩
  ctx.fillRect(0, 0, canvas.width, sel.y);
  // 下方遮罩
  ctx.fillRect(0, sel.y + sel.height, canvas.width, canvas.height - sel.y - sel.height);
  // 左侧遮罩
  ctx.fillRect(0, sel.y, sel.x, sel.height);
  // 右侧遮罩
  ctx.fillRect(sel.x + sel.width, sel.y, canvas.width - sel.x - sel.width, sel.height);
  
  // 绘制选区边框
  ctx.strokeStyle = '#3b82f6';
  ctx.lineWidth = 2;
  ctx.strokeRect(sel.x, sel.y, sel.width, sel.height);
  
  // 绘制调整手柄
  const handleSize = 10;
  ctx.fillStyle = '#3b82f6';
  // 四个角
  ctx.fillRect(sel.x - handleSize / 2, sel.y - handleSize / 2, handleSize, handleSize);
  ctx.fillRect(sel.x + sel.width - handleSize / 2, sel.y - handleSize / 2, handleSize, handleSize);
  ctx.fillRect(sel.x - handleSize / 2, sel.y + sel.height - handleSize / 2, handleSize, handleSize);
  ctx.fillRect(sel.x + sel.width - handleSize / 2, sel.y + sel.height - handleSize / 2, handleSize, handleSize);
  
  // 四条边中点
  ctx.fillRect(sel.x + sel.width / 2 - handleSize / 2, sel.y - handleSize / 2, handleSize, handleSize);
  ctx.fillRect(sel.x + sel.width / 2 - handleSize / 2, sel.y + sel.height - handleSize / 2, handleSize, handleSize);
  ctx.fillRect(sel.x - handleSize / 2, sel.y + sel.height / 2 - handleSize / 2, handleSize, handleSize);
  ctx.fillRect(sel.x + sel.width - handleSize / 2, sel.y + sel.height / 2 - handleSize / 2, handleSize, handleSize);
};

const onCanvasMouseDown = (e) => {
  const canvas = cropCanvas.value;
  if (!canvas) return;
  
  const rect = canvas.getBoundingClientRect();
  const x = e.clientX - rect.left;
  const y = e.clientY - rect.top;
  
  const sel = cropSelection.value;
  const handleSize = 10;
  
  // 检查是否点击在调整手柄上
  const handles = {
    'nw': { x: sel.x, y: sel.y },
    'ne': { x: sel.x + sel.width, y: sel.y },
    'sw': { x: sel.x, y: sel.y + sel.height },
    'se': { x: sel.x + sel.width, y: sel.y + sel.height },
    'n': { x: sel.x + sel.width / 2, y: sel.y },
    's': { x: sel.x + sel.width / 2, y: sel.y + sel.height },
    'w': { x: sel.x, y: sel.y + sel.height / 2 },
    'e': { x: sel.x + sel.width, y: sel.y + sel.height / 2 },
  };
  
  for (const [handle, pos] of Object.entries(handles)) {
    if (Math.abs(x - pos.x) <= handleSize && Math.abs(y - pos.y) <= handleSize) {
      isResizing.value = true;
      resizeHandle.value = handle;
      dragStartPos.value = { x, y };
      selectionStart.value = { ...sel };
      return;
    }
  }
  
  // 检查是否点击在选区内（拖动）
  if (x >= sel.x && x <= sel.x + sel.width && y >= sel.y && y <= sel.y + sel.height) {
    isDraggingCrop.value = true;
    dragStartPos.value = { x, y };
    selectionStart.value = { ...sel };
  }
};

const onCanvasMouseMove = (e) => {
  const canvas = cropCanvas.value;
  if (!canvas) return;
  
  const rect = canvas.getBoundingClientRect();
  const x = e.clientX - rect.left;
  const y = e.clientY - rect.top;
  
  if (isResizing.value) {
    const dx = x - dragStartPos.value.x;
    const dy = y - dragStartPos.value.y;
    const start = selectionStart.value;
    let newSel = { ...start };
    
    const handle = resizeHandle.value;
    
    // 根据手柄位置调整选区
    if (handle.includes('n')) {
      newSel.y = Math.max(0, Math.min(start.y + dy, start.y + start.height - 20));
      newSel.height = start.height - (newSel.y - start.y);
    }
    if (handle.includes('s')) {
      newSel.height = Math.max(20, Math.min(start.height + dy, canvas.height - start.y));
    }
    if (handle.includes('w')) {
      newSel.x = Math.max(0, Math.min(start.x + dx, start.x + start.width - 20));
      newSel.width = start.width - (newSel.x - start.x);
    }
    if (handle.includes('e')) {
      newSel.width = Math.max(20, Math.min(start.width + dx, canvas.width - start.x));
    }
    
    cropSelection.value = newSel;
    drawCropCanvas();
  } else if (isDraggingCrop.value) {
    const dx = x - dragStartPos.value.x;
    const dy = y - dragStartPos.value.y;
    const start = selectionStart.value;
    
    let newX = start.x + dx;
    let newY = start.y + dy;
    
    // 限制在canvas范围内
    newX = Math.max(0, Math.min(newX, canvas.width - start.width));
    newY = Math.max(0, Math.min(newY, canvas.height - start.height));
    
    cropSelection.value = { ...start, x: newX, y: newY };
    drawCropCanvas();
  }
};

const onCanvasMouseUp = () => {
  isDraggingCrop.value = false;
  isResizing.value = false;
  resizeHandle.value = '';
};

const rotateCropImage = (degrees) => {
  cropRotation.value = (cropRotation.value + degrees) % 360;
  
  // 旋转后重新初始化canvas
  if (!cropImageElement.value || !cropCanvas.value) return;
  
  const img = cropImageElement.value;
  const canvas = cropCanvas.value;
  
  // 如果是90度或270度旋转，需要交换宽高
  const isVerticalRotation = Math.abs(cropRotation.value) === 90 || Math.abs(cropRotation.value) === 270;
  
  const maxWidth = Math.min(window.innerWidth * 0.8, 1200);
  const maxHeight = Math.min(window.innerHeight * 0.6, 800);
  let width = isVerticalRotation ? img.height : img.width;
  let height = isVerticalRotation ? img.width : img.height;
  
  if (width > maxWidth) {
    height = (height * maxWidth) / width;
    width = maxWidth;
  }
  if (height > maxHeight) {
    width = (width * maxHeight) / height;
    height = maxHeight;
  }
  
  canvas.width = width;
  canvas.height = height;
  
  // 重置选区为整个图片
  cropSelection.value = { x: 0, y: 0, width: width, height: height };
  
  drawCropCanvas();
};

const resetCropSelection = () => {
  if (!cropCanvas.value) {
    console.error('Canvas未初始化');
    return;
  }
  
  console.log('重置选区到:', cropCanvas.value.width, 'x', cropCanvas.value.height);
  
  cropSelection.value = {
    x: 0,
    y: 0,
    width: cropCanvas.value.width,
    height: cropCanvas.value.height,
  };
  
  drawCropCanvas();
  
  console.log('选区已重置');
};

const applyCrop = async () => {
  if (!cropCanvas.value || !cropImageElement.value || cropImageIndex.value === null) return;
  
  const img = cropImageElement.value;
  const sel = cropSelection.value;
  const canvas = cropCanvas.value;
  
  console.log('开始裁剪');
  console.log('Canvas尺寸:', canvas.width, 'x', canvas.height);
  console.log('选区:', sel);
  console.log('旋转角度:', cropRotation.value);
  
  // 检查选区是否有效
  if (sel.width <= 0 || sel.height <= 0) {
    alert('选区无效，请重新选择');
    return;
  }
  
  // 创建临时canvas用于旋转
  const tempCanvas = document.createElement('canvas');
  const tempCtx = tempCanvas.getContext('2d');
  
  // 根据旋转角度设置临时canvas尺寸
  const isVerticalRotation = Math.abs(cropRotation.value) === 90 || Math.abs(cropRotation.value) === 270;
  tempCanvas.width = isVerticalRotation ? img.height : img.width;
  tempCanvas.height = isVerticalRotation ? img.width : img.height;
  
  console.log('原图尺寸:', img.width, 'x', img.height);
  console.log('旋转后尺寸:', tempCanvas.width, 'x', tempCanvas.height);
  
  // 应用旋转到临时canvas
  tempCtx.save();
  tempCtx.translate(tempCanvas.width / 2, tempCanvas.height / 2);
  tempCtx.rotate((cropRotation.value * Math.PI) / 180);
  tempCtx.drawImage(img, -img.width / 2, -img.height / 2);
  tempCtx.restore();
  
  // 创建结果canvas用于裁切
  const resultCanvas = document.createElement('canvas');
  const resultCtx = resultCanvas.getContext('2d');
  
  // 计算裁切区域在原图中的比例
  const scaleX = tempCanvas.width / canvas.width;
  const scaleY = tempCanvas.height / canvas.height;
  
  const cropX = sel.x * scaleX;
  const cropY = sel.y * scaleY;
  const cropWidth = sel.width * scaleX;
  const cropHeight = sel.height * scaleY;
  
  console.log('缩放比例:', scaleX, scaleY);
  console.log('实际裁剪区域:', cropX, cropY, cropWidth, cropHeight);
  
  resultCanvas.width = cropWidth;
  resultCanvas.height = cropHeight;
  
  // 从旋转后的图片中裁切
  resultCtx.drawImage(
    tempCanvas,
    cropX,
    cropY,
    cropWidth,
    cropHeight,
    0,
    0,
    cropWidth,
    cropHeight
  );
  
  console.log('结果canvas尺寸:', resultCanvas.width, 'x', resultCanvas.height);
  
  // 转换为Blob
  resultCanvas.toBlob((blob) => {
    if (!blob) {
      console.error('裁剪失败：无法生成图片');
      alert('裁剪失败，请重试');
      return;
    }
    
    const index = cropImageIndex.value;
    
    // 创建新的File对象
    const originalFile = imageFiles.value[index];
    const fileName = originalFile.name;
    const newFile = new File([blob], fileName, { type: 'image/jpeg' });
    
    console.log('裁剪前文件大小:', originalFile.size, '裁剪后文件大小:', newFile.size);
    
    // 更新文件数组
    const newFiles = [...imageFiles.value];
    newFiles[index] = newFile;
    imageFiles.value = newFiles;
    
    // 更新预览
    const reader = new FileReader();
    reader.onload = (e) => {
      const newImg = new Image();
      newImg.onload = () => {
        console.log('裁剪后图片尺寸:', newImg.width, 'x', newImg.height);
        
        // 创建新的预览对象，使用新的id强制Vue更新
        const newPreviews = [...imagePreview.value];
        newPreviews[index] = {
          id: nextId++, // 新的ID
          url: e.target.result,
          width: newImg.width,
          height: newImg.height,
          isPortrait: newImg.height > newImg.width,
          file: newFile,
        };
        imagePreview.value = newPreviews;
        
        console.log('预览已更新，新ID:', newPreviews[index].id);
      };
      newImg.src = e.target.result;
    };
    reader.readAsDataURL(newFile);
    
    closeCropModal();
  }, 'image/jpeg', 0.95);
};

const closeCropModal = () => {
  showCropModal.value = false;
  cropImageIndex.value = null;
  cropImageSrc.value = '';
  cropRotation.value = 0;
  showOriginalImage.value = false;
  imageZoomScale.value = 1;
};

const onDocChange = (e) => {
  const files = e.target.files;
  docFile.value = files && files.length ? files[0] : null;
};

const onSubmitImage = async () => {
  if (!imageTitle.value || !imageFiles.value.length) return;

  // 检查是否有横版图片
  const hasLandscape = imagePreview.value.some((p) => !p.isPortrait);
  if (hasLandscape) {
    if (!window.confirm(
      '检测到横版图片！\n\n横向或倒置的图片可能导致OCR识别失败或识别结果不准确。\n\n建议：\n1. 使用竖版拍摄的图片\n2. 或在手机相册中旋转图片后重新上传\n\n确定要继续上传吗？'
    )) {
      return;
    }
  }

  loadingImage.value = true;
  message.value = '';
  error.value = '';
  try {
    const formData = new FormData();
    formData.append('title', imageTitle.value);
    imageFiles.value.forEach((file) => {
      formData.append('file', file);
    });
    // POST /essay/image (multipart/form-data)
    const res = await http.post('/essay/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    const essayId = res.data;
    message.value = `图片OCR识别成功！已创建草稿（ID：${essayId}），正在跳转到作文列表...`;
    setTimeout(() => {
      router.push('/essays');
    }, 1500);
  } catch (e) {
    error.value = e.message || '图片上传创建失败';
  } finally {
    loadingImage.value = false;
  }
};

const onSubmitDocument = async () => {
  if (!docTitle.value || !docFile.value) return;
  loadingDoc.value = true;
  message.value = '';
  error.value = '';
  try {
    const formData = new FormData();
    formData.append('title', docTitle.value);
    formData.append('file', docFile.value);
    // POST /essay/document (multipart/form-data)
    const res = await http.post('/essay/document', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    const essayId = res.data;
    message.value = `文档上传成功，已创建草稿，作文ID：${essayId}`;
    setTimeout(() => {
      router.push('/essays');
    }, 800);
  } catch (e) {
    error.value = e.message || '文档上传创建失败';
  } finally {
    loadingDoc.value = false;
  }
};
</script>

<style scoped>
.dragging {
  opacity: 0.5;
  transform: scale(0.95);
}

.drag-over {
  border: 2px dashed #3b82f6 !important;
  background: rgba(59, 130, 246, 0.05);
}

.crop-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.crop-modal {
  background: white;
  border-radius: 8px;
  max-width: 700px;
  width: 90%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.crop-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.crop-modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.crop-close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6b7280;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.crop-close-btn:hover {
  background: #f3f4f6;
}

.crop-modal-body {
  padding: 20px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.crop-controls {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.crop-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
}
</style>

