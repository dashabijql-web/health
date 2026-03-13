/**
 * Shared ECharts configuration factories for health-monitor pages.
 * Keeps visual consistency and eliminates ~200 lines of duplication.
 */

/* ── colour tokens ── */
const C = {
  label:     '#8ba6c8',
  labelAlt:  '#a8c5e6',
  line:      'rgba(0,212,255,0.18)',
  lineLight: 'rgba(0,212,255,0.15)',
  split:     'rgba(0,212,255,0.07)',
  splitAlt:  'rgba(0,212,255,0.06)',
  tipBg:     'rgba(8,13,35,0.92)',
  tipBorder: 'rgba(0,212,255,0.25)',
  tipText:   '#e0eaf4',
  emptyText: '#8ba6c8'
}

/** Empty chart placeholder (暂无数据) */
export function emptyOption(text = '暂无数据', fontSize = 14) {
  return {
    backgroundColor: 'transparent',
    graphic: [{
      type: 'text', left: 'center', top: 'middle',
      style: { text, fill: C.emptyText, fontSize }
    }]
  }
}

/** Standard dark tooltip */
export function chartTooltip(formatter, trigger = 'axis') {
  return {
    trigger,
    backgroundColor: C.tipBg,
    borderColor: C.tipBorder,
    textStyle: { color: C.tipText, fontSize: 12 },
    ...(formatter ? { formatter } : {})
  }
}

/** Category axis (x or y) with line */
export function categoryAxis(data, opts = {}) {
  const { fontSize = 11, interval, labelColor = C.labelAlt, lineColor = C.line, show = true } = opts
  return {
    type: 'category',
    data,
    axisLine: show ? { lineStyle: { color: lineColor } } : { show: false },
    axisTick: { show: false },
    axisLabel: { color: labelColor, fontSize, ...(interval != null ? { interval } : {}) },
    ...opts.extra
  }
}

/** Value axis with hidden line + dashed split */
export function valueAxis(opts = {}) {
  const { fontSize = 10, name, splitColor = C.split, min, max } = opts
  return {
    type: 'value',
    ...(name ? { name, nameTextStyle: { color: C.label, fontSize } } : {}),
    axisLine: { show: false },
    axisTick: { show: false },
    splitLine: { lineStyle: { color: splitColor, type: 'dashed' } },
    axisLabel: { color: C.label, fontSize },
    ...(min != null ? { min } : {}),
    ...(max != null ? { max } : {})
  }
}

/** Horizontal bar grid (department stats) */
export function deptGrid() {
  return { left: '26%', right: '8%', top: '10%', bottom: '6%' }
}

/** Standard trend grid */
export function trendGrid() {
  return { left: '5%', right: '3%', top: '12%', bottom: '12%', containLabel: true }
}

/** Hourly / secondary chart grid */
export function hourlyGrid() {
  return { left: '8%', right: '2%', top: '14%', bottom: '16%', containLabel: true }
}

/** Age distribution grid */
export function ageGrid() {
  return { left: '10%', right: '4%', top: '16%', bottom: '16%' }
}

/** Stacked bar label (show only non-zero) */
export function barLabel(position = 'inside') {
  return {
    show: true, position, color: '#fff', fontSize: 10,
    formatter: p => p.value > 0 ? p.value : ''
  }
}
