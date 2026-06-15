# Health Cockpit Lab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone desktop-first sci-fi health cockpit page in a new project directory without modifying the existing HealthShow app.

**Architecture:** Create a new Vite + TypeScript app that splits responsibilities between a Three.js WebGL stage and a DOM HUD overlay. Keep all data behind typed builder functions so visuals can ship first and integrations can be added later.

**Tech Stack:** Vite, TypeScript, Three.js, GSAP, CSS, Node built-in test runner, `tsx`

---

### Task 1: Scaffold the standalone app

**Files:**
- Create: `labs/health-cockpit-lab/package.json`
- Create: `labs/health-cockpit-lab/tsconfig.json`
- Create: `labs/health-cockpit-lab/vite.config.ts`
- Create: `labs/health-cockpit-lab/index.html`

- [ ] Define package scripts for dev, build and tests.
- [ ] Add runtime dependencies for the WebGL and animation stack.
- [ ] Add TypeScript and test-runner support.

### Task 2: Write failing tests for the app contracts

**Files:**
- Create: `labs/health-cockpit-lab/tests/cockpit-data.test.ts`
- Create: `labs/health-cockpit-lab/tests/layout.test.ts`
- Create: `labs/health-cockpit-lab/tests/hud-markup.test.ts`

- [ ] Write tests that define viewport behavior, static data shape and HUD markup expectations.
- [ ] Run the tests and confirm they fail before any implementation exists.

### Task 3: Implement data and HUD foundations

**Files:**
- Create: `labs/health-cockpit-lab/src/data/layout.ts`
- Create: `labs/health-cockpit-lab/src/data/cockpit-data.ts`
- Create: `labs/health-cockpit-lab/src/ui/render-hud.ts`

- [ ] Implement typed viewport-mode resolution.
- [ ] Implement the cockpit snapshot builder and static module data.
- [ ] Implement the HUD renderer that maps snapshot data into structured overlay markup.
- [ ] Re-run the tests and make them pass.

### Task 4: Implement the WebGL stage and shell

**Files:**
- Create: `labs/health-cockpit-lab/src/scene/CockpitScene.ts`
- Create: `labs/health-cockpit-lab/src/main.ts`
- Create: `labs/health-cockpit-lab/src/styles.css`

- [ ] Build the WebGL scene with hologram body, ring system, beam, particles and bloom.
- [ ] Mount the HUD and scene into one full-screen shell.
- [ ] Add resize and teardown handling.

### Task 5: Verify and polish

**Files:**
- Review: `labs/health-cockpit-lab/dist/*`

- [ ] Run: `npm test`
- [ ] Run: `npm run build`
- [ ] Launch the app locally and inspect the flagship page.
- [ ] Adjust layout and motion until the desktop presentation is coherent.
