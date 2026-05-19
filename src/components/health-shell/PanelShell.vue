<template>
  <section class="hm-panel-shell">
    <header class="hm-panel-shell__head">
      <div class="hm-panel-shell__lead">
        <span :class="['hm-panel-shell__accent', accentClass]"></span>
        <div class="hm-panel-shell__titles">
          <div class="hm-panel-shell__title-row">
            <h2 class="hm-panel-shell__title">{{ title }}</h2>
            <span v-if="badge" :class="['hm-status-chip', badgeToneClass]">{{ badge }}</span>
          </div>
          <p v-if="subtitle" class="hm-panel-shell__subtitle">{{ subtitle }}</p>
        </div>
      </div>
      <div v-if="$slots.actions" class="hm-panel-shell__actions">
        <slot name="actions" />
      </div>
    </header>
    <div :class="['hm-panel-shell__body', bodyClass]">
      <slot />
    </div>
  </section>
</template>

<script>
export default {
  name: 'PanelShell',
  props: {
    title: {
      type: String,
      required: true
    },
    subtitle: {
      type: String,
      default: ''
    },
    accent: {
      type: String,
      default: 'primary'
    },
    badge: {
      type: String,
      default: ''
    },
    badgeTone: {
      type: String,
      default: 'primary'
    },
    bodyClass: {
      type: String,
      default: ''
    }
  },
  computed: {
    accentClass() {
      return this.accent === 'primary' ? '' : `hm-panel-shell__accent--${this.accent}`
    },
    badgeToneClass() {
      return this.badgeTone === 'primary' ? '' : `hm-status-chip--${this.badgeTone}`
    }
  }
}
</script>
