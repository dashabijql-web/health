let xlsxPromise
let html2canvasPromise
let jsPdfPromise
let markedPromise

export function getXLSX() {
  if (!xlsxPromise) {
    xlsxPromise = import('xlsx')
  }
  return xlsxPromise
}

export function getHtml2Canvas() {
  if (!html2canvasPromise) {
    html2canvasPromise = import('html2canvas').then(mod => mod.default || mod)
  }
  return html2canvasPromise
}

export function getJsPDF() {
  if (!jsPdfPromise) {
    jsPdfPromise = import('jspdf').then(mod => mod.default || mod.jsPDF || mod)
  }
  return jsPdfPromise
}

export function getMarked() {
  if (!markedPromise) {
    markedPromise = import('marked').then(mod => {
      const parser = mod.marked
      parser.setOptions({ breaks: true, gfm: true })
      return parser
    })
  }
  return markedPromise
}

export async function renderMarkdown(markdown) {
  if (!markdown) return ''
  const parser = await getMarked()
  return parser.parse(markdown)
}
