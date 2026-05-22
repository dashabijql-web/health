# HealthShow Frontend Beautification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `2026-05-22` frontend beautification master plan so `HealthShow` reads as one coherent occupational-health product across flagship pages, warning/report/AI pages, metric analysis pages, and admin CRUD pages.

**Architecture:** Keep the existing dark industrial shell, then consolidate styling through shared tokens and reusable shell patterns before touching individual pages. Favor broad visual wins from shared SCSS and shell components, then use focused template edits to remove emoji debt, improve empty states, and align headers, toolbars, tables, and action modules.

**Tech Stack:** Vue 3, Vite 5, SCSS, Element Plus, Playwright visual audits, Node built-in test runner

---

### Task 1: Lock shared visual rules and add regression coverage

**Files:**
- Create: `HealthShow/tests/frontend-beautification.mjs`
- Modify: `HealthShow/package.json`
- Modify: `HealthShow/src/styles/index.scss`
- Modify: `HealthShow/src/styles/health-monitor.scss`

- [ ] Add a regression test that fails while target views and shared interface components still contain banned emoji or temporary glyph shortcuts.
- [ ] Add a dedicated `npm` script so the beautification regression can be run in isolation and reused during verification.
- [ ] Tighten global tokens and shared health-monitor utility classes for hero shells, filter toolbars, empty states, action buttons, and admin page shells.
- [ ] Run: `node --test tests/frontend-beautification.mjs`
- [ ] Run: `npm run test:frontend-beautification`

### Task 2: Consolidate flagship shell pages

**Files:**
- Modify: `HealthShow/src/views/health-monitor/dashboard/index.vue`
- Modify: `HealthShow/src/views/health-monitor/dashboard/dashboard.scss`
- Modify: `HealthShow/src/views/safety-command/index.vue`
- Modify: `HealthShow/src/views/safety-command/safety-command-view-model.js`
- Modify: `HealthShow/src/views/safety-command/components/SafetyCommandDialogs.vue`
- Modify: `HealthShow/src/views/safety-command/components/RiskPersonPanel.vue`
- Modify: `HealthShow/src/views/safety-command/components/PersonDetailDrawer.vue`
- Modify: `HealthShow/src/views/safety-command/safety-command.scss`

- [ ] Replace command-center emoji actions with icon-driven or text-first controls while preserving current behavior.
- [ ] Tighten dashboard and safety-command hierarchy so top-level actions, metrics, and panels read as one command environment.
- [ ] Re-run the beautification regression test after emoji cleanup.

### Task 3: Refine people-centered flagship pages

**Files:**
- Modify: `HealthShow/src/views/health-monitor/workbench/index.vue`
- Modify: `HealthShow/src/views/health-monitor/workbench/workbench.scss`
- Modify: `HealthShow/src/views/health-monitor/employee-profile/index.vue`
- Modify: `HealthShow/src/views/health-monitor/employee-profile/employee-profile-view-model.js`
- Modify: `HealthShow/src/views/health-monitor/employee-profile/employee-profile.scss`
- Modify: `HealthShow/src/views/health-monitor/mine-entry/index.vue`
- Modify: `HealthShow/src/views/health-monitor/mine-entry/mine-entry.scss`
- Modify: `HealthShow/src/views/health-monitor/employee-archive/index.vue`
- Modify: `HealthShow/src/views/health-monitor/employee-archive/employee-archive.scss`
- Modify: `HealthShow/src/views/personnel-management/health-portrait/index.vue`
- Modify: `HealthShow/src/views/personnel-management/health-portrait/health-portrait.scss`

- [ ] Remove metric emoji and symbol shortcuts from workbench, employee-profile, mine-entry, and portrait-related views.
- [ ] Improve workbench calendar readability and mobile-safe spacing.
- [ ] Rework employee-profile side density and mine-entry empty-state presentation without sacrificing current business density.
- [ ] Promote archive and portrait pages onto the same shell language as the flagship people pages.

### Task 4: Unify warning workflow, reports, and AI surfaces

