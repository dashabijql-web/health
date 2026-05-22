# HealthShow Frontend Beautification Master Plan

## Document Positioning

This document is the current master plan for `HealthShow` frontend beautification as of `2026-05-22`.

It is intended to unify and supersede the following earlier planning documents without deleting their historical value:

- `前端页面美化计划.md`
- `前端页面美化第二轮计划.md`
- `前端页面美化设计计划（Skill版）.md`
- `统一管控针对性美化计划.md`

Use this file as the primary execution and acceptance reference for upcoming UI beautification work unless a newer dated master plan replaces it.

## Why This Plan Exists

The current frontend is not "unstyled." It already has:

- a strong dark industrial-monitoring visual base
- stable route grouping and mobile bottom navigation
- shared shell components such as `PageHeroHeader` and `MetricStrip`
- a meaningful amount of visual and e2e audit coverage

The real problem is inconsistency, not absence:

- some pages feel like premium command-center cockpit screens
- some pages feel like functional but unfinished workbench pages
- some pages still look like old Element Plus admin CRUD screens
- some pages mix polished shell components with temporary icons, emoji, or page-local visual language

The goal is not to redesign the product from zero.
The goal is to turn the existing deep-blue occupational-health system into one coherent product language.

## Reference Skill Baseline

This plan uses the GitHub project [`Leonxlnx/taste-skill`](https://github.com/Leonxlnx/taste-skill) as the external design-method baseline.

Why this one:

- As of `2026-05-22`, the GitHub repository showed about `18.6k` stars.
- Its `design-taste-frontend` skill is explicitly aimed at avoiding generic, repetitive AI-looking frontend output.
- It emphasizes layout hierarchy, typography, motion restraint, density control, anti-emoji rules, and premium UI discipline rather than framework-specific novelty.
- The repository also ships a `redesign-existing-projects` style skill path, which matches this task better than a greenfield landing-page skill.

Relevant source facts from the repo:

- Repository: `Leonxlnx/taste-skill`
- Install name: `design-taste-frontend`
- Adjacent relevant install names:
  - `redesign-existing-projects`
  - `gpt-taste`
  - `high-end-visual-design`

Practical note for this repo:

- `HealthShow` is `Vue 3 + Vite + SCSS + Element Plus`
- therefore this plan adopts the design discipline from the skill, but not its React/Next/Tailwind implementation assumptions

Local skill mapping already available in this environment:

- `/home/j/code/AI/AI-SkillsMCP-WSL/skills/design-taste-frontend/SKILL.md`

## Evidence Base

This plan is based on current code facts plus recent validation artifacts.

### Code facts reviewed

- Routes:
  - `HealthShow/src/router/app-routes.mjs`
  - `HealthShow/src/router/health-monitor.mjs`
  - `HealthShow/src/router/alert-management.mjs`
- Layout and navigation:
  - `HealthShow/src/layout/index.vue`
  - `HealthShow/src/layout/menu/navigation.mjs`
- Global style foundation:
  - `HealthShow/src/styles/index.scss`
  - `HealthShow/src/styles/variables.scss`
- Shared shell components:
  - `HealthShow/src/components/health-shell/PageHeroHeader.vue`
  - `HealthShow/src/components/health-shell/MetricStrip.vue`
  - `HealthShow/src/components/WarningCenterNav.vue`
- Representative page implementations:
  - `dashboard`
  - `real-time`
  - `workbench`
  - `employee-archive`
  - `employee-profile`
  - `mine-entry`
  - `report-center`
  - `trend-warning`
  - `notifications`
  - `records`
  - `ai-chat`
  - `safety-command`

### Validation artifacts reviewed

- Route and navigation coverage:
  - `HealthShow/tests/e2e/artifacts/2026-05-19T03-31-29-018Z/summary.md`
- Recent visual coverage:
  - `HealthShow/tests/visual/artifacts/2026-05-20T03-49-45-501Z/layout-summary.md`
  - `HealthShow/tests/visual/artifacts/2026-05-20T07-04-24-052Z/layout-summary.md`
  - `HealthShow/tests/visual/artifacts/2026-05-20T07-09-07-255Z/layout-summary.md`
  - `HealthShow/tests/visual/artifacts/2026-05-20T15-18-55-565Z/layout-summary.md`
  - `HealthShow/tests/visual/artifacts/2026-05-20T15-28-18-762Z/layout-summary.md`

### Current-session limitation

The in-app browser is visibly open and usable by the user, but the Browser plugin control bridge in this current session has not yet reloaded after its marketplace fix. That means:

- I could inspect the current open page visually
- I could review code and existing screenshot evidence
- I could not yet do a fresh full-site browser-operated walkthrough in this exact session

Therefore this plan is honest about its evidence sources:

- code structure
- existing visual artifacts
- current visible in-app browser page

The plan is still actionable and complete enough to execute immediately.

## Current Route Inventory

### A. Command and Monitoring Core

- `/safety-command/index`
- `/health-monitor/dashboard`
- `/health-monitor/real-time`
- `/health-monitor/workbench`

### B. Metric Analysis

- `/health-monitor/heart-rate`
- `/health-monitor/pressure`
- `/health-monitor/blood-pressure`
- `/health-monitor/blood-oxygen`
- `/health-monitor/sleep`
- `/health-monitor/trend-warning`

### C. People and Portrait

- `/health-monitor/employee-archive`
- `/health-monitor/employee-profile`
- `/health-monitor/mine-entry`
- `/personnel-management/health-portrait` via hidden route use

### D. Warning Workflow

- `/health-monitor/risk-warning`
- `/alert-management/notifications`
- `/alert-management/records`
- `/alert-management/config`
- `/alert-management/sos`

### E. Reports and AI

- `/health-monitor/report-center`
- `/ai-chat/index`

### F. Admin CRUD

- `/admin/device-list`
- `/admin/user-list`
- `/admin/role`
- `/admin/department`
- `/admin/job-type`

### G. Login and Entry Redirects

- `/login`
- alias entry redirects such as `command-center`, `monitoring-center`, `warning-center`, `people-center`, `report-ai`

## High-Level Judgment

### What should be preserved

- Deep dark occupational-health monitoring base
- Blue-green safety-tech palette direction
- Shared shell idea already present in `PageHeroHeader`, `MetricStrip`, `WarningCenterNav`
- Relatively strong desktop cockpit identity in `dashboard` and `safety-command`
- High information density in monitoring flows, as long as hierarchy is improved
- Mobile bottom navigation concept and route grouping model

### What must be corrected

- Visual language fragmentation across pages
- Old Element-style CRUD screens not matching the main product shell
- Emoji and temporary glyph usage inside professional interfaces
- Mobile content collisions and density compression failures
- Chart containers that are too small to communicate meaning
- Over-boxing and over-paneling in some screens
- Underdesigned empty states in pages like `mine-entry`
- Underspecified AI/productivity feel in `ai-chat`

## Real Current Problems Observed

### 1. Theme fragmentation

The project already has a real token base in `src/styles/index.scss`, including:

- `--bg-*`
- `--border-*`
- `--accent-*`
- `--text-*`
- `--font-display`
- `--font-body`
- `--font-mono`
- `--radius-*`
- `--shadow-*`

But those tokens are not yet enforced across all pages consistently.

### 2. Emoji and temporary-symbol debt

Current code still includes visual shortcuts that reduce professionalism:

- `workbench` uses `❤`, `🩸`, `👟`, `⚡`
- `safety-command` still contains `📞`, `📢`, `🚨`
- `mine-entry` uses symbol-like inline markers and export label shortcuts
- `employee-profile` and supporting view-model code still use heart/blood emoji-like markers

For this product category, that is a direct quality downgrade.

### 3. Mobile overlap debt

Recent artifact evidence shows actual mobile issues, not hypothetical ones:

- `workbench mobile-390` previously failed due to bottom-nav text overlap
- `employee-profile mobile-390` previously failed due to text overlap
- key cockpit pages historically needed repeated visual fixes at narrow widths

This means mobile must be treated as a first-class beautification scope, not a final-pass afterthought.

### 4. Chart sizing debt

Artifact evidence from `2026-05-20T15-18-55-565Z` showed:

- `employee-profile` chart-like containers too small
- `trend-warning` dozens of chart-like containers too small at desktop widths

This is not merely a chart bug.
It indicates structural over-fragmentation and poor chart-to-card ratio.

### 5. Product maturity mismatch by page type

The pages do not fail in the same way:

- `dashboard` and `safety-command` are visually ambitious
- `report-center` is orderly but too plain
- `ai-chat` is too empty relative to the rest of the system
- `mine-entry` has a weak empty state and too much dead space
- CRUD admin pages are functional but not visually integrated into the same product

## Design North Star

The product should feel like:

- a professional occupational health command platform
- optimized for long-duration use
- serious and trustworthy
- data-dense without becoming cluttered
- modern but restrained

It should not feel like:

- a generic SaaS dashboard template
- a neon cyberpunk demo
- a toy AI interface
- a patchwork of command screen, admin backend, and unfinished workbench

## Design System Rules for This Repo

### 1. Palette

Keep one core accent family:

- Primary accent: blue-cyan
- Success: safety green
- Warning: amber-orange
- Danger: restrained red

Rules:

- no purple-primary page accents
- no mixed warm/cool gray systems
- no random local page glows
- use color semantically, not decoratively

### 2. Typography

Use the existing split more intentionally:

- `--font-display` for page titles, cockpit headings, critical numerics
- `--font-body` for UI copy
- `--font-mono` only for data strings, time, machine-like metrics, not entire pages

Required hierarchy:

- page title
- section title
- metric value
- metric label
- helper/secondary line

Common current failure:

- many pages rely on size alone, not spacing and weight, to establish hierarchy

### 3. Surfaces

Surface language should be standardized into three levels:

- Level 1: page background
- Level 2: major section shells
- Level 3: focused cards or action modules

Rules:

- not every data block should be a full bordered card
- use spacing and section grouping more often
- keep shadows soft and structural, not glowing
- keep borders subtle and consistent

### 4. Motion

Motion should be functional and calm:

- hover lift
- active press
- tab/toolbar transitions
- chart fade/slide reveal when useful

Avoid:

- theatrical perpetual animation in dense operator screens
- decorative motion that hurts scan speed
- multiple competing animated hotspots on the same screen

### 5. Icons

Move all pages to a unified icon strategy:

- `Element Plus` icons where appropriate
- project-local SVGs only when a domain-specific icon is required

Hard rule:

- no emoji in interface chrome, action buttons, metric labels, filters, cards, or status badges

### 6. Mobile rules

All mobile layouts must assume the persistent bottom nav exists.

That means:

- explicit bottom padding in long pages
- no content edge collision with nav labels
- hero area must compress gracefully
- filter controls must stack or scroll, not squeeze
- tables must convert to structured cards when needed

## Page Archetypes and Beautification Strategy

### A. Command Cockpit Pages

Pages:

- `dashboard`
- `safety-command`

Target:

- high-confidence command environment
- fast top-down scan
- one strong first-screen narrative

Current issues:

- both pages are visually heavy, but not differentiated enough in purpose
- some controls and labels still feel temporary
- `safety-command` still contains visually cheap emergency actions

Beautification direction:

- `dashboard`: "operational overview and decision summary"
- `safety-command`: "incident command and escalation console"
- reduce redundant panel framing
- improve first-screen focal order
- unify KPI density and decision panel styling

Priority:

- P0

Primary files:

- `src/views/health-monitor/dashboard/*`
- `src/views/safety-command/*`

### B. Monitoring Analysis Pages

Pages:

- `real-time`
- `heart-rate`
- `pressure`
- `blood-pressure`
- `blood-oxygen`
- `sleep`
- `trend-warning`

Target:

- consistent analysis-page template
- stable scan path: summary, filters, chart, list, detail

Current issues:

- pages likely vary too much in structure and chart framing
- `trend-warning` had excessive tiny chart containers
- `real-time` likely remains more functional than refined

Beautification direction:

- define one shared analysis page shell
- standardize filter bar treatment
- standardize chart-card ratio and minimum container size
- reduce card fragmentation in trend and metric pages

Priority:

- P1

Primary files:

- `src/views/health-monitor/real-time/*`
- `src/views/health-monitor/heart-rate/*`
- `src/views/health-monitor/pressure/*`
- `src/views/health-monitor/blood-pressure/*`
- `src/views/health-monitor/blood-oxygen/*`
- `src/views/health-monitor/sleep/*`
- `src/views/health-monitor/trend-warning/*`
- shared metric-page helpers

### C. People and Portrait Pages

Pages:

- `employee-archive`
- `employee-profile`
- `health-portrait`
- `workbench`
- `mine-entry`

Target:

- more human-centered storytelling
- less dashboard clutter
- better empty and detail states

Current issues:

- `employee-profile` has strong ambition but overpacked side information
- `workbench` had mobile overlap and still uses emoji metrics
- `mine-entry` empty state is structurally weak
- `employee-archive` card actions and card density can be more refined

Beautification direction:

- `employee-profile`: reduce side-fragmentation, strengthen primary narrative
- `workbench`: elevate calendar as hero module, simplify micro-symbol system
- `mine-entry`: make empty state explanatory and operational, not blank
- `employee-archive`: modernize archive cards and action hierarchy

Priority:

- P0 for `employee-profile`, `workbench`, `mine-entry`
- P1 for `employee-archive`, `health-portrait`

Primary files:

- `src/views/health-monitor/employee-archive/*`
- `src/views/health-monitor/employee-profile/*`
- `src/views/health-monitor/workbench/*`
- `src/views/health-monitor/mine-entry/*`
- `src/views/personnel-management/health-portrait/*`

### D. Warning Workflow Pages

Pages:

- `risk-warning`
- `notifications`
- `records`
- `config`
- `sos`

Target:

- one coherent warning-center subsystem
- stronger workflow rhythm
- unified state and severity language

Current issues:

- shell components exist, but not all pages feel like one subsystem
- `notifications` is already relatively mature
- `records` still mixes old panel/table phrasing with newer hero shell
- `sos` likely needs more incident-chain storytelling
- `config` risks feeling like a plain settings form

Beautification direction:

- keep `WarningCenterNav`
- unify top hero, stat cards, filter strips, list/table cards, and detail drawers
- upgrade `config` into a rule console feel
- make `sos` read like emergency workflow, not just a red list

Priority:

- P1

Primary files:

- `src/views/health-monitor/risk-warning/*`
- `src/views/alert-management/notifications/*`
- `src/views/alert-management/records/*`
- `src/views/alert-management/config/*`
- `src/views/alert-management/sos/*`
- `src/components/WarningCenterNav.vue`

### E. Reports and AI

Pages:

- `report-center`
- `ai-chat`

Target:

- report pages should feel analytical and authoritative
- AI pages should feel assistive and productive, not empty

Current issues:

- `report-center` is structurally solid but visually flatter than flagship pages
- `ai-chat` is the most obviously underdesigned page among visible product pages

Beautification direction:

- `report-center`: stronger overview band, more deliberate report identity
- `ai-chat`: build a proper AI-workbench feel with better conversation staging, suggestions, result framing, and empty states

Priority:

- P1

Primary files:

- `src/views/health-monitor/report-center/*`
- `src/views/ai-chat/*`

### F. Admin CRUD Pages

Pages:

- `device-list`
- `user-list`
- `role`
- `department`
- `job-type`

Target:

- visually integrated professional admin backend
- not forced into cockpit style
- still clearly part of the same product

Current issues:

- functionality-first Element layouts
- old admin visual language remains obvious
- toolbar, table, action-link, drawer, and dialog styles likely differ page by page

Beautification direction:

- define an admin-shell variant of the design system
- standardize:
  - search toolbar
  - table shell
  - header actions
  - dialog and drawer spacing
  - status chips

Priority:

- P2

Primary files:

- `src/views/device-management/index.vue`
- `src/views/user-list/index.vue`
- `src/views/role-management/index.vue`
- `src/views/org-management/department/index.vue`
- `src/views/org-management/job-type/index.vue`

## Full Page-by-Page Beautification Matrix

| Page | Current State | Main Problems | Target Direction | Priority |
| --- | --- | --- | --- | --- |
| `dashboard` | strong but visually crowded | focus competition, mixed component maturity | operational command overview | P0 |
| `safety-command` | visually strong but rough in places | emoji actions, uneven refinement | incident command console | P0 |
| `workbench` | useful and promising | emoji metrics, mobile overlap history | premium calendar workbench | P0 |
| `employee-profile` | flagship candidate | too dense, chart sizing history | narrative portrait center | P0 |
| `mine-entry` | structurally okay | weak empty state, dead space, mixed symbols | access-control workflow page | P0 |
| `real-time` | functional core page | table-heavy and likely visually plain | live monitoring console | P1 |
| `trend-warning` | dense and data-rich | too many tiny charts, readability | ranked trend-risk analysis page | P1 |
| `employee-archive` | useful card library | card action hierarchy, mixed polish | premium archive directory | P1 |
| `health-portrait` | data-rich | likely older styling and table focus | secondary portrait/report page | P1 |
| `risk-warning` | meaningful overview page | needs stronger subsystem unification | warning overview hub | P1 |
| `notifications` | relatively mature | can still align more with subsystem shell | operator queue page | P1 |
| `records` | mixed old/new language | panel/table shell mismatch | handled-record console | P1 |
| `config` | useful rules page | risks looking like a basic form page | threshold rule console | P1 |
| `sos` | emergency page | needs stronger action-chain storytelling | emergency operations list | P1 |
| `report-center` | orderly and clean | flatter than flagship pages | analytical report center | P1 |
| `ai-chat` | visibly underdesigned | too empty, weak assistive feel | AI copilot workbench | P1 |
| `heart-rate` | likely usable | needs template consistency | analysis page archetype | P2 |
| `pressure` | likely usable | needs template consistency | analysis page archetype | P2 |
| `blood-pressure` | likely visually inconsistent | old accent debt and style mismatch | analysis page archetype | P2 |
| `blood-oxygen` | likely usable | consistency and chart sizing discipline | analysis page archetype | P2 |
| `sleep` | hidden but should not rot | likely lower polish | analysis page archetype | P2 |
| `device-list` | functional | old admin style | integrated admin backend | P2 |
| `user-list` | functional | old admin style | integrated admin backend | P2 |
| `role` | functional | old admin style | integrated admin backend | P2 |
| `department` | functional | old admin style | integrated admin backend | P2 |
| `job-type` | functional | old admin style | integrated admin backend | P2 |
| `login` | not core scope this round | unknown current polish | align with product shell later | P3 |

## Execution Strategy

### Phase 0. Lock Foundations and Stop Regression

Goal:

- stop style drift before page-by-page beautification

Actions:

- declare this file as the current master plan
- remove new emoji additions going forward
- establish shared page-shell and admin-shell scope
- define visual acceptance rules per page archetype

Primary files:

- this document
- `src/styles/index.scss`
- `src/layout/*`
- `src/components/health-shell/*`

Exit criteria:

- no new page work proceeds without using shared shell direction

### Phase 1. Global Design System Consolidation

Goal:

- convert existing token base into enforced shared design language

Actions:

- tighten palette usage
- standardize heading rhythm
- standardize card/surface hierarchy
- standardize empty state and filter bar patterns
- remove temporary icons and symbol debt

Primary files:

- `src/styles/index.scss`
- `src/styles/health-monitor.scss`
- `src/styles/sidebar.scss`
- `src/layout/index.vue`
- `src/layout/components/*`
- `src/components/health-shell/*`
- `src/components/WarningCenterNav.vue`

Expected outputs:

- shared shell spec usable by all future page work

### Phase 2. Flagship Experience Pass

Goal:

- make the most visible pages feel premium and internally coherent

Pages:

- `dashboard`
- `safety-command`
- `employee-profile`
- `workbench`
- `mine-entry`

Why first:

- these pages define product perception fastest
- they expose both desktop and mobile weaknesses
- they contain the richest design debt and the highest payoff

### Phase 3. Warning Workflow Unification

Goal:

- make warning management behave and feel like one subsystem

Pages:

- `risk-warning`
- `notifications`
- `records`
- `config`
- `sos`

### Phase 4. Report and AI Productization

Goal:

- improve low-maturity pages with the highest visual gap

Pages:

- `report-center`
- `ai-chat`

### Phase 5. Monitoring Analysis Template Rollout

Goal:

- bring all metric-analysis pages under one unified structure

Pages:

- `real-time`
- `heart-rate`
- `pressure`
- `blood-pressure`
- `blood-oxygen`
- `sleep`
- `trend-warning`

### Phase 6. Admin Backend Integration

Goal:

- unify CRUD pages without forcing cockpit overstyling

Pages:

- `device-list`
- `user-list`
- `role`
- `department`
- `job-type`

## Concrete First-Batch Work Items

These should be executed first because they unlock most later work.

1. Remove emoji and temporary visual symbols from:
   - `workbench`
   - `safety-command`
   - `employee-profile`
   - `mine-entry`
   - related view-model helpers
2. Expand `PageHeroHeader` into the single default shell for:
   - monitoring pages
   - people pages
   - report/AI pages
3. Create a shared filter-toolbar pattern for:
   - `mine-entry`
   - `notifications`
   - `records`
   - admin CRUD pages
4. Create a shared empty-state pattern and apply it first to:
   - `mine-entry`
   - `ai-chat`
   - `employee-archive`
5. Rework `employee-profile` chart and side-panel layout to eliminate over-fragmentation.
6. Rework `trend-warning` card/chart sizing so microcharts are not visually meaningless.
7. Revisit `workbench` mobile layout with explicit bottom-nav-safe spacing and simpler cell microcontent.
8. Define an admin-shell table style and apply it first to:
   - `device-list`
   - `user-list`

## File Responsibility Map

### Global shell and tokens

- `HealthShow/src/styles/index.scss`
- `HealthShow/src/styles/health-monitor.scss`
- `HealthShow/src/layout/index.vue`
- `HealthShow/src/layout/menu/navigation.mjs`
- `HealthShow/src/components/health-shell/PageHeroHeader.vue`
- `HealthShow/src/components/health-shell/MetricStrip.vue`
- `HealthShow/src/components/WarningCenterNav.vue`

### Flagship pages

- `HealthShow/src/views/health-monitor/dashboard/*`
- `HealthShow/src/views/safety-command/*`
- `HealthShow/src/views/health-monitor/employee-profile/*`
- `HealthShow/src/views/health-monitor/workbench/*`
- `HealthShow/src/views/health-monitor/mine-entry/*`

### Workflow pages

- `HealthShow/src/views/health-monitor/risk-warning/*`
- `HealthShow/src/views/alert-management/notifications/*`
- `HealthShow/src/views/alert-management/records/*`
- `HealthShow/src/views/alert-management/config/*`
- `HealthShow/src/views/alert-management/sos/*`

### Report and AI pages

- `HealthShow/src/views/health-monitor/report-center/*`
- `HealthShow/src/views/ai-chat/*`

### Analysis pages

- `HealthShow/src/views/health-monitor/real-time/*`
- `HealthShow/src/views/health-monitor/heart-rate/*`
- `HealthShow/src/views/health-monitor/pressure/*`
- `HealthShow/src/views/health-monitor/blood-pressure/*`
- `HealthShow/src/views/health-monitor/blood-oxygen/*`
- `HealthShow/src/views/health-monitor/sleep/*`
- `HealthShow/src/views/health-monitor/trend-warning/*`

### Admin backend pages

- `HealthShow/src/views/device-management/index.vue`
- `HealthShow/src/views/user-list/index.vue`
- `HealthShow/src/views/role-management/index.vue`
- `HealthShow/src/views/org-management/department/index.vue`
- `HealthShow/src/views/org-management/job-type/index.vue`

## Validation Strategy

No beautification stage should be declared complete without fresh verification.

### Required baseline checks

- `npm run build`
- `npm run audit:structure`
- `npm run audit:e2e`

### Visual checks

Always use artifact-backed visual verification, not only manual eyeballing.

Full:

- `npm run audit:visual`

Route-specific:

- `node scripts/with-env.mjs VISUAL_ROUTES=dashboard -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=workbench -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=employee-profile -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=trend-warning -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=mine-entry -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=report-center -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=ai-chat -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=alert-notifications -- npm run audit:visual`

### Mobile acceptance gates

At minimum, the following must be visually rechecked on `mobile-390` and `mobile-414` after meaningful page changes:

- `dashboard`
- `real-time`
- `workbench`
- `employee-profile`
- `mine-entry`
- `risk-warning`
- `notifications`
- `records`
- `report-center`
- `ai-chat`

## Definition of Done

A beautified page is not "done" when it merely looks better in one screenshot.

It is done when all of the following are true:

- it clearly belongs to the same product as the rest of the system
- it preserves the page's business density and workflow usefulness
- desktop hierarchy is cleaner than before
- mobile layout does not collide with the bottom nav or hero content
- charts are large enough to communicate meaning
- empty, loading, and error states feel intentional
- no emoji or temporary UI shortcuts remain
- build and audit gates still pass

## Immediate Recommendation

If execution starts now, the first implementation sprint should focus on exactly this order:

1. global shell and token tightening
2. emoji and temporary-symbol cleanup
3. `dashboard`
4. `employee-profile`
5. `workbench`
6. `mine-entry`
7. `notifications`
8. `report-center`
9. `ai-chat`

That order maximizes visible quality gain while minimizing style fragmentation.

## References

- GitHub skill baseline:
  - <https://github.com/Leonxlnx/taste-skill>
- Internal skill mirror:
  - `/home/j/code/AI/AI-SkillsMCP-WSL/skills/design-taste-frontend/SKILL.md`
- Current route facts:
  - `HealthShow/src/router/app-routes.mjs`
  - `HealthShow/src/router/health-monitor.mjs`
  - `HealthShow/src/router/alert-management.mjs`
- Current shared shell:
  - `HealthShow/src/components/health-shell/*`
  - `HealthShow/src/components/WarningCenterNav.vue`
- Validation artifacts:
  - `HealthShow/tests/e2e/artifacts/2026-05-19T03-31-29-018Z/summary.md`
  - `HealthShow/tests/visual/artifacts/2026-05-20T03-49-45-501Z/layout-summary.md`
  - `HealthShow/tests/visual/artifacts/2026-05-20T07-04-24-052Z/layout-summary.md`
  - `HealthShow/tests/visual/artifacts/2026-05-20T07-09-07-255Z/layout-summary.md`
  - `HealthShow/tests/visual/artifacts/2026-05-20T15-18-55-565Z/layout-summary.md`
  - `HealthShow/tests/visual/artifacts/2026-05-20T15-28-18-762Z/layout-summary.md`
