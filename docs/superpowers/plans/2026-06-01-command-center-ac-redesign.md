# Command Center A+C Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild safety command as an incident-first field command table and dashboard as a duty-closure control system, while keeping backups of the prior pages.

**Architecture:** Preserve existing Vue data loaders and interaction handlers. Replace only page composition and SCSS layout for `safety-command` and `dashboard`, reusing the current API/view-model data so behavior remains stable.

**Tech Stack:** Vue 3, Element Plus, ECharts, Sass, existing `PageHeroHeader` and `MetricStrip` shell components.

---

### Task 1: Structure Contracts

**Files:**
- Modify: `HealthShow/tests/frontend-beautification.mjs`

- [ ] **Step 1: Write the failing tests**

Add tests that assert:
- backup files exist under `src/views/**/legacy-20260601/`
- safety command includes `.sc-war-room`, `.sc-incident-hero`, `.sc-situation-stage`, `.sc-response-queue`
- dashboard includes `.db-control-system`, `.db-duty-hero`, `.db-closure-lane`, `.db-governance-workspace`

- [ ] **Step 2: Run the test to verify it fails**

Run: `npm run test:frontend-beautification`

Expected: FAIL because backup files and new shell classes do not exist yet.

### Task 2: Back Up Prior Pages

**Files:**
- Create: `HealthShow/src/views/safety-command/legacy-20260601/index.vue`
- Create: `HealthShow/src/views/safety-command/legacy-20260601/safety-command.scss`
- Create: `HealthShow/src/views/health-monitor/dashboard/legacy-20260601/index.vue`
- Create: `HealthShow/src/views/health-monitor/dashboard/legacy-20260601/dashboard.scss`

- [ ] **Step 1: Copy current committed page files**

Copy the current working versions before redesign into the legacy directories.

- [ ] **Step 2: Re-run structure test**

Run: `npm run test:frontend-beautification`

Expected: still FAIL because new shell classes are not implemented yet.

### Task 3: Safety Command War Room

**Files:**
- Modify: `HealthShow/src/views/safety-command/index.vue`
- Modify: `HealthShow/src/views/safety-command/safety-command.scss`

- [ ] **Step 1: Recompose template**

Replace the five-column data wall with:
- incident hero
- primary situation stage
- response queue
- high-risk people lane
- compact telemetry strip
- supporting panels below the fold

- [ ] **Step 2: Add computed rows**

Add small computed collections for incident summary, action queue, stage nodes, and compact telemetry.

- [ ] **Step 3: Re-run structure test**

Run: `npm run test:frontend-beautification`

Expected: dashboard portion still FAILS, safety command checks pass.

### Task 4: Dashboard Control System

**Files:**
- Modify: `HealthShow/src/views/health-monitor/dashboard/index.vue`
- Modify: `HealthShow/src/views/health-monitor/dashboard/dashboard.scss`

- [ ] **Step 1: Recompose template**

Keep dashboard components but rebuild top-level page into:
- duty hero
- closure lane
- governance workspace
- health snapshot rail
- monitoring and support bands below

- [ ] **Step 2: Add dashboard computed helpers**

Add computed collections for duty metrics and closure states using existing `headerKpis`, `warningEvents`, `preShiftData`, `deviceCards`.

- [ ] **Step 3: Re-run structure test**

Run: `npm run test:frontend-beautification`

Expected: PASS.

### Task 5: Verification

**Files:**
- No new production files beyond tasks above.

- [ ] **Step 1: Run structure audit**

Run: `npm run audit:structure`

Expected: PASS.

- [ ] **Step 2: Run build**

Run: `npm run build`

Expected: PASS.

- [ ] **Step 3: Run visual audit for the two routes**

Run: `node scripts/with-env.mjs VISUAL_ROUTES=safety-command,dashboard -- npm run audit:visual`

Expected: PASS with 5 viewport screenshots per route and 0 issues.

- [ ] **Step 4: Browser smoke**

Open `/safety-command/index` and `/health-monitor/dashboard`, verify page titles are visible and console has 0 runtime errors.
