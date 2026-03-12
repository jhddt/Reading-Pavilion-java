<template>
  <div class="rp-card" v-if="detail">
    <div class="rp-card-header">
      <div class="rp-card-title">批改详情 · ID {{ detail.reviewId }}</div>
      <button class="rp-btn rp-btn-outline" @click="$router.back()">返回</button>
    </div>

    <!-- 左右布局：图片在左，批注在右 -->
    <div class="review-layout" v-if="images.length > 0">
      <!-- 左侧：图片区域 -->
      <div class="image-section">
        <div v-for="(img, imgIndex) in images" :key="img.fileId" class="image-container">
          <div class="image-header">
            第 {{ imgIndex + 1 }} 页
          </div>
          
          <!-- 图片 + 标注 -->
          <div class="image-wrapper">
            <div style="position: relative; display: inline-block; width: 100%">
              <img
                :ref="el => imageRefs[imgIndex] = el"
                :src="img.imageUrl"
                alt="作文图片"
                style="width: 100%; display: block"
                @load="() => onImageLoad(imgIndex)"
              />
              
              <!-- 纠错标记（红色序号） -->
              <div
                v-for="(correction, idx) in getCorrectionsForImage(imgIndex)"
                :key="'correction-' + correction.correctionId"
                :style="getCorrectionMarkerStyle(correction, imgIndex)"
                class="correction-marker"
                @click="selectCorrection(correction)"
                :class="{ active: selectedCorrection?.correctionId === correction.correctionId }"
                :title="correction.originalText"
              >
                {{ idx + 1 }}
              </div>
              
              <!-- 评价标记（黄色序号） -->
              <div
                v-for="(comment, idx) in getCommentsForImage(imgIndex)"
                :key="'comment-' + comment.commentId"
                :style="getCommentMarkerStyle(comment, imgIndex)"
                class="comment-marker"
                @click="selectComment(comment)"
                :class="{ active: selectedComment?.commentId === comment.commentId }"
                :title="comment.relatedText || comment.content.substring(0, 20) + '...'"
              >
                {{ idx + 1 }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：批注区域 -->
      <div class="annotation-section">
        <!-- 总分和总评 -->
        <div class="summary-card">
          <div class="score-display">
            <div class="total-score">
              <span class="score-label">总分</span>
              <span class="score-value">{{ detail.totalScore?.toFixed(2) || '-' }}</span>
            </div>
            <div class="dimension-scores">
              <div v-for="s in detail.scores" :key="s.dimensionId" class="dimension-item">
                <div class="dimension-name">{{ s.dimensionName }}</div>
                <div class="dimension-score">{{ s.score?.toFixed(1) || '-' }}</div>
              </div>
            </div>
          </div>
          
          <!-- 总评 -->
          <div v-for="c in getSummaryComments()" :key="c.commentId" class="summary-comment">
            <div class="comment-title">【总评】</div>
            <div class="comment-content">{{ c.content }}</div>
          </div>
        </div>

        <!-- 文本纠错列表 -->
        <div v-if="detail.textCorrections && detail.textCorrections.length > 0" class="annotation-group">
          <div class="group-title">文本纠错 ({{ detail.textCorrections.length }})</div>
          <div
            v-for="(correction, idx) in detail.textCorrections"
            :key="correction.correctionId"
            class="annotation-item"
            :class="{ active: selectedCorrection?.correctionId === correction.correctionId }"
            @click="selectCorrection(correction)"
          >
            <div class="item-number correction-number">{{ idx + 1 }}</div>
            <div class="item-content">
              <div class="correction-pair">
                <div class="original-text">{{ correction.originalText }}</div>
                <div class="arrow">→</div>
                <div class="corrected-text">{{ correction.correctedText }}</div>
              </div>
              <div v-if="correction.suggestion" class="suggestion-text">
                💡 {{ correction.suggestion }}
              </div>
            </div>
          </div>
        </div>

        <!-- 改进建议列表 -->
        <div v-if="getSuggestionComments().length > 0" class="annotation-group">
          <div class="group-title">改进建议 ({{ getSuggestionComments().length }})</div>
          <div
            v-for="(comment, idx) in getSuggestionComments()"
            :key="comment.commentId"
            class="annotation-item"
            :class="{ active: selectedComment?.commentId === comment.commentId }"
            @click="selectComment(comment)"
          >
            <div class="item-number suggestion-number">{{ idx + 1 }}</div>
            <div class="item-content">
              <div v-if="comment.relatedText" class="related-text">原文：{{ comment.relatedText }}</div>
              <div class="comment-text">{{ comment.content }}</div>
            </div>
          </div>
        </div>

        <!-- 修改意见列表 -->
        <div v-if="getRevisionComments().length > 0" class="annotation-group">
          <div class="group-title">修改意见 ({{ getRevisionComments().length }})</div>
          <div
            v-for="(comment, idx) in getRevisionComments()"
            :key="comment.commentId"
            class="annotation-item"
            :class="{ active: selectedComment?.commentId === comment.commentId }"
            @click="selectComment(comment)"
          >
            <div class="item-number revision-number">{{ idx + 1 }}</div>
            <div class="item-content">
              <div v-if="comment.relatedText" class="related-text">原文：{{ comment.relatedText }}</div>
              <div class="comment-text">{{ comment.content }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import http from '../api/http';

const route = useRoute();
const detail = ref(null);
const images = ref([]);
const imageRefs = ref([]);
const imageScales = ref([]);
const selectedCorrection = ref(null);
const selectedComment = ref(null);

const onImageLoad = (imgIndex) => {
  if (imageRefs.value[imgIndex] && images.value[imgIndex]?.imageWidth) {
    imageScales.value[imgIndex] = imageRefs.value[imgIndex].clientWidth / images.value[imgIndex].imageWidth;
  }
};

const getCommentTypeIcon = (type) => {
  if (type === 2) return '💡'; // 改进建议
  if (type === 3) return '✎';  // 修改意见
  return '📝';
};

const getCorrectionMarkerStyle = (correction, imgIndex) => {
  const position = findTextPosition(correction.startOffset, imgIndex);
  if (!position) return { display: 'none' };
  
  const scale = imageScales.value[imgIndex] || 1;
  const x = position.centerX * scale;
  const y = position.centerY * scale;
  
  return {
    position: 'absolute',
    left: `${x}px`,
    top: `${y}px`,
    transform: 'translate(-50%, -50%)',
  };
};

const getCommentMarkerStyle = (comment, imgIndex) => {
  if (!comment.startOffset && comment.startOffset !== 0) {
    console.log('评论没有 startOffset，不显示标记');
    return { display: 'none' };
  }
  
  const position = findTextPosition(comment.startOffset, imgIndex);
  if (!position) {
    console.log('评论找不到位置，不显示标记');
    return { display: 'none' };
  }
  
  const scale = imageScales.value[imgIndex] || 1;
  const x = position.centerX * scale;
  const y = position.centerY * scale;
  
  return {
    position: 'absolute',
    left: `${x + 20}px`,  // 偏移避免与纠错标记重叠
    top: `${y - 20}px`,   // 向上偏移
    transform: 'translate(-50%, -50%)',
  };
};

const findTextPosition = (offset, imgIndex) => {
  const img = images.value[imgIndex];
  if (!img) {
    console.log('findTextPosition: 图片不存在', imgIndex);
    return null;
  }
  
  let currentOffset = 0;
  
  // 计算这张图片之前的所有文本长度
  for (let i = 0; i < imgIndex; i++) {
    currentOffset += images.value[i].textBlocks.reduce((sum, block) => sum + block.text.length + 1, 0);
  }
  
  console.log(`查找位置: offset=${offset}, imgIndex=${imgIndex}, 图片起始offset=${currentOffset}`);
  
  // 在当前图片的文本块中查找
  for (const block of img.textBlocks) {
    const blockLength = block.text.length;
    if (currentOffset <= offset && offset < currentOffset + blockLength) {
      console.log('找到文本块:', {
        blockText: block.text.substring(0, 20),
        centerX: block.centerX,
        centerY: block.centerY
      });
      return {
        centerX: block.centerX,
        centerY: block.centerY
      };
    }
    currentOffset += blockLength + 1; // +1 for newline
  }
  
  console.log('未找到对应的文本块');
  return null;
};

const getCorrectionsForImage = (imgIndex) => {
  if (!detail.value?.textCorrections) return [];
  
  const img = images.value[imgIndex];
  if (!img) return [];
  
  let startOffset = 0;
  for (let i = 0; i < imgIndex; i++) {
    startOffset += images.value[i].textBlocks.reduce((sum, block) => sum + block.text.length + 1, 0);
  }
  
  const endOffset = startOffset + img.textBlocks.reduce((sum, block) => sum + block.text.length + 1, 0);
  
  return detail.value.textCorrections.filter(tc => 
    tc.startOffset >= startOffset && tc.startOffset < endOffset
  );
};

const getCommentsForImage = (imgIndex) => {
  if (!detail.value?.comments) {
    console.log('没有评论数据');
    return [];
  }
  
  const img = images.value[imgIndex];
  if (!img) {
    console.log('图片不存在:', imgIndex);
    return [];
  }
  
  let startOffset = 0;
  for (let i = 0; i < imgIndex; i++) {
    startOffset += images.value[i].textBlocks.reduce((sum, block) => sum + block.text.length + 1, 0);
  }
  
  const endOffset = startOffset + img.textBlocks.reduce((sum, block) => sum + block.text.length + 1, 0);
  
  console.log(`图片 ${imgIndex + 1} 的文本范围:`, { startOffset, endOffset });
  
  // 只显示改进建议(2)和修改意见(3)，不显示总评(1)
  const filtered = detail.value.comments.filter(c => 
    c.commentType !== 1 && 
    c.startOffset != null &&
    c.startOffset >= startOffset && 
    c.startOffset < endOffset
  );
  
  console.log(`图片 ${imgIndex + 1} 的评论标记数量:`, filtered.length);
  filtered.forEach(c => {
    console.log('  - 评论:', {
      commentType: c.commentType,
      startOffset: c.startOffset,
      relatedText: c.relatedText?.substring(0, 20)
    });
  });
  
  return filtered;
};

const getSummaryComments = () => {
  if (!detail.value?.comments) return [];
  return detail.value.comments.filter(c => c.commentType === 1);
};

const getSuggestionComments = () => {
  if (!detail.value?.comments) return [];
  return detail.value.comments.filter(c => c.commentType === 2);
};

const getRevisionComments = () => {
  if (!detail.value?.comments) return [];
  return detail.value.comments.filter(c => c.commentType === 3);
};

const selectCorrection = (correction) => {
  selectedCorrection.value = correction;
  selectedComment.value = null;
};

const selectComment = (comment) => {
  selectedComment.value = comment;
  selectedCorrection.value = null;
};

const clearSelection = () => {
  selectedCorrection.value = null;
  selectedComment.value = null;
};

const loadData = async () => {
  const reviewId = route.params.reviewId;
  if (!reviewId) return;
  
  try {
    console.log('开始加载批改详情，reviewId:', reviewId);
    const res = await http.get(`/review/record/${reviewId}`);
    detail.value = res.data;
    console.log('批改详情加载成功:', detail.value);
    console.log('评论数量:', detail.value?.comments?.length);
    console.log('纠错数量:', detail.value?.textCorrections?.length);
    
    // 打印评论的位置信息
    if (detail.value?.comments) {
      detail.value.comments.forEach((c, idx) => {
        console.log(`评论 ${idx + 1}:`, {
          commentType: c.commentType,
          hasStartOffset: c.startOffset != null,
          startOffset: c.startOffset,
          endOffset: c.endOffset,
          relatedText: c.relatedText?.substring(0, 20)
        });
      });
    }
    
    if (detail.value?.essayId) {
      const essayRes = await http.get(`/essay/${detail.value.essayId}`);
      const essay = essayRes.data;
      console.log('作文信息:', { essayId: essay.id, submitType: essay.submitType });
      
      if (essay.submitType === 0) {
        const filesRes = await http.get(`/file/essay/${essay.id}`);
        const files = filesRes.data || [];
        console.log('作文文件数量:', files.length);
        
        for (const file of files) {
          try {
            console.log('加载文件 OCR 信息:', file.id);
            const ocrRes = await http.get(`/ocr/file/${file.id}`);
            const ocrRecord = ocrRes.data;
            
            if (ocrRecord?.ocrId) {
              const ocrDetailRes = await http.get(`/ocr/${ocrRecord.ocrId}/detail`);
              const ocrDetail = ocrDetailRes.data;
              console.log('OCR 详情:', {
                ocrId: ocrDetail.ocrId,
                imageWidth: ocrDetail.imageWidth,
                imageHeight: ocrDetail.imageHeight,
                textBlocksCount: ocrDetail.textBlocks?.length
              });
              
              const fileUrlRes = await http.get(`/file/url/path`, {
                params: { filePath: file.filePath }
              });
              
              images.value.push({
                fileId: file.id,
                fileName: file.fileName,
                imageUrl: fileUrlRes.data,
                imageWidth: ocrDetail.imageWidth,
                imageHeight: ocrDetail.imageHeight,
                textBlocks: ocrDetail.textBlocks || []
              });
              console.log('图片加载成功:', file.fileName);
            }
          } catch (e) {
            console.error(`获取文件 ${file.id} 的 OCR 详情失败:`, e);
          }
        }
        
        console.log('所有图片加载完成，总数:', images.value.length);
      }
    }
  } catch (e) {
    console.error('加载批改详情失败:', e);
    alert(e.message || '加载失败');
  }
};

onMounted(loadData);
</script>

<style scoped>
/* 左右布局 */
.review-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 左侧图片区域 */
.image-section {
  flex: 1;
  min-width: 0;
}

.image-container {
  margin-bottom: 32px;
}

.image-header {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #374151;
}

.image-wrapper {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #f9fafb;
}

/* 右侧批注区域 */
.annotation-section {
  width: 400px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  padding-right: 8px;
}

/* 总分卡片 */
.summary-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.score-display {
  margin-bottom: 16px;
}

.total-score {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.score-label {
  font-size: 14px;
  color: #6b7280;
}

.score-value {
  font-size: 32px;
  font-weight: bold;
  color: #1f2937;
}

.dimension-scores {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.dimension-item {
  text-align: center;
  padding: 8px;
  background: #f9fafb;
  border-radius: 6px;
}

.dimension-name {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.dimension-score {
  font-size: 18px;
  font-weight: 600;
  color: #3b82f6;
}

.summary-comment {
  border-top: 1px solid #e5e7eb;
  padding-top: 12px;
}

.comment-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #374151;
}

.comment-content {
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.6;
  color: #4b5563;
}

/* 批注组 */
.annotation-group {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
}

/* 批注项 */
.annotation-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  background: #f9fafb;
}

.annotation-item:hover {
  background: #f3f4f6;
}

.annotation-item.active {
  background: #dbeafe;
  border: 1px solid #3b82f6;
}

.item-number {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
}

.correction-number {
  background: #ef4444;
}

.suggestion-number {
  background: #f59e0b;
}

.revision-number {
  background: #8b5cf6;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.correction-pair {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.original-text {
  padding: 6px 8px;
  background: #fee2e2;
  border-radius: 4px;
  font-size: 12px;
  color: #991b1b;
  text-decoration: line-through;
  flex: 1;
  word-break: break-all;
}

.arrow {
  font-size: 14px;
  color: #9ca3af;
  flex-shrink: 0;
}

.corrected-text {
  padding: 6px 8px;
  background: #d1fae5;
  border-radius: 4px;
  font-size: 12px;
  color: #065f46;
  flex: 1;
  word-break: break-all;
}

.suggestion-text {
  font-size: 11px;
  color: #6b7280;
  margin-top: 4px;
}

.related-text {
  padding: 6px 8px;
  background: #fef3c7;
  border-radius: 4px;
  font-size: 11px;
  color: #92400e;
  margin-bottom: 6px;
  word-break: break-all;
}

.comment-text {
  font-size: 12px;
  line-height: 1.5;
  color: #4b5563;
  word-break: break-all;
}

/* 图片上的标记 */
.correction-marker {
  width: 24px;
  height: 24px;
  background: rgba(239, 68, 68, 0.95);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  z-index: 5;
}

.correction-marker:hover,
.correction-marker.active {
  background: rgba(220, 38, 38, 1);
  transform: translate(-50%, -50%) scale(1.3);
  z-index: 15;
}

.comment-marker {
  width: 24px;
  height: 24px;
  background: rgba(245, 158, 11, 0.95);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  z-index: 5;
}

.comment-marker:hover,
.comment-marker.active {
  background: rgba(217, 119, 6, 1);
  transform: translate(-50%, -50%) scale(1.3);
  z-index: 15;
}

/* 滚动条样式 */
.annotation-section::-webkit-scrollbar {
  width: 6px;
}

.annotation-section::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.annotation-section::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.annotation-section::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
