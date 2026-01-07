<template>
  <div v-if="loading" :class="['grid gap-6', sidebarMode ? 'grid-cols-1' : 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3']">
    <div v-for="i in (sidebarMode ? 3 : 6)" :key="i" class="p-4 border rounded-xl bg-surface-card" style="border-color: var(--border)">
       <div class="flex items-center gap-4 mb-3">
         <Skeleton shape="circle" size="3rem" style="background-color: var(--skeleton-bg)"></Skeleton>
         <div class="flex-1">
           <Skeleton width="70%" height="1.2rem" class="mb-2" style="background-color: var(--skeleton-bg)"></Skeleton>
           <Skeleton width="40%" height="0.8rem" style="background-color: var(--skeleton-bg)"></Skeleton>
         </div>
       </div>
       <Skeleton width="100%" height="2rem" class="rounded-lg" style="background-color: var(--skeleton-bg)"></Skeleton>
    </div>
  </div>
  
  <div v-else-if="!challenges || challenges.length === 0" class="text-center py-8 text-color-secondary">
    <i class="pi pi-flag text-4xl mb-3 block"></i>
    진행 중인 챌린지가 없습니다.
  </div>

  <div v-else :class="['grid gap-6', sidebarMode ? 'grid-cols-1' : 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3']">
    <ChallengeCard 
      v-for="challenge in challenges" 
      :key="challenge.id" 
      :challenge="challenge"
      @click="onCardClick(challenge)"
    />
  </div>
</template>

<script setup lang="ts">
import ChallengeCard from './ChallengeCard.vue'
import Skeleton from "primevue/skeleton";

defineProps<{
  challenges: any[]
  loading: boolean
  sidebarMode?: boolean
}>()

const emit = defineEmits(['select'])

const onCardClick = (challenge: any) => {
  emit('select', challenge.id)
}
</script>
