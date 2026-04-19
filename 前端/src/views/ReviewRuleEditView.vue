<template>
  <div v-if="selectedRule" class="fs-grid">
    <div class="fs-grid fs-grid-two">
      <div class="fs-card">
        <div class="fs-card-header">
          <div class="fs-card-title">编辑评分细则</div>
          <button class="fs-btn fs-btn-outline" @click="router.push('/dimensions')">返回列表</button>
        </div>
        <div class="fs-card-body">
          <form class="fs-form" @submit.prevent="onUpdateRule">
            <div class="config-row">
              <div class="fs-form-item">
                <label class="fs-form-label">细则名称</label>
                <input v-model="ruleEditForm.ruleName" class="fs-input" required />
              </div>
              <div class="fs-form-item">
                <label class="fs-form-label">批改类型</label>
                <input v-model="ruleEditForm.reviewType" class="fs-input" />
              </div>
            </div>

            <div class="config-row">
              <div class="fs-form-item">
                <label class="fs-form-label">适用学段</label>
                <input v-model="ruleEditForm.gradeLevel" class="fs-input" />
              </div>
              <div class="fs-form-item">
                <label class="fs-form-label">原文美化等级</label>
                <input v-model="ruleEditForm.beautifyLevel" class="fs-input" />
              </div>
            </div>

            <div class="fs-form-item">
              <label class="fs-form-label">题干要求</label>
              <textarea v-model="ruleEditForm.topicRequirement" class="fs-input fs-textarea" rows="3" />
            </div>

            <div class="fs-form-item">
              <label class="fs-form-label">自定义批改要求</label>
              <textarea v-model="ruleEditForm.customRequirement" class="fs-input fs-textarea" rows="3" />
            </div>

            <div class="fs-form-item">
              <label class="fs-form-label">扣分细则</label>
              <textarea v-model="ruleEditForm.deductionDetail" class="fs-input fs-textarea" rows="3" />
            </div>

            <div class="fs-form-item">
              <label class="fs-form-label">补充提示词</label>
              <textarea v-model="ruleEditForm.promptTemplate" class="fs-input fs-textarea" rows="3" />
            </div>

            <div class="action-row">
              <button type="submit" class="fs-btn fs-btn-primary" :disabled="updatingRule">
                {{ updatingRule ? '保存中...' : '保存细则' }}
              </button>
              <button type="button" class="fs-btn" :class="selectedRule.status === 1 ? 'fs-btn-warning' : 'fs-btn-success'" @click="toggleRuleStatus(selectedRule)">
                {{ selectedRule.status === 1 ? '停用细则' : '启用细则' }}
              </button>
              <button type="button" class="fs-btn fs-btn-danger" @click="removeRule(selectedRule)">删除细则</button>
            </div>
          </form>
        </div>
      </div>

      <div class="fs-card">
      <div class="fs-card-header">
          <div class="fs-card-title">按需补充评分项</div>
        </div>
        <div class="fs-card-body">
          <form class="fs-form" @submit.prevent="onCreateDimension">
            <div class="fs-form-item">
              <label class="fs-form-label">评分项名称</label>
              <input v-model="dimensionForm.dimensionName" class="fs-input" placeholder="如：内容评价" required />
            </div>

            <div class="config-row config-row-three">
              <div class="fs-form-item">
                <label class="fs-form-label">权重</label>
                <input v-model.number="dimensionForm.weight" type="number" min="0" max="100" class="fs-input" required />
              </div>
              <div class="fs-form-item">
                <label class="fs-form-label">满分值</label>
                <input v-model.number="dimensionForm.maxScore" type="number" min="1" class="fs-input" required />
              </div>
              <div class="fs-form-item">
                <label class="fs-form-label">排序值</label>
                <input v-model.number="dimensionForm.sortOrder" type="number" min="0" class="fs-input" />
              </div>
            </div>

            <div class="fs-form-item">
              <label class="fs-form-label">评分说明</label>
              <textarea v-model="dimensionForm.description" class="fs-input fs-textarea" rows="3" placeholder="描述该评分项重点考察什么" />
            </div>

            <button type="submit" class="fs-btn fs-btn-primary" :disabled="creatingDimension">
              {{ creatingDimension ? '创建中...' : '新增评分项' }}
            </button>
          </form>
        </div>
      </div>
    </div>

    <div class="fs-card">
      <div class="fs-card-header">
        <div class="fs-card-title">评分项列表 - {{ selectedRule.ruleName }}</div>
      </div>
      <div class="fs-card-body">
        <table v-if="dimensions.length" class="fs-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>评分项</th>
              <th>说明</th>
              <th>权重</th>
              <th>满分值</th>
              <th>排序</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in dimensions" :key="item.dimensionId">
              <td>{{ item.dimensionId }}</td>
              <td><input v-model="item.dimensionName" class="table-input" /></td>
              <td><textarea v-model="item.description" class="table-textarea" rows="2" /></td>
              <td><input v-model.number="item.weight" type="number" min="0" max="100" class="table-input small-input" /></td>
              <td><input v-model.number="item.maxScore" type="number" min="1" class="table-input small-input" /></td>
              <td><input v-model.number="item.sortOrder" type="number" min="0" class="table-input small-input" /></td>
              <td>
                <span class="fs-tag" :class="item.status === 1 ? 'fs-tag-success' : 'fs-tag-neutral'">
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td>
                <div class="action-row">
                  <button class="fs-btn fs-btn-primary fs-btn-sm" @click="saveDimension(item)">保存</button>
                  <button class="fs-btn fs-btn-outline fs-btn-sm" @click="toggleDimensionStatus(item)">
                    {{ item.status === 1 ? '禁用' : '启用' }}
                  </button>
                  <button class="fs-btn fs-btn-danger fs-btn-sm" @click="removeDimension(item)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="fs-empty">当前细则下还没有评分项；如果你当前主要依赖题干要求和扣分细则，这里可以暂时不配。</div>
      </div>
    </div>
  </div>

  <div v-else class="fs-card">
    <div class="fs-empty">正在加载评分细则...</div>
  </div>

  <div v-if="error" class="fs-panel fs-panel-dark error-panel">{{ error }}</div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const router = useRouter()

