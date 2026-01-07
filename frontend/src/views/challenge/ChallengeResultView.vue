<template>
  <div class="result-container">
    <div class="container mx-auto px-4">
      <div v-if="loading" class="text-center py-8">
        <i class="pi pi-spin pi-spinner text-4xl"></i>
      </div>

      <div v-else class="flex flex-col lg:flex-row justify-center gap-6 items-start relative">
        <!-- 메인 결과 카드 -->
        <div class="w-full max-w-3xl flex flex-col gap-6 text-center">
        <Card>
          <template #content>
            <div class="flex flex-col gap-6 items-center py-8">
              <i class="pi pi-trophy text-6xl text-yellow-500 mb-4"></i>
              
              <h2 class="text-3xl font-bold">결과</h2>
              
              <div class="flex flex-col gap-2">
                <div class="text-xl text-color-secondary">당신의 순위</div>
                <div class="text-5xl font-bold text-primary">{{ result.rank }}위</div>
              </div>
              
              <Divider />
              
              <div class="flex flex-col gap-2">
                <div class="text-xl text-color-secondary">정답 개수</div>
                <div class="text-4xl font-bold">{{ result.correctCount }} 문제</div>
              </div>
              
              <div v-if="result.playTime !== undefined && result.formattedTime" class="flex flex-col gap-2 mt-4">
                 <div class="text-lg text-color-secondary">소요 시간</div>
                 <div class="text-2xl font-mono">{{ result.formattedTime }}초</div>
              </div>
              
              <div class="flex gap-4 mt-6">
                <Button label="목록으로" icon="pi pi-list" outlined @click="goList" />
                <Button label="다시 도전" icon="pi pi-refresh" @click="retry" />
              </div>
            </div>
          </template>
        </Card>
      </div>

      <!-- 우측 컬럼: 랭킹 위젯 (고정 너비) -->
      <div class="w-full lg:w-[380px] shrink-0 hidden lg:block">
           <div class="sticky top-6">
               <ChallengeRankingWidget :challengeId="Number(challengeId)" />
           </div>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Divider from 'primevue/divider'
import { getChallengeResult } from '@/api/challenge'
import ChallengeRankingWidget from "@/components/challenge/ChallengeRankingWidget.vue"

const route = useRoute()
const router = useRouter()
const challengeId = route.params.id

const loading = ref(true)
const result = ref({
    rank: 0,
    formattedTime: '',
    correctCount: 0,
    playTime: 0
})

onMounted(async () => {
    try {
        const data = await getChallengeResult(challengeId)
        result.value = data
    } catch (error) {
        console.error(error)
        // 에러 처리 또는 리다이렉트
    } finally {
        loading.value = false
    }
})

const goList = () => {
    router.push('/challenges')
}

const retry = () => {
    router.push(`/challenges/${challengeId}`)
}
</script>

<style scoped>
.result-container {
  min-height: calc(100vh - 200px);
}
</style>