**Files:**
- Modify: `HealthShow/src/views/health-monitor/risk-warning/index.vue`
- Modify: `HealthShow/src/views/health-monitor/risk-warning/risk-warning.scss`
- Modify: `HealthShow/src/views/alert-management/notifications/index.vue`
- Modify: `HealthShow/src/views/alert-management/notifications/notifications.scss`
- Modify: `HealthShow/src/views/alert-management/records/index.vue`
- Modify: `HealthShow/src/views/alert-management/records/records.scss`
- Modify: `HealthShow/src/views/alert-management/config/index.vue`
- Modify: `HealthShow/src/views/alert-management/sos/index.vue`
- Modify: `HealthShow/src/views/health-monitor/report-center/index.vue`
- Modify: `HealthShow/src/views/health-monitor/report-center/report-center.scss`
- Modify: `HealthShow/src/views/ai-chat/index.vue`
- Modify: `HealthShow/src/views/ai-chat/ai-chat.scss`

- [ ] Standardize warning-center hero, filter, table/list shell, and severity language across all warning pages.
- [ ] Upgrade `sos`, `report-center`, and `ai-chat` from functional layouts into clearer workflow/product surfaces with deliberate empty and assistive states.
- [ ] Preserve existing route structure and warning lifecycle semantics while changing presentation.

### Task 5: Roll the design system through metric analysis and admin CRUD pages

**Files:**
- Modify: `HealthShow/src/views/health-monitor/real-time/index.vue`
- Modify: `HealthShow/src/views/health-monitor/real-time/realtime.scss`
- Modify: `HealthShow/src/views/health-monitor/heart-rate/index.vue`
- Modify: `HealthShow/src/views/health-monitor/heart-rate/heart-rate.scss`
- Modify: `HealthShow/src/views/health-monitor/pressure/index.vue`
- Modify: `HealthShow/src/views/health-monitor/pressure/pressure.scss`
- Modify: `HealthShow/src/views/health-monitor/blood-pressure/index.vue`
- Modify: `HealthShow/src/views/health-monitor/blood-pressure/blood-pressure.scss`
- Modify: `HealthShow/src/views/health-monitor/blood-oxygen/index.vue`
- Modify: `HealthShow/src/views/health-monitor/blood-oxygen/blood-oxygen.scss`
- Modify: `HealthShow/src/views/health-monitor/sleep/index.vue`
- Modify: `HealthShow/src/views/health-monitor/sleep/sleep.scss`
- Modify: `HealthShow/src/views/health-monitor/trend-warning/index.vue`
- Modify: `HealthShow/src/views/health-monitor/trend-warning/trend-warning.scss`
- Modify: `HealthShow/src/views/device-management/index.vue`
- Modify: `HealthShow/src/views/device-management/device-management.scss`
- Modify: `HealthShow/src/views/user-list/index.vue`
- Modify: `HealthShow/src/views/user-list/user-list.scss`
- Modify: `HealthShow/src/views/role-management/index.vue`
- Modify: `HealthShow/src/views/role-management/role-management.scss`
- Modify: `HealthShow/src/views/org-management/department/index.vue`
- Modify: `HealthShow/src/views/org-management/job-type/index.vue`

- [ ] Align metric-analysis pages around one analysis shell with consistent hero rhythm, chart sizing, and filter/tool list treatment.
- [ ] Introduce an admin-shell variant so CRUD pages feel related to the product without being forced into cockpit styling.
- [ ] Keep existing table logic, drawers, and dialogs intact while modernizing shells, headers, and filters.

### Task 6: Verify the implementation with evidence

**Files:**
- Review: `HealthShow/tests/visual/artifacts/*`
- Review: `HealthShow/tests/e2e/artifacts/*`

- [ ] Run: `npm run test:frontend-beautification`
- [ ] Run: `npm run build`
- [ ] Run: `npm run audit:structure`
- [ ] Run: `node scripts/with-env.mjs VISUAL_ROUTES=dashboard,workbench,employee-profile,mine-entry,alert-notifications,report-center,ai-chat,trend-warning -- npm run audit:visual`
- [ ] If the local stack is available, run: `npm run audit:e2e`
- [ ] Inspect the latest artifact summaries before claiming the work is complete.