const selectedRule = ref(null)
const dimensions = ref([])
const error = ref('')
const updatingRule = ref(false)
const creatingDimension = ref(false)

const ruleEditForm = reactive({
  ruleName: '',
  reviewType: '',
  gradeLevel: '',
  promptTemplate: '',
  topicRequirement: '',
  beautifyLevel: '',
  customRequirement: '',
  deductionDetail: '',
})

const dimensionForm = reactive({
  dimensionName: '',
  weight: 25,
  maxScore: 25,
  description: '',
  sortOrder: 0,
})

const fillRuleEditForm = (rule) => {
  ruleEditForm.ruleName = rule?.ruleName || ''
  ruleEditForm.reviewType = rule?.reviewType || ''
  ruleEditForm.gradeLevel = rule?.gradeLevel || ''
  ruleEditForm.promptTemplate = rule?.promptTemplate || ''
  ruleEditForm.topicRequirement = rule?.topicRequirement || ''
  ruleEditForm.beautifyLevel = rule?.beautifyLevel || ''
  ruleEditForm.customRequirement = rule?.customRequirement || ''
  ruleEditForm.deductionDetail = rule?.deductionDetail || ''
}

const resetDimensionForm = () => {
  dimensionForm.dimensionName = ''
  dimensionForm.weight = 25
  dimensionForm.maxScore = 25
  dimensionForm.description = ''
  dimensionForm.sortOrder = 0
}

const loadRule = async () => {
  const ruleId = Number(route.params.ruleId)
  const res = await http.get('/review/rules', { params: { enabledOnly: false } })
  const rules = res.data || []
  const found = rules.find((item) => item.ruleId === ruleId)
  if (!found) {
    throw new Error('评分细则不存在')
  }
  selectedRule.value = found
  fillRuleEditForm(found)
}

