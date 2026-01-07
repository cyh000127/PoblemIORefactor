<template>
  <div class="quiz-result-container">
    <div class="container mx-auto px-4 max-w-4xl">
      <div v-if="!quizStore.quizResult" class="text-center py-8">
        <i class="pi pi-spin pi-spinner text-4xl"></i>
      </div>

      <div v-else class="flex flex-col gap-6">
        <!-- 최종 결과만 표시 -->
       <Card>
          <template #content>
            <div class="result-hero">
              <div class="confetti"></div>
              <div class="spark"></div>
              <div class="text-center flex flex-col gap-6">
                <div class="medal">
                  <i class="pi pi-trophy"></i>
                </div>
                <div>
                  <h2 class="headline">{{ resultMessage }}</h2>
                  <p class="score">{{ scorePercentage }}%</p>
                  <p class="subtext">
                    총 <span class="font-bold">{{ totalQuestions }}</span>문제 중
                    <span class="font-bold">{{ correctCount }}</span>문제 정답
                    ({{ answeredCount }}문제 풀이 완료)
                  </p>
                </div>

                <!-- 세부 스탯을 하나의 섹션으로 묶음 -->
                <div class="stat-strip">
                  <div class="stat-chip">
                    <p class="stat-label">총 문제</p>
                    <p class="stat-value">{{ totalQuestions }}</p>
                  </div>
                  <div class="stat-chip">
                    <p class="stat-label">정답</p>
                    <p class="stat-value text-main">{{ correctCount }}</p>
                  </div>
                  <div class="stat-chip">
                    <p class="stat-label">풀이 완료</p>
                    <p class="stat-value text-main">{{ answeredCount }}</p>
                  </div>
                </div>

                <div class="action-row">
                  <Button
                    label="제작자의 다른 문제 풀기"
                    icon="pi pi-arrow-right"
                    size="large"
                    :disabled="!authorId"
                    @click="goAuthorQuizzes"
                  />
                  <Button
                    label="그만 풀기"
                    icon="pi pi-home"
                    severity="secondary"
                    outlined
                    size="large"
                    @click="goToHome"
                  />
                </div>
              </div>
            </div>
          </template>
        </Card>

        <!-- 댓글 섹션 (결과 화면에서 노출) -->
        <Card>
          <template #content>
            <div class="flex justify-between items-center mb-3">
              <h3 class="text-xl font-bold m-0">댓글</h3>
              <span class="text-sm text-color-secondary"
                >결과를 공유하고 피드백을 남겨보세요</span
              >
            </div>
            <CommentSection :quiz-id="quizId" />
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useQuizStore } from '@/stores/quiz'
import Button from 'primevue/button'
import Card from 'primevue/card'
import CommentSection from '@/components/comment/CommentSection.vue'

const quizStore = useQuizStore()
const router = useRouter()
const route = useRoute()
const quizId = computed(() => Number(route.params.id))

onMounted(() => {
  // 결과 없으면 플레이 화면으로 되돌리기
  if (!quizStore.quizResult) {
    router.push(`/quiz/${route.params.id}/play`)
  }
})

// ===== 파생 데이터 (Derived Data) =====
const totalQuestions = computed(
  () => quizStore.quizResult?.totalQuestions || 0,
)
const correctCount = computed(
  () => quizStore.quizResult?.correctCount || 0,
)
const answeredCount = computed(
  () => quizStore.quizResult?.answeredCount || 0,
)

const scorePercentage = computed(() =>
  totalQuestions.value
    ? Math.round((correctCount.value / totalQuestions.value) * 100)
    : 0,
)

// 제작자 id (없을 수 있으니 optional)
const authorId = computed(
  () => quizStore.currentQuiz?.author?.id ?? null,
)

const resultMessage = computed(() => {
  const score = scorePercentage.value
  if (score === 100) return 'Perfect Score!'
  if (score >= 80) return 'Excellent!'
  if (score >= 60) return 'Good Job!'
  return 'Keep Practicing!'
})

// ===== 액션 (Actions) =====
const goAuthorQuizzes = () => {
  if (!authorId.value) return
  router.push(`/users/${authorId.value}`)
}

const goToHome = () => {
  quizStore.resetQuizPlay()
  router.push('/')
}
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
}

.quiz-result-container {
  min-height: calc(100vh - 200px);
}

.result-hero {
  position: relative;
  overflow: hidden;
  padding: 2rem 1.5rem 2.5rem;
  border-radius: 24px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
}

.medal {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  margin: 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 2px solid var(--text-main);
  box-shadow: none;
  color: var(--text-main);
  font-size: 3rem;
}

.headline {
  font-size: 2.4rem;
  font-weight: 800;
  margin: 0.25rem 0 0.5rem;
}

.score {
  font-size: 3.8rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
}

.subtext {
  font-size: 1.1rem;
  color: var(--text-color-secondary);
  margin: 0;
}

.stat-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.75rem;
  padding: 1rem;
  border-radius: 14px;
  background: var(--bg-surface-hover);
  border: 1px solid var(--border);
}

.stat-chip {
  padding: 0.75rem 0.5rem;
  border-radius: 12px;
  background: var(--bg-main);
  text-align: center;
}

.stat-label {
  margin: 0;
  color: var(--text-color-secondary);
  font-weight: 600;
  font-size: 0.9rem;
}

.stat-value {
  margin: 0.2rem 0 0;
  font-size: 1.5rem;
  font-weight: 800;
}

.action-row {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

@media (min-width: 640px) {
  .action-row {
    flex-direction: row;
    justify-content: center;
  }
  .score {
    font-size: 4.5rem;
  }
}

.confetti,
.spark {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.confetti::before,
.confetti::after {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, var(--text-main), transparent 60%);
  animation: float 10s ease-in-out infinite;
}

.confetti::before {
  top: -40px;
  left: -60px;
}

.confetti::after {
  bottom: -60px;
  right: -50px;
  animation-delay: 2s;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}
</style>
