import { gsap } from 'gsap'

import { buildCockpitSnapshot } from './data/cockpit-data'
import { resolveViewportMode } from './data/layout'
import { CockpitScene } from './scene/CockpitScene'
import './styles.css'
import { renderHudShell } from './ui/render-hud'

const root = document.querySelector<HTMLDivElement>('#app')

if (!root) {
  throw new Error('Missing #app mount node')
}

const snapshot = buildCockpitSnapshot()
root.innerHTML = renderHudShell(snapshot)

const stageContainer = root.querySelector<HTMLElement>('#stage-canvas')

if (!stageContainer) {
  throw new Error('Missing stage canvas container')
}

const updateViewportMode = () => {
  const mode = resolveViewportMode(window.innerWidth)
  document.documentElement.dataset.viewportMode = mode
}

updateViewportMode()
const scene = new CockpitScene(stageContainer)

gsap.from('.cockpit-header > *', {
  y: -22,
  opacity: 0,
  duration: 0.9,
  stagger: 0.12,
  ease: 'power3.out'
})

gsap.from('.cockpit-rail .hud-panel, .cockpit-bottom-band .bottom-module', {
  y: 26,
  opacity: 0,
  duration: 0.85,
  stagger: 0.08,
  ease: 'power3.out',
  delay: 0.18
})

gsap.from('.cockpit-stage__actions button, .cockpit-bottom-nav span', {
  y: 18,
  opacity: 0,
  duration: 0.7,
  stagger: 0.06,
  ease: 'power2.out',
  delay: 0.42
})

const handleResize = () => {
  updateViewportMode()
  scene.resize()
}

window.addEventListener('resize', handleResize)
window.addEventListener('beforeunload', () => {
  window.removeEventListener('resize', handleResize)
  scene.dispose()
})
