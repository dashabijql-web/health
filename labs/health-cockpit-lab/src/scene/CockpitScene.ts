import * as THREE from 'three'
import { gsap } from 'gsap'
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js'

export class CockpitScene {
  private readonly container: HTMLElement
  private readonly scene = new THREE.Scene()
  private readonly camera = new THREE.PerspectiveCamera(34, 1, 0.1, 120)
  private readonly renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  private readonly composer: EffectComposer
  private readonly clock = new THREE.Clock()
  private readonly pointer = new THREE.Vector2()
  private readonly ringGroup = new THREE.Group()
  private readonly figureGroup = new THREE.Group()
  private readonly pulseGroup = new THREE.Group()
  private readonly starField = new THREE.Group()
  private animationFrameId = 0

  constructor(container: HTMLElement) {
    this.container = container
    this.scene.fog = new THREE.FogExp2(0x020814, 0.045)
    this.camera.position.set(0, 0.55, 10.8)

    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.6))
    this.renderer.outputColorSpace = THREE.SRGBColorSpace
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping
    this.renderer.toneMappingExposure = 1.05
    this.renderer.domElement.className = 'cockpit-stage__webgl'
    this.container.append(this.renderer.domElement)

    this.composer = new EffectComposer(this.renderer)
    this.composer.addPass(new RenderPass(this.scene, this.camera))
    this.composer.addPass(new UnrealBloomPass(new THREE.Vector2(1, 1), 1.55, 0.65, 0.15))

    this.createLights()
    this.createBackground()
    this.createStage()
    this.createFigure()
    this.createStars()
    this.bindPointer()
    this.resize()
    this.intro()
    this.render()
  }

  resize(): void {
    const width = Math.max(this.container.clientWidth, 1)
    const height = Math.max(this.container.clientHeight, 1)
    this.camera.aspect = width / height
    this.camera.updateProjectionMatrix()
    this.renderer.setSize(width, height, false)
    this.composer.setSize(width, height)
  }

  dispose(): void {
    cancelAnimationFrame(this.animationFrameId)
    this.container.removeEventListener('pointermove', this.handlePointerMove)
    this.container.removeEventListener('pointerleave', this.handlePointerLeave)

    this.scene.traverse((object: THREE.Object3D) => {
      const mesh = object as THREE.Mesh
      if (mesh.geometry) {
        mesh.geometry.dispose()
      }
      const material = mesh.material
      if (Array.isArray(material)) {
        material.forEach((item) => item.dispose())
      } else if (material) {
        material.dispose()
      }
    })

    this.renderer.dispose()
    this.container.innerHTML = ''
  }

  private readonly handlePointerMove = (event: PointerEvent) => {
    const bounds = this.container.getBoundingClientRect()
    const x = (event.clientX - bounds.left) / bounds.width
    const y = (event.clientY - bounds.top) / bounds.height
    this.pointer.set((x - 0.5) * 2, (0.5 - y) * 2)
  }

  private readonly handlePointerLeave = () => {
    this.pointer.set(0, 0)
  }

  private bindPointer(): void {
    this.container.addEventListener('pointermove', this.handlePointerMove)
    this.container.addEventListener('pointerleave', this.handlePointerLeave)
  }

  private createLights(): void {
    const ambient = new THREE.AmbientLight(0x55c8ff, 1.35)
    const top = new THREE.PointLight(0x7ce3ff, 32, 30, 2.1)
    top.position.set(0, 5, 4)
    const base = new THREE.PointLight(0x0eb6ff, 28, 28, 2.4)
    base.position.set(0, -3.6, 2)
    const rim = new THREE.DirectionalLight(0x8de5ff, 1.5)
    rim.position.set(-4, 2, 4)

    this.scene.add(ambient, top, base, rim)
  }

  private createBackground(): void {
    const grid = new THREE.GridHelper(18, 28, 0x1b90ff, 0x0f3859)
    grid.position.y = -4.4
    grid.material.transparent = true
    grid.material.opacity = 0.18
    this.scene.add(grid)

    const backPlane = new THREE.Mesh(
      new THREE.CircleGeometry(7.2, 128),
      new THREE.MeshBasicMaterial({
        color: 0x0a1830,
        transparent: true,
        opacity: 0.35
      })
    )
    backPlane.position.set(0, 0.3, -2.6)
    this.scene.add(backPlane)

    const haloMaterial = new THREE.LineBasicMaterial({
      color: 0x55d6ff,
      transparent: true,
      opacity: 0.2
    })
    ;[
      [6.4, 5.7, -2.4],
      [5.7, 5.0, -2.1],
      [4.9, 4.35, -1.8]
    ].forEach(([radiusX, radiusY, depth]) => {
      const curve = new THREE.EllipseCurve(0, 0, radiusX, radiusY, 0, Math.PI * 2, false, 0)
      const points = curve
        .getPoints(220)
        .map((point: THREE.Vector2) => new THREE.Vector3(point.x, point.y * 0.94, depth))
      const geometry = new THREE.BufferGeometry().setFromPoints(points)
      const line = new THREE.LineLoop(geometry, haloMaterial.clone())
      this.scene.add(line)
    })

    const frame = new THREE.Mesh(
      new THREE.TorusGeometry(7.1, 0.06, 18, 180),
      new THREE.MeshBasicMaterial({
        color: 0x40cfff,
        transparent: true,
        opacity: 0.24
      })
    )
    frame.rotation.x = Math.PI / 2
    frame.position.y = -2.55
    this.scene.add(frame)
  }

  private createStage(): void {
    const pedestal = new THREE.Group()

    const base = new THREE.Mesh(
      new THREE.CylinderGeometry(1.5, 2.2, 0.4, 80, 1, true),
      new THREE.MeshStandardMaterial({
        color: 0x07101f,
        emissive: 0x0ea9f6,
        emissiveIntensity: 0.22,
        metalness: 0.72,
        roughness: 0.35,
        transparent: true,
        opacity: 0.86
      })
    )
    base.position.y = -3.22
    pedestal.add(base)

    ;[1.6, 2.4, 3.2].forEach((radius, index) => {
      const ring = new THREE.Mesh(
        new THREE.TorusGeometry(radius, 0.035, 10, 160),
        new THREE.MeshBasicMaterial({
          color: index === 0 ? 0x9bf5ff : 0x24b6ff,
          transparent: true,
          opacity: 0.55 - index * 0.12
        })
      )
      ring.rotation.x = Math.PI / 2
      ring.position.y = -3.02 + index * 0.045
      pedestal.add(ring)
      this.pulseGroup.add(ring)
    })

    ;[0, 1, 2, 3].forEach((index) => {
      const ring = new THREE.Mesh(
        new THREE.TorusGeometry(2.05 + index * 0.95, 0.03, 8, 100),
        new THREE.MeshBasicMaterial({
          color: index % 2 === 0 ? 0x49d6ff : 0xffb547,
          transparent: true,
          opacity: index === 0 ? 0.65 : 0.22
        })
      )
      ring.rotation.x = Math.PI / 2
      ring.rotation.z = index * 0.42
      ring.position.y = -1.9 + index * 1.24
      this.ringGroup.add(ring)
    })

    const beam = new THREE.Mesh(
      new THREE.CylinderGeometry(0.95, 1.4, 7.4, 64, 1, true),
      new THREE.MeshBasicMaterial({
        color: 0x6ae8ff,
        transparent: true,
        opacity: 0.08,
        blending: THREE.AdditiveBlending,
        side: THREE.DoubleSide
      })
    )
    beam.position.y = -0.08
    this.pulseGroup.add(beam)

    const innerBeam = new THREE.Mesh(
      new THREE.CylinderGeometry(0.28, 0.42, 7.8, 32, 1, true),
      new THREE.MeshBasicMaterial({
        color: 0xe4fbff,
        transparent: true,
        opacity: 0.1,
        blending: THREE.AdditiveBlending,
        side: THREE.DoubleSide
      })
    )
    innerBeam.position.y = 0.1
    this.pulseGroup.add(innerBeam)

    this.scene.add(pedestal, this.ringGroup, this.pulseGroup)
  }

  private createFigure(): void {
    const texture = this.createFigureTexture()
    const material = new THREE.MeshBasicMaterial({
      map: texture,
      transparent: true,
      opacity: 0.86,
      blending: THREE.AdditiveBlending,
      side: THREE.DoubleSide,
      depthWrite: false
    })

    const planeA = new THREE.Mesh(new THREE.PlaneGeometry(3.1, 6.4), material)
    const planeB = planeA.clone()
    planeB.rotation.y = Math.PI / 5.2
    const planeC = planeA.clone()
    planeC.rotation.y = -Math.PI / 5.2

    ;[planeA, planeB, planeC].forEach((plane) => {
      plane.position.y = -0.05
      this.figureGroup.add(plane)
    })

    const glowPlane = new THREE.Mesh(
      new THREE.PlaneGeometry(3.7, 7.1),
      new THREE.MeshBasicMaterial({
        map: texture,
        transparent: true,
        opacity: 0.18,
        blending: THREE.AdditiveBlending,
        side: THREE.DoubleSide,
        depthWrite: false
      })
    )
    glowPlane.position.z = -0.16
    this.figureGroup.add(glowPlane)

    const spine = new THREE.Mesh(
      new THREE.CylinderGeometry(0.045, 0.045, 4.4, 8),
      new THREE.MeshBasicMaterial({
        color: 0xd8fbff,
        transparent: true,
        opacity: 0.45,
        blending: THREE.AdditiveBlending
      })
    )
    spine.position.y = 0.1
    this.figureGroup.add(spine)

    this.scene.add(this.figureGroup)
  }

  private createFigureTexture(): THREE.CanvasTexture {
    const canvas = document.createElement('canvas')
    canvas.width = 1024
    canvas.height = 2048
    const context = canvas.getContext('2d')

    if (!context) {
      throw new Error('Failed to create 2D context for hologram texture')
    }

    context.clearRect(0, 0, canvas.width, canvas.height)
    context.strokeStyle = 'rgba(133, 238, 255, 0.95)'
    context.fillStyle = 'rgba(116, 224, 255, 0.18)'
    context.lineWidth = 10
    context.shadowColor = 'rgba(129, 242, 255, 0.45)'
    context.shadowBlur = 24

    const midX = canvas.width / 2
    context.beginPath()
    context.arc(midX, 240, 104, 0, Math.PI * 2)
    context.stroke()

    context.beginPath()
    context.moveTo(midX, 350)
    context.bezierCurveTo(midX + 180, 420, midX + 190, 660, midX + 120, 860)
    context.bezierCurveTo(midX + 88, 960, midX + 92, 1150, midX + 98, 1400)
    context.stroke()

    context.beginPath()
    context.moveTo(midX, 350)
    context.bezierCurveTo(midX - 180, 420, midX - 190, 660, midX - 120, 860)
    context.bezierCurveTo(midX - 88, 960, midX - 92, 1150, midX - 98, 1400)
    context.stroke()

    context.beginPath()
    context.moveTo(midX - 108, 520)
    context.lineTo(midX - 270, 770)
    context.lineTo(midX - 310, 1110)
    context.stroke()

    context.beginPath()
    context.moveTo(midX + 108, 520)
    context.lineTo(midX + 270, 770)
    context.lineTo(midX + 310, 1110)
    context.stroke()

    context.beginPath()
    context.moveTo(midX - 90, 1410)
    context.lineTo(midX - 120, 1810)
    context.lineTo(midX - 72, 2000)
    context.stroke()

    context.beginPath()
    context.moveTo(midX + 90, 1410)
    context.lineTo(midX + 120, 1810)
    context.lineTo(midX + 72, 2000)
    context.stroke()

    context.lineWidth = 4
    context.strokeStyle = 'rgba(193, 251, 255, 0.42)'
    for (let y = 120; y < canvas.height; y += 40) {
      context.beginPath()
      context.moveTo(midX - 250, y)
      context.lineTo(midX + 250, y)
      context.stroke()
    }

    const glowGradient = context.createRadialGradient(midX, 980, 40, midX, 980, 360)
    glowGradient.addColorStop(0, 'rgba(255,255,255,0.42)')
    glowGradient.addColorStop(0.35, 'rgba(110,235,255,0.22)')
    glowGradient.addColorStop(1, 'rgba(110,235,255,0)')
    context.fillStyle = glowGradient
    context.beginPath()
    context.arc(midX, 980, 320, 0, Math.PI * 2)
    context.fill()

    for (let index = 0; index < 140; index += 1) {
      const x = midX + Math.sin(index * 0.61) * (110 + (index % 13) * 9)
      const y = 240 + index * 11.5
      context.fillStyle = index % 9 === 0 ? 'rgba(255, 187, 84, 0.75)' : 'rgba(154, 244, 255, 0.76)'
      context.beginPath()
      context.arc(x, y, index % 7 === 0 ? 5.5 : 3.2, 0, Math.PI * 2)
      context.fill()
    }

    const texture = new THREE.CanvasTexture(canvas)
    texture.colorSpace = THREE.SRGBColorSpace
    return texture
  }

  private createStars(): void {
    const starGeometry = new THREE.BufferGeometry()
    const positions = new Float32Array(1600 * 3)
    const colors = new Float32Array(1600 * 3)
    const color = new THREE.Color()

    for (let index = 0; index < 1600; index += 1) {
      const stride = index * 3
      const radius = 6 + Math.random() * 10
      const theta = Math.random() * Math.PI * 2
      const phi = Math.random() * Math.PI
      positions[stride] = Math.sin(phi) * Math.cos(theta) * radius
      positions[stride + 1] = (Math.random() - 0.5) * 10
      positions[stride + 2] = Math.cos(phi) * radius - 4
      color.set(index % 12 === 0 ? 0xffb347 : 0x6de9ff)
      colors[stride] = color.r
      colors[stride + 1] = color.g
      colors[stride + 2] = color.b
    }

    starGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    starGeometry.setAttribute('color', new THREE.BufferAttribute(colors, 3))

    const starMaterial = new THREE.PointsMaterial({
      size: 0.045,
      transparent: true,
      opacity: 0.85,
      vertexColors: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    })

    const stars = new THREE.Points(starGeometry, starMaterial)
    this.starField.add(stars)
    this.scene.add(this.starField)
  }

  private intro(): void {
    gsap.fromTo(
      this.figureGroup.scale,
      { x: 0.86, y: 0.86, z: 0.86 },
      { x: 1, y: 1, z: 1, duration: 1.4, ease: 'power3.out' }
    )
    gsap.fromTo(
      this.ringGroup.rotation,
      { z: -0.22 },
      { z: 0, duration: 1.2, ease: 'power3.out' }
    )
  }

  private render = () => {
    const elapsed = this.clock.getElapsedTime()
    this.animationFrameId = window.requestAnimationFrame(this.render)

    this.starField.rotation.y = elapsed * 0.03
    this.ringGroup.rotation.z = elapsed * 0.12
    this.ringGroup.children.forEach((ring: THREE.Object3D, index: number) => {
      ring.rotation.z += (index % 2 === 0 ? 1 : -1) * 0.0025
    })

    this.pulseGroup.children.forEach((child: THREE.Object3D, index: number) => {
      const material = (child as THREE.Mesh).material as THREE.MeshBasicMaterial
      material.opacity = 0.08 + Math.sin(elapsed * 1.6 + index) * 0.05 + index * 0.02
    })

    this.figureGroup.position.y = Math.sin(elapsed * 1.25) * 0.08
    this.figureGroup.rotation.y = this.pointer.x * 0.14
    this.figureGroup.rotation.x = this.pointer.y * 0.06
    this.camera.position.x = THREE.MathUtils.lerp(this.camera.position.x, this.pointer.x * 0.45, 0.06)
    this.camera.position.y = THREE.MathUtils.lerp(this.camera.position.y, 0.55 + this.pointer.y * 0.22, 0.06)
    this.camera.lookAt(0, 0.05, 0)

    this.composer.render()
  }
}
