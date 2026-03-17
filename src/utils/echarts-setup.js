/**
 * ECharts 按需引入 — 只注册项目实际使用的图表类型和组件
 * 替代 `import * as echarts from 'echarts'`，减少打包体积
 *
 * 已注册：line / bar / pie / gauge / radar / treemap + 常用组件
 */
import { use, init, graphic, getInstanceByDom } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart, GaugeChart, RadarChart, TreemapChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  MarkLineComponent,
  MarkPointComponent,
  RadarComponent,
  TitleComponent,
  DataZoomComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GaugeChart,
  RadarChart,
  TreemapChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  MarkLineComponent,
  MarkPointComponent,
  RadarComponent,
  TitleComponent,
  DataZoomComponent
])

// 导出使用到的核心 API（命名导出，让文件通过 echarts.init / echarts.graphic 调用）
export { init, graphic, getInstanceByDom }
