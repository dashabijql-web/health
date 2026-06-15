import type { AlertItem, CockpitSnapshot, SignalRow } from '../data/cockpit-data'

function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function renderAlert(alert: AlertItem): string {
  return `
    <article class="alert-card is-${alert.severity}">
      <div class="alert-card__title">${escapeHtml(alert.title)}</div>
      <div class="alert-card__time">${escapeHtml(alert.time)}</div>
    </article>
  `
}

function renderSignal(signal: SignalRow): string {
  const polyline = signal.points
    .map((point, index) => `${index * 16},${66 - point}`)
    .join(' ')

  return `
    <div class="signal-row">
      <div class="signal-row__meta">
        <span>${escapeHtml(signal.label)}</span>
        <strong>${escapeHtml(signal.value)}</strong>
      </div>
      <svg class="signal-row__chart" viewBox="0 0 144 72" preserveAspectRatio="none" aria-hidden="true">
        <polyline points="${polyline}" />
      </svg>
    </div>
  `
}

function renderTopIcons(): string {
  const icons = ['sys', 'pulse', 'shield', 'orbit', 'scan']
  return `
    <div class="cockpit-top-icons" aria-label="System Controls">
      ${icons
        .map(
          (icon) => `
            <button type="button" class="top-icon top-icon--${icon}" aria-label="${icon}">
              <span class="top-icon__frame"></span>
              <span class="top-icon__glyph"></span>
            </button>
          `
        )
        .join('')}
    </div>
  `
}

function renderDesktopFidelityOverlay(): string {
  return `
    <div class="desktop-fidelity-overlay" aria-hidden="true">
      <div class="desktop-fidelity-overlay__grid"></div>
      <div class="desktop-fidelity-overlay__scan"></div>
      <div class="desktop-fidelity-overlay__pulse desktop-fidelity-overlay__pulse--core"></div>
      <div class="desktop-fidelity-overlay__pulse desktop-fidelity-overlay__pulse--halo"></div>
      <div class="desktop-fidelity-overlay__shimmer desktop-fidelity-overlay__shimmer--alerts"></div>
      <div class="desktop-fidelity-overlay__shimmer desktop-fidelity-overlay__shimmer--signals"></div>
      <div class="desktop-fidelity-overlay__orbit desktop-fidelity-overlay__orbit--a"></div>
      <div class="desktop-fidelity-overlay__orbit desktop-fidelity-overlay__orbit--b"></div>
      <div class="desktop-fidelity-controls">
        <div class="desktop-fidelity-controls__commands">
          <button type="button" class="desktop-fidelity-command" aria-label="全息视图"></button>
          <button type="button" class="desktop-fidelity-command" aria-label="系统诊断"></button>
          <button type="button" class="desktop-fidelity-command" aria-label="能力映射"></button>
          <button type="button" class="desktop-fidelity-command" aria-label="历史记录"></button>
        </div>
        <div class="desktop-fidelity-controls__nav">
          <button type="button" class="desktop-fidelity-nav" aria-label="Dashboard"></button>
          <button type="button" class="desktop-fidelity-nav" aria-label="Tactical"></button>
          <button type="button" class="desktop-fidelity-nav" aria-label="Intel"></button>
          <button type="button" class="desktop-fidelity-nav" aria-label="Command"></button>
          <button type="button" class="desktop-fidelity-nav" aria-label="System"></button>
          <button type="button" class="desktop-fidelity-nav" aria-label="Settings"></button>
        </div>
      </div>
    </div>
  `
}

