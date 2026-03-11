<template>
  <div class="ht-root">
    <div class="ht-header">
      <span class="ht-dot"></span>
      <span class="ht-title">健康小贴士</span>
      <span class="ht-date">{{ dateLabel }}</span>
    </div>
    <transition-group name="ht-slide" tag="ul" class="ht-list">
      <li v-for="tip in todayTips" :key="tip.id" class="ht-item">
        <span class="ht-emoji">{{ tip.emoji }}</span>
        <span class="ht-text">{{ tip.text }}</span>
      </li>
    </transition-group>
  </div>
</template>

<script>
import dayjs from 'dayjs'

const ALL_TIPS = [
  { id: 1,  emoji: '💧', cat: '水分',   text: '每天至少喝 1500–2000ml 水，分次少量补充，出汗多时适量补充电解质。' },
  { id: 2,  emoji: '🚶', cat: '运动',   text: '久坐超过 1 小时请起身活动 3–5 分钟，做肩颈后仰伸展，缓解肌肉紧张。' },
  { id: 3,  emoji: '❤️', cat: '心率',   text: '心率偏快时先放慢呼吸，避免咖啡因与情绪波动；持续异常请及时就医评估。' },
  { id: 4,  emoji: '🩸', cat: '血压',   text: '高血压家族史人群建议每周自测 2–3 次血压，同一时间段、坐位静息测量。' },
  { id: 5,  emoji: '😴', cat: '睡眠',   text: '保持 7–8 小时睡眠；固定作息、睡前少刷屏，卧室保持黑暗、安静、凉爽。' },
  { id: 6,  emoji: '🍎', cat: '饮食',   text: '每天 300–500g 蔬果，至少 3 种颜色，优先整果整蔬，减少果汁和糖分摄入。' },
  { id: 7,  emoji: '🧂', cat: '饮食',   text: '成人每日食盐不超过 5g；外卖重口味选少盐少酱料，养成看营养成分表的习惯。' },
  { id: 8,  emoji: '🧠', cat: '压力',   text: '压力大时试试 4-7-8 呼吸法：吸气4秒、屏气7秒、呼气8秒，循环 4 次。' },
  { id: 9,  emoji: '🧘', cat: '压力',   text: '午后疲劳时做 2 分钟闭眼冥想或身体扫描，有助于恢复专注与情绪稳定。' },
  { id: 10, emoji: '👀', cat: '用眼',   text: '用眼遵循 20-20-20 法则：每 20 分钟，看 20 英尺外 20 秒，减轻眼部疲劳。' },
  { id: 11, emoji: '🏃', cat: '运动',   text: '每周至少 150 分钟中等强度运动；走路、骑行与爬楼都是高性价比选择。' },
  { id: 12, emoji: '🧯', cat: '职业',   text: '在高温或粉尘环境工作注意补水与防护，必要时佩戴口罩与护目镜。' },
  { id: 13, emoji: '🥗', cat: '饮食',   text: '餐盘法：蔬菜占一半，蛋白质和主食各占四分之一，减少油炸高脂肪食物。' },
  { id: 14, emoji: '🫀', cat: '戒烟',   text: '吸烟伤心血管与肺部，尽量减少二手烟暴露；需要时寻求专业戒烟帮助。' },
  { id: 15, emoji: '🦷', cat: '口腔',   text: '每天认真刷牙两次各 2–3 分钟，使用牙线清理齿缝，半年到一年洗牙一次。' },
  { id: 16, emoji: '🌡️', cat: '体温',   text: '体温持续超过 37.5°C 超过两天，或伴随其他症状，请及时就医检查。' },
  { id: 17, emoji: '🩺', cat: '血压',   text: '收缩压超过 140 或舒张压超过 90 为高血压，需在医生指导下调整生活方式或用药。' },
  { id: 18, emoji: '🥤', cat: '饮食',   text: '含糖饮料每周不超过 1–2 次；优先白水、无糖茶或淡咖啡，避免能量饮料。' },
  { id: 19, emoji: '🧊', cat: '运动',   text: '运动前热身 5–10 分钟，结束后做静态拉伸，有助于降低受伤风险并加速恢复。' },
  { id: 20, emoji: '💤', cat: '睡眠',   text: '睡眠不足 6 小时连续超过 3 天，注意力与判断力明显下降，高危操作请格外小心。' }
]

export default {
  name: 'HealthTips',
  props: {
    count: { type: Number, default: 5 }
  },
  computed: {
    dateLabel() {
      return dayjs().format('MM月DD日 健康提示')
    },
    todayTips() {
      // 用日期作为随机种子，保证当天内容固定
      const seed = parseInt(dayjs().format('YYYYMMDD'))
      const shuffled = [...ALL_TIPS].sort((a, b) => {
        const ha = Math.sin(seed * a.id) * 10000
        const hb = Math.sin(seed * b.id) * 10000
        return (ha - Math.floor(ha)) - (hb - Math.floor(hb))
      })
      return shuffled.slice(0, this.count)
    }
  }
}
</script>

<style lang="scss" scoped>
.ht-root {
  background: rgba(10, 18, 48, 0.65);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;
  padding: 14px 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ht-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.ht-dot {
  width: 6px;
  height: 6px;
  background: #00d4ff;
  border-radius: 50%;
  box-shadow: 0 0 6px #00d4ff;
}

.ht-title {
  font-size: 14px;
  font-weight: bold;
  color: #e0f0ff;
  border-left: 3px solid #00d4ff;
  padding-left: 8px;
}

.ht-date {
  font-size: 11px;
  color: #8ba6c8;
  margin-left: auto;
}

.ht-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  overflow: hidden;
}

.ht-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: rgba(0, 40, 90, 0.35);
  border: 1px solid rgba(0, 212, 255, 0.1);
  border-radius: 6px;
  padding: 8px 12px;
  transition: border-color 0.2s, background 0.2s;

  &:hover {
    border-color: rgba(0, 212, 255, 0.3);
    background: rgba(0, 50, 110, 0.45);
  }
}

.ht-emoji {
  font-size: 18px;
  flex-shrink: 0;
  line-height: 1.4;
}

.ht-text {
  font-size: 12px;
  color: #a8c5e6;
  line-height: 1.6;
}

.ht-slide-enter-active,
.ht-slide-leave-active { transition: all 0.4s ease; }
.ht-slide-enter-from   { opacity: 0; transform: translateX(-12px); }
.ht-slide-leave-to     { opacity: 0; transform: translateX(12px); }
</style>
