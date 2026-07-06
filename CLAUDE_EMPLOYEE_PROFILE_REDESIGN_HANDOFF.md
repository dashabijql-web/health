# Claude Code Handoff: Employee Profile Redesign

## Goal

Redesign the employee health portrait page as a polished operational dashboard / data-screen page.

The user explicitly wants Claude Code to take over this frontend redesign work. Implement the redesign directly, run it locally, verify visually, and commit the finished changes.

## Project

- Repo: `/Users/jiangqianli/Documents/Codex/health-mac/health`
- Branch: `codex-frontend-beautification-20260522`
- Frontend: `HealthShow` (Vue 3 + Vite)
- Main page: `HealthShow/src/views/health-monitor/employee-profile/index.vue`
- Main styles: `HealthShow/src/views/health-monitor/employee-profile/employee-profile.scss`
- Related page logic:
  - `HealthShow/src/views/health-monitor/employee-profile/use-employee-profile-page.js`
  - `HealthShow/src/views/health-monitor/employee-profile/employee-profile-view-model.js`
  - `HealthShow/src/views/health-monitor/employee-profile/employee-profile-runtime.js`

## Current State

The page has been restored to the static center image version:

```vue
<img class="ep-miner" src="/assets/miner-worker.png" />
```

Relevant recent commits:

- `bac1509 Revert "feat: embed rotating health portrait video"`: restores the page after a failed video experiment.
- `0fc76eb feat: embed rotating health portrait video`: tried to embed the rotating human video, but the visual result was rejected.
- `e85b7d1 chore: add mac local runtime helpers`: Mac-native run scripts.

Do not repeat the rejected approach of simply embedding the video into the current center stage.

## Failed Attempt To Avoid

Video file previously tested:

`/Users/jiangqianli/Downloads/帮我生成_度旋转的视频，适合嵌入到数据可视化大屏里的那种.mp4`

Problem:

- Direct embedding produced obvious extra blue blocks above and below the human body.
- CSS cropping still felt forced and visually awkward.
- User rejected it and asked to restore.

If you use video or new media, design the container and composition around it first, then verify by screenshot. If the source asset does not suit the layout, do not use it.

## Design Direction

This is not a landing page. It is an operational health-monitoring dashboard.

Preserve current business behavior:

- AI report button/dialog
- Back button
- Refresh
- Summary metrics
- Basic employee information
- AI risk summary
- 7-day trends
- Realtime vitals
- Risk assessment
- Center human/health portrait area
- Hotspots and warning states

Improve:

- Overall visual hierarchy
- Center human composition and proportions
- Left/right side panel rhythm
- Card density and scanability
- Text fit, no overlaps
- No obvious empty blocks, hard crops, or decorative clutter
- Mature data-screen style, not marketing hero style

## Local Runtime

The user strongly prefers Mac-native execution where feasible.

Use Mac-native frontend/backend/Redis. Docker is acceptable for SQL Server only.

Existing helpers:

```bash
./tools/run-redis-mac.sh
./tools/run-backend-mac.sh
./tools/run-frontend-mac.sh
```

Typical URLs:

- Frontend: `http://localhost:9528/`
- Employee profile: `http://localhost:9528/#/health-monitor/employee-profile`
- Backend health: `http://localhost:8080/health/actuator/health`
- Login: `admin / admin123`

## Verification Required

Before finishing:

```bash
cd HealthShow
npm run build
```

Also verify in the browser:

- Open `http://localhost:9528/#/health-monitor/employee-profile`
- Login with `admin/admin123` if redirected
- Take a screenshot or visually inspect the desktop/wide layout
- Confirm no text overlap, no clipped controls, no awkward media blocks, and the center visual works with the surrounding panels

## Expected Delivery

Make a clear local commit after implementation, for example:

```bash
git add HealthShow/src/views/health-monitor/employee-profile
git commit -m "feat: redesign employee health portrait page"
```

In the final note, report:

- What changed
- Build result
- Browser verification result
- Commit hash