const loadDimensions = async () => {
  if (!selectedRule.value?.ruleId) {
    dimensions.value = []
    return
  }
  const res = await http.get('/review/dimensions', {
    params: {
      ruleId: selectedRule.value.ruleId,
      enabledOnly: false,
    },
  })
  dimensions.value = (res.data || []).map((item) => ({
    ...item,
    description: item.description || '',
    sortOrder: item.sortOrder ?? 0,
  }))
}

const loadData = async () => {
  error.value = ''
  await loadRule()
  await loadDimensions()
}

const onUpdateRule = async () => {
  if (!selectedRule.value?.ruleId) return
  updatingRule.value = true
  error.value = ''
  try {
    await http.put(`/review/rules/${selectedRule.value.ruleId}`, { ...ruleEditForm })
    await loadData()
  } catch (e) {
    error.value = e.message || '更新批改细则失败'
  } finally {
    updatingRule.value = false
  }
}

const toggleRuleStatus = async (rule) => {
  try {
    await http.patch(`/review/rules/${rule.ruleId}/status`, null, {
      params: { enabled: rule.status !== 1 },
    })
    await loadData()
  } catch (e) {
    error.value = e.message || '更新细则状态失败'
  }
}

const removeRule = async (rule) => {
  if (!window.confirm(`确定删除批改细则「${rule.ruleName}」吗？`)) return
  try {
    await http.delete(`/review/rules/${rule.ruleId}`)
    router.push('/dimensions')
  } catch (e) {
    error.value = e.message || '删除细则失败'
  }
}

const onCreateDimension = async () => {
  if (!selectedRule.value?.ruleId) return
  creatingDimension.value = true
  error.value = ''
  try {
    await http.post('/review/dimensions', {
      ...dimensionForm,
      ruleId: selectedRule.value.ruleId,
    })
    resetDimensionForm()
    await loadDimensions()
  } catch (e) {
    error.value = e.message || '创建评分项失败'
  } finally {
    creatingDimension.value = false
  }
}

const saveDimension = async (item) => {
  try {
    await http.put(`/review/dimensions/${item.dimensionId}`, {
      ruleId: selectedRule.value.ruleId,
      dimensionName: item.dimensionName,
      weight: item.weight,
      maxScore: item.maxScore,
      description: item.description,
      sortOrder: item.sortOrder,
    })
    await loadDimensions()
  } catch (e) {
    error.value = e.message || '保存评分项失败'
  }
}

const toggleDimensionStatus = async (item) => {
  try {
    await http.patch(`/review/dimensions/${item.dimensionId}/status`, null, {
      params: { enabled: item.status !== 1 },
    })
    await loadDimensions()
  } catch (e) {
    error.value = e.message || '更新评分项状态失败'
  }
}

const removeDimension = async (item) => {
  if (!window.confirm(`确定删除评分项「${item.dimensionName}」吗？`)) return
  try {
    await http.delete(`/review/dimensions/${item.dimensionId}`)
    await loadDimensions()
  } catch (e) {
    error.value = e.message || '删除评分项失败'
  }
}

onMounted(async () => {
  try {
    await loadData()
  } catch (e) {
    error.value = e.message || '加载评分细则失败'
  }
})
</script>

<style scoped>
.config-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.config-row-three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.fs-textarea {
  min-height: 96px;
  resize: vertical;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.table-input,
.table-textarea {
  width: 100%;
  border: 1px solid rgba(34, 77, 105, 0.16);
  border-radius: 12px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.94);
  color: #21475f;
}

.table-textarea {
  min-height: 70px;
  resize: vertical;
}

.small-input {
  min-width: 88px;
}

.error-panel {
  margin-top: 18px;
  color: #fff4f4;
  background: linear-gradient(135deg, #913535, #6c2323);
}

.fs-btn-warning {
  background: #f0b24d;
  color: #5b3900;
}

.fs-btn-success {
  background: #4caf7d;
  color: #083d25;
}

@media (max-width: 860px) {
  .config-row,
  .config-row-three {
    grid-template-columns: 1fr;
  }
}
</style>