function renderBottomModules(snapshot: CockpitSnapshot): string {
  return `
    <article class="bottom-module bottom-module--ship">
      <span>SHIP STATUS</span>
      <h3>舰船状态</h3>
      <div class="module-ship">
        <div class="module-ship__blueprint"></div>
      </div>
      <div class="bottom-module__legend bottom-module__legend--triple">
        <span><i></i>推进 100%</span>
        <span><i></i>护盾 98%</span>
        <span><i></i>武器 95%</span>
      </div>
    </article>
    <article class="bottom-module bottom-module--threat">
      <span>THREAT MATRIX</span>
      <h3>${escapeHtml(snapshot.bottomModules[0]?.title ?? '威胁态势')}</h3>
      <div class="module-radar">
        <div class="module-radar__ring module-radar__ring--1"></div>
        <div class="module-radar__ring module-radar__ring--2"></div>
        <div class="module-radar__ring module-radar__ring--3"></div>
        <span class="module-radar__dot module-radar__dot--a"></span>
        <span class="module-radar__dot module-radar__dot--b"></span>
        <span class="module-radar__dot module-radar__dot--c"></span>
        <span class="module-radar__dot module-radar__dot--d"></span>
      </div>
      <div class="bottom-module__metrics">
        <strong>HIGH</strong>
        <p>7 高危 / 12 中危 / 23 低危</p>
      </div>
    </article>
    <article class="bottom-module bottom-module--quantum">
      <span>QUANTUM CHANNELS</span>
      <h3>${escapeHtml(snapshot.bottomModules[1]?.title ?? '量子通道')}</h3>
      <div class="module-orbit">
        <div class="module-orbit__core"></div>
        <div class="module-orbit__ring module-orbit__ring--a"></div>
        <div class="module-orbit__ring module-orbit__ring--b"></div>
      </div>
      <div class="bottom-module__metrics">
        <strong>98.2%</strong>
        <p>稳定 / O-ALPHA / O-BETA / O-GAMMA</p>
      </div>
    </article>
    <article class="bottom-module bottom-module--energy">
      <span>ENERGY DISTRIBUTION</span>
      <h3>${escapeHtml(snapshot.bottomModules[2]?.title ?? '能量分布')}</h3>
      <div class="module-donut">
        <div class="module-donut__ring"></div>
        <div class="module-donut__center">
          <strong>2.48</strong>
          <em>TW</em>
        </div>
      </div>
      <div class="bottom-module__legend">
        <span><i></i>核心 42%</span>
        <span><i></i>护盾 28%</span>
        <span><i></i>推进 18%</span>
        <span><i></i>武器 7%</span>
      </div>
    </article>
    <article class="bottom-module bottom-module--recommend">
      <span>AI RECOMMENDATIONS</span>
      <h3>${escapeHtml(snapshot.bottomModules[3]?.title ?? 'AI 战术建议')}</h3>
      <div class="module-reco">
        ${snapshot.recommendationRows
          .map(
            (row, index) => `
              <div class="module-reco__row">
                <span class="module-reco__index">0${index + 1}</span>
                <p>${escapeHtml(row)}</p>
              </div>
            `
          )
          .join('')}
      </div>
    </article>
  `
}

