<template>
  <div class="dm-dispatch-rail">
    <div :class="['dm-dispatch-hero', `tone-${dispatchPriority.tone}`]">
      <div class="dm-dispatch-hero-label">当前值班优先级</div>
      <div class="dm-dispatch-hero-title">{{ dispatchPriority.title }}</div>
      <div class="dm-dispatch-hero-sub">{{ dispatchPriority.sub }}</div>
      <div class="dm-dispatch-hero-pills">
        <span class="dm-dispatch-hero-pill tone-danger">高危待处理 {{ kpiUnhandledHigh }}</span>
        <span class="dm-dispatch-hero-pill tone-warning">禁止入井 {{ preShiftData.failedCount || 0 }}</span>
      </div>
    </div>
    <div class="dm-dispatch-actions">
      <button
        v-for="action in dispatchActionItems"
        :key="action.label"
        :class="['dm-dispatch-action', `tone-${action.tone}`]"
        @click="$emit('navigate', action.path)"
      >
        <span class="dm-dispatch-action-label">{{ action.label }}</span>
        <span class="dm-dispatch-action-value">{{ action.value }}</span>
        <span class="dm-dispatch-action-sub">{{ action.sub }}</span>
        <span class="dm-dispatch-action-link">进入处置</span>
      </button>
    </div>

    <div class="dm-dispatch-grid">
      <div class="dm-dispatch-card">
        <div class="dm-dispatch-label">高危待处理</div>
        <div class="dm-dispatch-value danger">{{ kpiUnhandledHigh }}</div>
        <div class="dm-dispatch-desc">优先进入待处理列表，完成高危预警闭环。</div>
        <button class="dm-dispatch-btn" @click="$emit('navigate', '/alert-management/notifications')">进入待处理</button>
      </div>
      <div class="dm-dispatch-card">
        <div class="dm-dispatch-label">禁止入井</div>
        <div class="dm-dispatch-value warn">{{ preShiftData.failedCount || 0 }}</div>
        <div class="dm-dispatch-desc">班前健康筛查未通过人员，需优先复核准入原因。</div>
        <button class="dm-dispatch-btn" @click="$emit('navigate', '/health-monitor/mine-entry')">查看准入</button>
      </div>
      <div class="dm-dispatch-card dm-dispatch-card--list">
        <div class="dm-dispatch-head">
          <span class="dm-dispatch-label">重点人员</span>
          <span class="dm-dispatch-mini">{{ focusWarningEvents.length }} 人</span>
        </div>
        <div v-if="focusWarningEvents.length" class="dm-dispatch-list">
          <button
            v-for="(item, idx) in focusWarningEvents"
            :key="item.id || idx"
            class="dm-dispatch-person"
            @click="$emit('person-click', item)"
          >
            <span class="dm-dispatch-person-name">{{ item.userName || '--' }}</span>
            <span class="dm-dispatch-person-type">{{ item.type || item.warningType || '预警' }}</span>
            <span class="dm-dispatch-person-val">{{ item.value || item.warningValue || '--' }}</span>
          </button>
        </div>
        <div v-else class="dm-dispatch-empty">当前没有待处理重点人员。</div>
      </div>
      <div class="dm-dispatch-card dm-dispatch-card--wide">
        <div class="dm-dispatch-head">
          <span class="dm-dispatch-label">AI 值班摘要</span>
          <button class="dm-dispatch-link" @click="$emit('toggle-ai')">
            {{ mineAiLoading ? '分析中…' : mineAiReport ? '查看全文' : '生成分析' }}
          </button>
        </div>
        <p class="dm-dispatch-ai-text">{{ dashboardAiSummary }}</p>
        <div class="dm-dispatch-tags">
          <span v-if="riskDeptList[0]" class="dm-dispatch-tag">重点部门：{{ riskDeptList[0].name }}</span>
          <span v-if="latestDangerEvent?.userName" class="dm-dispatch-tag">重点人员：{{ latestDangerEvent.userName }}</span>
          <span class="dm-dispatch-tag">班前未通过：{{ preShiftData.failedCount || 0 }} 人</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  dispatchPriority: { type: Object, required: true },
  dispatchActionItems: { type: Array, required: true },
  kpiUnhandledHigh: { type: Number, default: 0 },
  preShiftData: { type: Object, default: () => ({ failedCount: 0 }) },
  focusWarningEvents: { type: Array, default: () => [] },
  mineAiReport: { type: String, default: '' },
  mineAiLoading: { type: Boolean, default: false },
  dashboardAiSummary: { type: String, default: '' },
  riskDeptList: { type: Array, default: () => [] },
  latestDangerEvent: { type: Object, default: null }
})

defineEmits(['navigate', 'person-click', 'toggle-ai'])
</script>

<style scoped>
.dm-dispatch-rail {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 12px;
  padding: 10px 14px 14px;
}

