export const riskWarningPageViewModel = {
  computed: {
    // FIX ②: 5个KPI含压力
    headerKpis() {
      const total = this.warningStats.reduce((s, x) => s + x.value, 0)
      return [
        { label: '今日总预警', val: total,                       cls: 'kpi-red'    },
        { label: '心率预警',   val: this.warningStats[0].value, cls: 'kpi-red'    },
        { label: '血氧预警',   val: this.warningStats[1].value, cls: 'kpi-orange' },
        { label: '体温预警',   val: this.warningStats[2].value, cls: 'kpi-cyan'   },
        { label: '压力预警',   val: this.warningStats[3].value, cls: 'kpi-purple' }
      ]
    },
    statTitle()  { return { day:'今日预警统计', week:'近7日预警统计', month:'近30日预警统计' }[this.activePeriod] },
    trendTitle() { return { day:'今日预警分布', week:'近7天预警趋势', month:'近30天预警趋势' }[this.activePeriod] },
    statMax() { return Math.max(1, ...this.warningStats.map(x => x.value)) },
    // FIX ⑤: 筛选
    filteredList() {
      return this.warningList.filter(item => {
        const nameOk  = !this.filterName  || (item.userName||'').includes(this.filterName)
        const levelOk = !this.filterLevel || item.warningLevel === this.filterLevel
        const typeOk  = !this.filterType  || item.warningType  === this.filterType
        return nameOk && levelOk && typeOk
      })
    },
    pagedList()   { const s=(this.currentPage-1)*this.pageSize; return this.filteredList.slice(s,s+this.pageSize) },
    totalPages()  { return Math.max(1, Math.ceil(this.filteredList.length/this.pageSize)) },
    warningTypes(){ return [...new Set(this.warningList.map(x=>x.warningType).filter(Boolean))] },
    pendingInFiltered() { return this.filteredList.filter(x => !x.handled) },
    allPendingSelected() { return this.pendingInFiltered.length > 0 && this.pendingInFiltered.every(x => this.selectedIds.includes(x.id)) },
    somePendingSelected() { return this.pendingInFiltered.some(x => this.selectedIds.includes(x.id)) },

    // 右侧面板：体征均值卡
    vitalAvg() {
      const total = this.warningList.length || 1
      const parse = v => parseFloat(v) || null
      const hrVals   = this.warningList.filter(x=>(x.warningType||'').includes('心率')).map(x=>parse(x.warningValue)).filter(v=>v!=null)
      const spo2Vals = this.warningList.filter(x=>(x.warningType||'').includes('血氧')).map(x=>parse(x.warningValue)).filter(v=>v!=null)
      const tempVals = this.warningList.filter(x=>(x.warningType||'').includes('体温')).map(x=>parse(x.warningValue)).filter(v=>v!=null)
      const avg = arr => arr.length ? (arr.reduce((s,v)=>s+v,0)/arr.length).toFixed(0) : '--'
      return [
        { label:'平均心率', val:hrVals.length   ? avg(hrVals)+'bpm'  : '--', color:'#ef4444', abnormal:hrVals.length,   rate:Math.round(hrVals.length/total*100)   },
        { label:'平均血氧', val:spo2Vals.length  ? avg(spo2Vals)+'%'  : '--', color:'#f97316', abnormal:spo2Vals.length, rate:Math.round(spo2Vals.length/total*100) },
        { label:'平均体温', val:tempVals.length  ? avg(tempVals)+'°C' : '--', color:'#22c55e', abnormal:tempVals.length, rate:Math.round(tempVals.length/total*100) }
      ]
    },

    // 右侧面板：处理进度
    handleProgress() {
      return ['心率','血氧','体温','压力'].map(key => {
        const matched = this.warningList.filter(x=>(x.warningType||'').includes(key))
        const handled = matched.filter(x=>x.handled||x.isHandled===1||x.isHandled===true)
        return { label:key+'预警', total:matched.length, handled:handled.length, rate:matched.length?Math.round(handled.length/matched.length*100):0 }
      })
    },
    handleProgressTotal() {
      const all = this.warningList.length; if(!all) return 0
      return Math.round(this.warningList.filter(x=>x.handled||x.isHandled===1||x.isHandled===true).length/all*100)
    },

  },
  watch: {
    filteredList() {
      this.$nextTick(() => { const el=this.$refs.listRef; if(el){ el.scrollTop=0; this.scrollTop=0 } })
    }
  },
}

