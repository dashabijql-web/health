export type AlertSeverity = 'critical' | 'warning' | 'info'

export interface AlertItem {
  title: string
  time: string
  severity: AlertSeverity
}

export interface SignalRow {
  label: string
  value: string
  points: number[]
}

export interface CapabilityPanel {
  title: string
  value: string
  status: string
  anchor: 'left' | 'right'
}

export interface StatusMetric {
  label: string
  value: string
}

export interface BottomModule {
  title: string
  eyebrow: string
  value: string
  detail: string
}

export interface CockpitSnapshot {
  title: string
  subtitle: string
  score: number
  titleBanner: string
  topBadges: StatusMetric[]
  sideMetrics: StatusMetric[]
  alertFeed: AlertItem[]
  signalRows: SignalRow[]
  capabilityPanels: CapabilityPanel[]
  bottomModules: BottomModule[]
  recommendationRows: string[]
  commandActions: string[]
}

export function buildCockpitSnapshot(): CockpitSnapshot {
  return {
    title: '统一健康驾驶舱',
    subtitle: 'UNIFIED HEALTH COCKPIT',
    score: 92,
    titleBanner: 'AI CORE ONLINE',
    topBadges: [
      { label: 'SYS STATUS', value: 'NOMINAL' },
      { label: 'CREW', value: 'SYS-7' },
      { label: 'LINK', value: 'STABLE' }
    ],
    sideMetrics: [
      { label: '系统完整性', value: '98.7%' },
      { label: '能耗稳定性', value: '94.1%' },
      { label: '网络延迟', value: '12.7ms' },
      { label: '防御等级', value: 'A+' }
    ],
    alertFeed: [
      { title: '能量核心波动', time: '23:45', severity: 'critical' },
      { title: '外部信号干扰', time: '23:44', severity: 'warning' },
      { title: '防御屏障过载', time: '23:42', severity: 'warning' },
      { title: '引擎散热异常', time: '23:41', severity: 'warning' },
      { title: '通信链路波动', time: '23:40', severity: 'info' }
    ],
    signalRows: [
      { label: '脑态带宽', value: '67 Hz', points: [30, 45, 38, 52, 44, 63, 49, 54, 61, 53] },
      { label: '心率步序', value: '72 ms', points: [46, 42, 48, 44, 51, 45, 47, 40, 55, 49] },
      { label: '能量回路', value: '98 %', points: [52, 57, 49, 60, 62, 54, 51, 58, 64, 56] },
      { label: '神经同步', value: '0.92', points: [36, 34, 39, 43, 47, 45, 42, 46, 44, 48] },
      { label: '量子稳定性', value: '97.1 %', points: [61, 58, 62, 65, 63, 67, 64, 68, 66, 69] }
    ],
    capabilityPanels: [
      { title: '神经组网', value: '100%', status: 'NOMINAL LINK', anchor: 'left' },
      { title: '认知负载', value: '42%', status: 'COGNITIVE LOAD', anchor: 'left' },
      { title: '代谢效率', value: '89%', status: 'METABOLIC EFF.', anchor: 'left' },
      { title: '动作协同', value: '91%', status: 'MOTOR COORD.', anchor: 'right' },
      { title: '心智状态', value: 'STABLE', status: 'MENTAL STATE', anchor: 'right' },
      { title: '意识清醒', value: '97%', status: 'AWARENESS', anchor: 'right' }
    ],
    bottomModules: [
      { title: '威胁态势', eyebrow: 'THREAT MATRIX', value: 'HIGH', detail: '7 高危 / 12 中危 / 23 低危' },
      { title: '量子通道', eyebrow: 'QUANTUM CHANNELS', value: '98.2%', detail: '稳定 / O-ALPHA / O-BETA / O-GAMMA' },
      { title: '能量分布', eyebrow: 'ENERGY DISTRIBUTION', value: '2.48 TW', detail: '核心 42 / 护盾 28 / 推进 18 / 武器 7 / 辅助 5' },
      { title: 'AI 战术建议', eyebrow: 'AI RECOMMENDATIONS', value: '4 项', detail: '优先稳能分配 / 扫描干扰源 / 加强护盾 / 执行记录' }
    ],
    recommendationRows: [
      '优化能量分配',
      '增强外围屏障',
      '扫描未知信号源',
      '执行巡检记录'
    ],
    commandActions: ['全息视图', '系统诊断', '能力映射', '历史记录']
  }
}