.dm-dispatch-hero {
  --dispatch-tone: var(--accent-primary);
  position: relative;
  overflow: hidden;
  min-height: 156px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid color-mix(in srgb, var(--dispatch-tone) 28%, transparent);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--dispatch-tone) 12%, rgba(8, 15, 30, 0.84)), rgba(8, 15, 30, 0.86)),
    linear-gradient(135deg, rgba(255,255,255,0.02), transparent 45%);
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,0.08),
    0 18px 36px -24px color-mix(in srgb, var(--dispatch-tone) 36%, transparent);
}
.dm-dispatch-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 100% 0%, rgba(255,255,255,0.08), transparent 40%);
  pointer-events: none;
}
.dm-dispatch-hero.tone-danger { --dispatch-tone: var(--accent-danger); }
.dm-dispatch-hero.tone-warn { --dispatch-tone: var(--accent-warning); }
.dm-dispatch-hero.tone-accent { --dispatch-tone: var(--accent-primary); }
.dm-dispatch-hero.tone-calm { --dispatch-tone: var(--accent-success); }
.dm-dispatch-hero-label { position: relative; z-index: 1; font-size: 12px; color: rgba(139,166,200,0.82); }
.dm-dispatch-hero-title { position: relative; z-index: 1; margin-top: 10px; font-size: 24px; font-weight: 800; color: #eef7ff; line-height: 1.3; letter-spacing: -0.02em; }
.dm-dispatch-hero-sub { position: relative; z-index: 1; margin-top: 8px; font-size: 12px; line-height: 1.7; color: rgba(143,181,211,0.92); }
.dm-dispatch-hero-pills {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.dm-dispatch-hero-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
  color: #d8e5f5;
  font-size: 11px;
  font-weight: 700;
}
.dm-dispatch-hero-pill.tone-danger {
  color: var(--accent-danger);
  border-color: rgba(248, 113, 113, 0.24);
  background: rgba(248, 113, 113, 0.08);
}
.dm-dispatch-hero-pill.tone-warning {
  color: var(--accent-warning);
  border-color: rgba(245, 158, 11, 0.24);
  background: rgba(245, 158, 11, 0.08);
}

.dm-dispatch-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.dm-dispatch-action {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 156px;
  border: 1px solid rgba(0, 212, 255, 0.16);
  border-radius: 16px;
  padding: 14px 14px 12px;
  background: linear-gradient(180deg, rgba(255,255,255,0.04), rgba(255,255,255,0.02));
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}
.dm-dispatch-action:hover {
  background: rgba(0,212,255,0.08);
  border-color: rgba(0,212,255,0.3);
  transform: translateY(-1px);
  box-shadow: 0 16px 28px -24px rgba(62, 183, 255, 0.7);
}
.dm-dispatch-action.tone-danger { border-color: rgba(255,95,95,0.22); }
.dm-dispatch-action.tone-warn { border-color: rgba(255,210,0,0.2); }
.dm-dispatch-action.tone-muted { border-color: rgba(139,166,200,0.16); }
.dm-dispatch-action-label { font-size: 12px; color: rgba(139,166,200,0.82); }
.dm-dispatch-action-value { font-size: 24px; font-weight: 800; color: #eef7ff; line-height: 1.1; letter-spacing: -0.03em; }
.dm-dispatch-action-sub { font-size: 11px; line-height: 1.6; color: rgba(143,181,211,0.88); }
.dm-dispatch-action-link {
  margin-top: auto;
  color: rgba(183, 233, 255, 0.92);
  font-size: 11px;
  font-weight: 700;
}

.dm-dispatch-grid {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.dm-dispatch-card {
  min-height: 112px;
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 16px;
  padding: 14px;
  background: rgba(255,255,255,0.025);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.dm-dispatch-label { font-size: 12px; color: #8ba6c8; }
.dm-dispatch-value { font-size: 30px; line-height: 1; font-weight: 700; font-family: 'Consolas', monospace; }
.dm-dispatch-value.danger { color: #ff5f5f; }
.dm-dispatch-value.warn { color: #ffd200; }
.dm-dispatch-desc { flex: 1; color: rgba(139,166,200,0.78); font-size: 12px; line-height: 1.7; }
.dm-dispatch-btn,
.dm-dispatch-link {
  border: 1px solid rgba(0,212,255,0.22);
  background: rgba(0,212,255,0.08);
  color: #b7e9ff;
  border-radius: 10px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}
.dm-dispatch-btn:hover,
.dm-dispatch-link:hover { background: rgba(0,212,255,0.16); }
.dm-dispatch-head { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.dm-dispatch-mini { color: rgba(139,166,200,0.7); font-size: 11px; }
.dm-dispatch-list { display:flex; flex-direction:column; gap:8px; }
.dm-dispatch-person {
  display:flex;
  align-items:center;
  gap:8px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.06);
  background: rgba(255,255,255,0.02);
  cursor: pointer;
  text-align: left;
}
.dm-dispatch-person:hover { background: rgba(255,82,82,0.12); }
.dm-dispatch-person-name { font-size:12px; font-weight:700; color:#e8f4ff; }
.dm-dispatch-person-type,
.dm-dispatch-person-val { font-size:11px; color:#8fb5d3; }
.dm-dispatch-empty { color: rgba(139,166,200,0.74); font-size:12px; line-height:1.7; }
.dm-dispatch-ai-text { margin:0; color:#d5ebff; font-size:12px; line-height:1.8; }
.dm-dispatch-tags { display:flex; flex-wrap:wrap; gap:8px; }
.dm-dispatch-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #a9dfff;
  background: rgba(0,212,255,0.06);
  border: 1px solid rgba(0,212,255,0.14);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 11px;
}

@media (max-width: 1400px) {
  .dm-dispatch-rail {
    grid-template-columns: 1fr;
  }
  .dm-dispatch-actions {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .dm-dispatch-rail {
    gap: 10px;
    padding: 10px 10px 12px;
  }

  .dm-dispatch-actions,
  .dm-dispatch-grid {
    grid-template-columns: 1fr;
  }

  .dm-dispatch-hero,
  .dm-dispatch-action,
  .dm-dispatch-card {
    min-height: auto;
  }
}
</style>
