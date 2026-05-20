/**
 * Shared chart initialization helpers.
 * Eliminates repeated init/dispose boilerplate and common chart options.
 */
import * as echarts from '@/utils/echarts-setup'

/** Vertical gradient (top→bottom). Shorthand for echarts.graphic.LinearGradient. */
export function gradV(c1, c2) {
  return new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: c1 }, { offset: 1, color: c2 }])
}

/** Horizontal gradient (right→left). Shorthand for echarts.graphic.LinearGradient. */
export function gradH(c1, c2) {
  return new echarts.graphic.LinearGradient(1, 0, 0, 0, [{ offset: 0, color: c1 }, { offset: 1, color: c2 }])
}

/**
 * Init or re-init an ECharts instance on a ref element.
 * Returns the chart instance, or null if refEl is missing.
 *
 * Usage:
 *   const c = initChart(this.charts, 'dist', this.$refs.distRef)
 *   if (c) c.setOption({ ... })
 */
export function initChart(charts, key, refEl) {
  if (!refEl) return null
  if (charts[key]) charts[key].dispose()
  const rect = refEl.getBoundingClientRect()
  const style = window.getComputedStyle(refEl)
  const width = rect.width || parseFloat(style.width)
  const height = rect.height || parseFloat(style.height)
  if (!width || !height) return null
  const c = echarts.init(refEl, null, { width, height })
  charts[key] = c
  return c
}

/**
 * Donut/pie distribution chart option.
 * Used by heart-rate, blood-oxygen, blood-pressure, pressure pages.
 * @param {Array} data - [{ name, value, color }]
 */
export function distOption(data) {
  return {
    backgroundColor: 'transparent',
    series: [{
      type: 'pie', radius: ['52%', '80%'], center: ['50%', '50%'],
      label: { show: false }, labelLine: { show: false },
      data: (data && data.length)
        ? data.map(x => ({
            value: x.value, name: x.name,
            itemStyle: { color: x.color, borderRadius: 4, shadowColor: x.color + '66', shadowBlur: 10 }
          }))
        : [{ name: '暂无数据', value: 1, itemStyle: { color: '#1e3a5f' } }]
    }]
  }
}

/**
 * Gauge chart option.
 * Used by heart-rate, blood-oxygen, pressure pages.
 * @param {number} value - current gauge value
 * @param {object} cfg - { min, max, colors, pointer, width }
 *   colors: [[ratio, color], ...] for axisLine
 *   pointer: accent color string (default '#00d4ff')
 */
export function gaugeOption(value, cfg = {}) {
  const {
    min = 0, max = 100,
    colors = [[0.5, '#4FC3F7'], [0.75, '#52c41a'], [1, '#FFB84D']],
    pointer = '#00d4ff',
    width = 16
  } = cfg
  return {
    series: [{
      type: 'gauge', startAngle: 225, endAngle: -45,
      radius: '90%', center: ['50%', '58%'],
      min, max,
      axisLine: { lineStyle: { width, color: colors } },
      pointer: { length: '60%', width: 6, itemStyle: { color: pointer, shadowBlur: 14, shadowColor: pointer + 'cc' } },
      axisTick: { length: 5, distance: -22, lineStyle: { color: pointer + '40', width: 1 } },
      splitLine: { length: 10, distance: -22, lineStyle: { color: pointer + '72', width: 2 } },
      axisLabel: { color: '#8ba6c8', fontSize: 10, distance: -28 },
      detail: { show: false },
      data: [{ value }]
    }]
  }
}