export function renderHudShell(snapshot: CockpitSnapshot): string {
  const primaryAlerts = snapshot.alertFeed.slice(0, 3)
  const primarySignals = snapshot.signalRows.slice(0, 3)
  const primaryRecommendations = snapshot.recommendationRows.slice(0, 2)
  const leftStagePanels = snapshot.capabilityPanels.filter((panel) => panel.anchor === 'left').slice(0, 2)
  const rightStagePanels = snapshot.capabilityPanels.filter((panel) => panel.anchor === 'right').slice(0, 2)

  return `
    <div class="cockpit-shell">
      ${renderDesktopFidelityOverlay()}
      <header class="cockpit-header">
        <div class="cockpit-brand">
          <div class="cockpit-brand__title">${escapeHtml(snapshot.title)}</div>
          <div class="cockpit-brand__subtitle">${escapeHtml(snapshot.subtitle)}</div>
        </div>
        <div class="cockpit-header__banner">${escapeHtml(snapshot.titleBanner)}</div>
        <div class="cockpit-header__badges">
          ${snapshot.topBadges
            .map(
              (badge) => `
                <div class="header-badge">
                  <span>${escapeHtml(badge.label)}</span>
                  <strong>${escapeHtml(badge.value)}</strong>
                </div>
              `
            )
            .join('')}
        </div>
        ${renderTopIcons()}
      </header>

      <div class="cockpit-grid">
        <aside class="cockpit-rail cockpit-rail--left">
          <section class="hud-panel">
            <div class="hud-panel__title">核心状态</div>
            <div class="core-status-body">
              <div class="core-gauge">
                <div class="core-gauge__value">${snapshot.score}</div>
                <div class="core-gauge__label">总部评分</div>
              </div>
              <div class="metric-list">
                ${snapshot.sideMetrics
                  .map(
                    (metric) => `
                      <div class="metric-list__row">
                        <span>${escapeHtml(metric.label)}</span>
                        <strong>${escapeHtml(metric.value)}</strong>
                      </div>
                    `
                  )
                  .join('')}
              </div>
            </div>
          </section>
          <section class="hud-panel">
            <div class="hud-panel__title">任务概览</div>
            <div class="mission-card">
              <div class="mission-card__badge">MISSION OVERVIEW</div>
              <div class="mission-card__blueprint">
                <div class="mission-card__ship"></div>
                <div class="mission-card__scan"></div>
              </div>
              <div class="mission-card__footer">
                <div class="mission-card__value">96.3%</div>
                <div class="mission-card__copy">闭环稳定</div>
              </div>
            </div>
          </section>
          <section class="hud-panel">
            <div class="hud-panel__title">环境监测</div>
            <div class="env-grid">
              <span>舱内压力 101.3 kPa</span>
              <span>舱内温度 22.7 C</span>
            </div>
            <div class="ship-status-panel">
              <div class="ship-status-panel__label">SHIP STATUS</div>
              <div class="ship-status-panel__blueprint">
                <div class="ship-status-panel__craft"></div>
              </div>
              <div class="ship-status-panel__stats">
                <span>推进 100%</span>
                <span>护盾 98%</span>
                <span>武器 95%</span>
              </div>
            </div>
          </section>
        </aside>

        <main class="cockpit-stage-column">
          <div class="cockpit-stage">
            <div class="cockpit-stage__canvas" id="stage-canvas"></div>
            <div class="cockpit-stage__core">
              <div class="stage-halo stage-halo--outer"></div>
              <div class="stage-halo stage-halo--mid"></div>
              <div class="stage-halo stage-halo--inner"></div>
              <div class="stage-orbit stage-orbit--a"></div>
              <div class="stage-orbit stage-orbit--b"></div>
              <div class="stage-orbit stage-orbit--c"></div>
              <div class="holo-human-shell">
              <div class="holo-human-shell__beam"></div>
              <div class="holo-human-shell__grid"></div>
              <div class="holo-human-shell__node-grid">
                <span class="holo-human-shell__node holo-human-shell__node--a"></span>
                <span class="holo-human-shell__node holo-human-shell__node--b"></span>
                <span class="holo-human-shell__node holo-human-shell__node--c"></span>
                <span class="holo-human-shell__node holo-human-shell__node--d"></span>
                <span class="holo-human-shell__node holo-human-shell__node--e"></span>
              </div>
              <svg class="holo-human-shell__figure" viewBox="0 0 260 620" aria-hidden="true">
                  <defs>
                    <linearGradient id="holoStroke" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stop-color="#d8fbff" />
                      <stop offset="55%" stop-color="#74efff" />
                      <stop offset="100%" stop-color="#32bfff" />
                    </linearGradient>
                  </defs>
                  <circle cx="130" cy="72" r="34" class="holo-human__head" />
                  <path class="holo-human__torso" d="M130 116 C176 144 196 214 196 290 L182 454 C178 524 162 564 146 596" />
                  <path class="holo-human__torso" d="M130 116 C84 144 64 214 64 290 L78 454 C82 524 98 564 114 596" />
                  <path class="holo-human__center" d="M130 116 L130 596" />
                  <path class="holo-human__arm" d="M76 236 C42 276 34 330 42 392" />
                  <path class="holo-human__arm" d="M184 236 C218 276 226 330 218 392" />
                  <path class="holo-human__leg" d="M112 420 C104 486 96 542 82 604" />
                  <path class="holo-human__leg" d="M148 420 C156 486 164 542 178 604" />
                  <ellipse cx="130" cy="240" rx="56" ry="86" class="holo-human__field" />
                  <ellipse cx="130" cy="314" rx="72" ry="112" class="holo-human__field holo-human__field--outer" />
                </svg>
                <div class="holo-human-shell__pedestal">
                  <div class="holo-human-shell__pedestal-ring holo-human-shell__pedestal-ring--a"></div>
                  <div class="holo-human-shell__pedestal-ring holo-human-shell__pedestal-ring--b"></div>
                  <div class="holo-human-shell__pedestal-core"></div>
                </div>
              </div>
            </div>
            <div class="cockpit-stage__overlay cockpit-stage__overlay--left">
              ${leftStagePanels
                .map(
                  (panel) => `
                    <div class="capability-panel">
                      <span>${escapeHtml(panel.status)}</span>
                      <strong>${escapeHtml(panel.value)}</strong>
                      <em>${escapeHtml(panel.title)}</em>
                    </div>
                  `
                )
                .join('')}
            </div>
            <div class="cockpit-stage__overlay cockpit-stage__overlay--right">
              ${rightStagePanels
                .map(
                  (panel) => `
                    <div class="capability-panel">
                      <span>${escapeHtml(panel.status)}</span>
                      <strong>${escapeHtml(panel.value)}</strong>
                      <em>${escapeHtml(panel.title)}</em>
                    </div>
                  `
                )
                .join('')}
            </div>
            <div class="cockpit-stage__actions">
              ${snapshot.commandActions.map((label) => `<button type="button">${escapeHtml(label)}</button>`).join('')}
            </div>
          </div>
        </main>

        <aside class="cockpit-rail cockpit-rail--right">
          <section class="hud-panel">
            <div class="hud-panel__title">告警信息</div>
            <div class="alert-feed">
              ${primaryAlerts.map(renderAlert).join('')}
            </div>
            <div class="alert-feed__footer">
              <span>07 CRITICAL</span>
              <span>12 WARNING</span>
              <button type="button">VIEW ALL ALERTS</button>
            </div>
          </section>
          <section class="hud-panel">
            <div class="hud-panel__title">实时信号</div>
            <div class="signal-feed">
              ${primarySignals.map(renderSignal).join('')}
            </div>
            <div class="signal-feed__footer">
              <span>量子稳定性 97.1%</span>
              <strong>信号同步率 98.6%</strong>
            </div>
          </section>
          <section class="hud-panel">
            <div class="hud-panel__title">AI 战术建议</div>
            <div class="recommendation-list">
              ${primaryRecommendations.map((row) => `<div>${escapeHtml(row)}</div>`).join('')}
            </div>
          </section>
        </aside>
      </div>

      <section class="cockpit-bottom-band">
        ${renderBottomModules(snapshot)}
      </section>

      <nav class="cockpit-bottom-nav" aria-label="Command Console">
        <span>DASHBOARD</span>
        <span>TACTICAL</span>
        <span>INTEL</span>
        <span>COMMAND</span>
        <span>SYSTEM</span>
        <span>SETTINGS</span>
      </nav>
    </div>
  `
}
