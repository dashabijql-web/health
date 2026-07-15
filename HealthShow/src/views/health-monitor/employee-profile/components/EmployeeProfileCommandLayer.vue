<template>
  <PersonDetailDrawer
    :visible="personVisible"
    :user-code="person.empCode || ''"
    :user-name="person.empName || ''"
    :dept-name="person.deptName || ''"
    :imei="person.imei || ''"
    :online="online"
    @update:visible="emit('update:personVisible', $event)"
    @emergency="emit('emergency', $event)"
  />

  <IncidentCommandDrawer
    :visible="incidentVisible"
    :event="incident"
    source-page="employee-profile"
    @update:visible="emit('update:incidentVisible', $event)"
    @updated="emit('updated')"
  />
</template>

<script setup>
import IncidentCommandDrawer from '@/views/safety-command/components/IncidentCommandDrawer.vue'
import PersonDetailDrawer from '@/views/safety-command/components/PersonDetailDrawer.vue'

defineProps({
  personVisible: { type: Boolean, default: false },
  incidentVisible: { type: Boolean, default: false },
  person: { type: Object, default: () => ({}) },
  incident: { type: Object, default: null },
  online: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:personVisible',
  'update:incidentVisible',
  'emergency',
  'updated'
])
</script>
