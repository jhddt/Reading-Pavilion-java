<template>
  <div class="fs-grid fs-grid-two">
    <div class="fs-card">
      <div class="fs-card-header">
        <div class="fs-card-title">新增评分细则</div>
        <button class="fs-btn fs-btn-outline" @click="$router.back()">返回</button>
      </div>
      <div class="fs-card-body">
        <form class="fs-form" @submit.prevent="onCreateRule">
          <div class="config-row">
            <div class="fs-form-item">
              <label class="fs-form-label">细则名称</label>
              <input v-model="ruleForm.ruleName" class="fs-input" placeholder="如：小学记叙文批改细则" required />
            </div>
            <div class="fs-form-item">
              <label class="fs-form-label">批改类型</label>
              <input v-model="ruleForm.reviewType" class="fs-input" placeholder="如：通用作文" />
            </div>
          </div>

          <div class="config-row">
            <div class="fs-form-item">
              <label class="fs-form-label">适用学段</label>
              <input v-model="ruleForm.gradeLevel" class="fs-input" placeholder="如：小学五年级" />
            </div>
            <div class="fs-form-item">
              <label class="fs-form-label">原文美化等级</label>
              <input v-model="ruleForm.beautifyLevel" class="fs-input" placeholder="如：轻度 / 中度" />
            </div>
          </div>

          <div class="fs-form-item">
            <label class="fs-form-label">题干要求</label>
            <textarea v-model="ruleForm.topicRequirement" class="fs-input fs-textarea" rows="3" placeholder="填写题干、体裁、字数等要求" />
          </div>

          <div class="fs-form-item">
            <label class="fs-form-label">自定义批改要求</label>
            <textarea v-model="ruleForm.customRequirement" class="fs-input fs-textarea" rows="3" placeholder="如：重点关注中心思想和细节描写" />
          </div>

          <div class="fs-form-item">
            <label class="fs-form-label">扣分细则</label>
            <textarea v-model="ruleForm.deductionDetail" class="fs-input fs-textarea" rows="3" placeholder="如：偏题、字数不足、结构混乱分别如何扣分" />
          </div>

          <div class="fs-form-item">
            <label class="fs-form-label">补充提示词</label>
            <textarea v-model="ruleForm.promptTemplate" class="fs-input fs-textarea" rows="3" placeholder="追加给 AI 的额外提示词" />
          </div>

          <div class="action-row">
            <button type="submit" class="fs-btn fs-btn-primary" :disabled="creatingRule">
              {{ creatingRule ? '创建中...' : '创建细则' }}
            </button>
            <button type="button" class="fs-btn fs-btn-outline" @click="router.push('/dimensions')">返回细则列表</button>
          </div>
        </form>
      </div>
    </div>

    <div class="fs-panel fs-panel-dark rule-intro">
      <h3>创建后下一步</h3>
      <p>创建评分细则后，系统会自动进入该细则的编辑页。你可以先把题干要求、扣分细则和自定义批改要求补完整，评分项配置改为按需补充。</p>
    </div>
  </div>

  <div v-if="error" class="fs-panel fs-panel-dark error-panel">{{ error }}</div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()
const creatingRule = ref(false)
const error = ref('')

const ruleForm = reactive({
  ruleName: '',
  reviewType: '',
  gradeLevel: '',
  promptTemplate: '',
  topicRequirement: '',
  beautifyLevel: '',
  customRequirement: '',
  deductionDetail: '',
})

const onCreateRule = async () => {
  creatingRule.value = true
  error.value = ''
  try {
    const res = await http.post('/review/rules', { ...ruleForm })
    const createdRuleId = res.data?.ruleId
    if (createdRuleId) {
      router.push(`/dimensions/${createdRuleId}/edit`)
      return
    }
    router.push('/dimensions')
  } catch (e) {
    error.value = e.message || '创建批改细则失败'
  } finally {
    creatingRule.value = false
  }
}
</script>

<style scoped>
.config-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.fs-textarea {
  min-height: 96px;
  resize: vertical;
}

.rule-intro {
  min-height: 100%;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.error-panel {
  margin-top: 18px;
  color: #fff4f4;
  background: linear-gradient(135deg, #913535, #6c2323);
}

@media (max-width: 860px) {
  .config-row {
    grid-template-columns: 1fr;
  }
}
</style>
