<template>
  <div class="fs-grid">
    <div class="fs-grid fs-grid-two">
      <div class="fs-panel fs-panel-dark rule-intro">
        <h3>批改细则管理</h3>
        <p>这里负责查看系统中的所有批改细则。当前推荐优先维护规则本体，包括题干要求、自定义批改要求和扣分细则，评分项配置改为可选补充。</p>
        <div class="tip-list">
          <div>新增细则后，先进入编辑页补完整体规则字段即可。</div>
          <div>批改时只会使用启用中的细则参与提示词构建。</div>
        </div>
      </div>

      <div class="fs-card">
        <div class="fs-card-header">
          <div class="fs-card-title">快捷操作</div>
          <button class="fs-btn fs-btn-outline" @click="loadData">刷新配置</button>
        </div>
        <div class="fs-card-body fs-stack">
          <div class="meta-item">
            <span>当前细则数量</span>
            <strong>{{ rules.length }}</strong>
          </div>
          <div class="meta-item">
            <span>启用中的细则</span>
            <strong>{{ enabledCount }}</strong>
          </div>
          <div class="action-row">
            <button class="fs-btn fs-btn-primary" @click="goCreateRule">新增评分细则</button>
          </div>
        </div>
      </div>
    </div>

    <div class="fs-card">
      <div class="fs-card-header">
        <div class="fs-card-title">批改细则列表</div>
      </div>
      <div class="fs-card-body">
        <div v-if="rules.length" class="rule-grid">
          <div v-for="rule in rules" :key="rule.ruleId" class="rule-tile">
            <div class="rule-tile-top">
              <div class="rule-tile-heading">
                <strong>{{ rule.ruleName }}</strong>
                <div class="rule-tile-meta">{{ rule.gradeLevel || '未设置学段' }} / {{ rule.reviewType || '未设置类型' }}</div>
              </div>
              <span class="fs-tag rule-status-tag" :class="rule.status === 1 ? 'fs-tag-success' : 'fs-tag-neutral'">
                {{ rule.status === 1 ? '启用中' : '已停用' }}
              </span>
            </div>
            <div class="rule-tile-body">
              <div v-if="rule.topicRequirement" class="rule-field">
                <span class="rule-field-label">题干要求</span>
                <div class="rule-field-value">{{ rule.topicRequirement }}</div>
              </div>
              <div v-if="rule.customRequirement" class="rule-field">
                <span class="rule-field-label">自定义批改要求</span>
                <div class="rule-field-value">{{ rule.customRequirement }}</div>
              </div>
              <div v-if="rule.deductionDetail" class="rule-field">
                <span class="rule-field-label">扣分细则</span>
                <div class="rule-field-value">{{ rule.deductionDetail }}</div>
              </div>
            </div>
            <div class="action-row rule-action-row">
              <button class="fs-btn fs-btn-primary fs-btn-sm" @click="goEditRule(rule.ruleId)">编辑细则</button>
              <button class="fs-btn fs-btn-outline fs-btn-sm" @click="toggleRuleStatus(rule)">
                {{ rule.status === 1 ? '停用' : '启用' }}
              </button>
              <button class="fs-btn fs-btn-danger fs-btn-sm" @click="removeRule(rule)">删除</button>
            </div>
          </div>
        </div>
        <div v-else class="fs-empty">暂无批改细则，可先新增一条配置</div>
      </div>
    </div>

    <div v-if="error" class="fs-panel fs-panel-dark error-panel">{{ error }}</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()
const rules = ref([])
const error = ref('')

const enabledCount = computed(() => rules.value.filter((rule) => rule.status === 1).length)

const loadData = async () => {
  error.value = ''
  try {
    const res = await http.get('/review/rules', { params: { enabledOnly: false } })
    rules.value = res.data || []
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

const goCreateRule = () => {
  router.push('/dimensions/create')
}

const goEditRule = (ruleId) => {
  router.push(`/dimensions/${ruleId}/edit`)
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
    await loadData()
  } catch (e) {
    error.value = e.message || '删除细则失败'
  }
}

onMounted(loadData)
</script>

<style scoped>
.rule-intro {
  min-height: 100%;
}

.tip-list {
  display: grid;
  gap: 10px;
  margin-top: 18px;
  color: rgba(255, 255, 255, 0.82);
}

.meta-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(56, 44, 31, 0.08);
}

.meta-item span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.meta-item strong {
  font-size: 15px;
  line-height: 1.7;
  font-weight: 600;
}

.rule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.rule-tile {
  display: grid;
  gap: 14px;
  border: 1px solid rgba(34, 77, 105, 0.1);
  background:
    radial-gradient(circle at top right, rgba(66, 141, 109, 0.09), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(246, 250, 252, 0.96));
  border-radius: 20px;
  padding: 18px 18px 16px;
  box-shadow: 0 10px 26px rgba(39, 72, 99, 0.06);
}

.rule-tile-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.rule-tile-heading {
  min-width: 0;
}

.rule-tile-heading strong {
  display: block;
  margin: 0;
  color: #1d3f53;
  font-size: 15px;
  line-height: 1.45;
  font-weight: 700;
}

.rule-tile-meta {
  margin-top: 5px;
  color: rgba(34, 77, 105, 0.64);
  font-size: 11.5px;
  line-height: 1.5;
  letter-spacing: 0.02em;
}

.rule-status-tag {
  flex-shrink: 0;
  font-size: 11px;
  padding: 3px 9px;
}

.rule-tile-body {
  display: grid;
  gap: 10px;
}

.rule-field {
  padding: 10px 11px;
  border-radius: 14px;
  background: rgba(244, 248, 250, 0.92);
  border: 1px solid rgba(34, 77, 105, 0.08);
}

.rule-field-label {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(34, 77, 105, 0.08);
  color: #31586f;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.rule-field-value {
  margin-top: 7px;
  color: #52616d;
  font-size: 12px;
  line-height: 1.68;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  overflow: hidden;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rule-action-row {
  margin-top: 2px;
  padding-top: 2px;
}

.error-panel {
  color: #fff4f4;
  background: linear-gradient(135deg, #913535, #6c2323);
}

@media (max-width: 860px) {
  .rule-tile-top {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
