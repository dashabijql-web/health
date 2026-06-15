# Health Cockpit Lab Design

**Date:** 2026-05-28

## Goal

Create an independent flagship cockpit page that chases the supplied sci-fi control-room reference as closely as practical without modifying the existing `HealthShow` application. The output is a runnable standalone frontend project focused on one full-screen desktop-first page.

## Constraints

- Do not modify the existing `HealthShow` dashboard implementation.
- Keep all new work inside a separate project directory under this repository.
- Prioritize visual impact over real backend integration.
- Desktop is the primary target (`1920`, `1707`, `1440` widths).
- Mobile may degrade to a simplified single-column status layout.

## Architecture

The page uses two explicit layers:

1. `WebGL stage`
   - Renders the central hologram stage, scan rings, starfield, glow, beam and ambient depth.
   - Owns the animation loop, resize handling and post effects.

2. `HUD overlay`
   - Renders all text, alert rails, status boxes, bottom modules and chrome.
   - Uses HTML/CSS/SVG so typography and layout remain precise and maintainable.

This split keeps the visual center unconstrained while avoiding the cost of pushing all interface text and interaction into Canvas.

## Technology Choice

- `Vite`
- `TypeScript`
- `Three.js`
- `GSAP`
- Plain `HTML/CSS`
- Node built-in test runner with `tsx`

This avoids framework overhead in the render loop and keeps direct control over scene composition.

## Scene Composition

### Center stage

- Procedural hologram figure rendered as layered transparent planes using a generated canvas texture.
- Circular pedestal and multiple animated scan rings.
- Ambient particle field with slow drift.
- Central light beam with additive blending.
- Background arc geometry and depth grid.

### HUD

- Top chrome with product title and core-online banner.
- Left rail with core score, system integrity, mission overview, environment scan and ship status modules.
- Right rail with alert feed, real-time signal traces and recommendation stack.
- Bottom four-module band with threat matrix, quantum channels, energy distribution and AI recommendations.
- Bottom command bar for visual framing.

## Visual System

- Palette: cold cyan, desaturated steel blue, limited amber/orange for warning.
- Background: deep non-black metallic navy with layered vignette and panel borders.
- Typography: technical sans display, narrow spacing, monospace numerics.
- Motion: pulse, slow orbital drift, scan sweeps, ring rotation, restrained HUD reveal.

## Data Strategy

Phase 1 uses static and derived data only. The page is a visual lab, not a backend integration exercise. Data is organized behind typed snapshot builders so later integration can replace the source without rewriting the HUD.

## Delivery Scope

Phase 1 must deliver:

- Standalone project scaffold
- Runnable local dev server
- Production build
- Animated flagship cockpit page
- Basic regression tests for viewport mode, HUD generation and snapshot shaping

Phase 1 intentionally excludes:

- Login
- Real API binding
- Embedding back into the current HealthShow shell
- Asset pipelines requiring external model files
