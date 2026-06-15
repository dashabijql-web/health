export type ViewportMode = 'desktop' | 'compact' | 'mobile'

export function resolveViewportMode(width: number): ViewportMode {
  if (width >= 1680) {
    return 'desktop'
  }
  if (width >= 980) {
    return 'compact'
  }
  return 'mobile'
}
