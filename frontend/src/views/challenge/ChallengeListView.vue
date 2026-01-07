<template>
  <div class="container mx-auto px-4 py-8 max-w-6xl">
    <div class="mb-6">
      <h1 class="text-3xl font-bold mb-2">이벤트 챌린지</h1>
      <p class="text-color-secondary">한계를 시험하고 랭킹에 도전하세요!</p>
    </div>
    
    <ChallengeList :challenges="challenges" :loading="loading" @select="goDetail" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ChallengeList from '@/components/challenge/ChallengeList.vue'
import { getChallenges } from '@/api/challenge'

const router = useRouter()
const challenges = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    challenges.value = await getChallenges()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
})

const goDetail = (id) => {
  // 챌린지 상세 페이지로 이동
  router.push(`/challenges/${id}`)
}
</script>
