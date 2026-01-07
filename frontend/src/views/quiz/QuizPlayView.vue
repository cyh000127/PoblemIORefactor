<template>
  <div class="quiz-play-container">
    <div class="container mx-auto px-4 max-w-4xl">
      <div v-if="!quizStore.currentQuiz" class="text-center py-8">
        <i class="pi pi-spin pi-spinner text-4xl"></i>
      </div>

      <div v-else class="flex flex-col gap-4 items-center">
        <!-- 상단 제목 + 진행도 (중앙 정렬) -->
        <div class="w-full max-w-2xl mb-1">
          <div class="flex justify-between items-center mb-1">
            <h2 class="text-2xl font-bold m-0 text-center w-full">
              {{ quizStore.currentQuiz.title }}
            </h2>
          </div>
          <div class="text-right mt-1 text-sm text-color-secondary">
            {{ quizStore.currentQuestionIndex + 1 }} / {{ quizStore.currentQuiz.questions.length }}
          </div>
        </div>

        <!-- 1. 문제 이미지 (분리됨 & 고정 크기) -->
        <div v-if="currentQuestion?.imageUrl" class="thumbnail-frame">
          <img
            :src="currentQuestion.imageUrl"
            :alt="currentQuestion.description"
            class="thumbnail-image"
          />
        </div>

        <!-- 2. 콘텐츠 카드 (질문 & 입력) -->
        <Card class="quiz-card w-full max-w-2xl">
          <template #content>
            <div class="flex flex-col h-full justify-between items-center text-center w-full">
              <!-- 1) 평소 문제 풀이 화면 -->
              <template v-if="!quizStore.showAnswerCard">
                <!-- 상단 콘텐츠: 설명 & 입력 -->
                <div class="flex-1 flex flex-col justify-center items-center w-full max-w-md gap-6">
                   <!-- 질문 설명 -->
                  <div v-if="currentQuestion?.description" class="text-xl font-medium leading-relaxed max-w-lg question-description">
                    {{ currentQuestion.description }}
                  </div>

                  <!-- 입력 -->
                  <div class="w-full">
                    <label class="block text-left text-sm font-bold mb-2 ml-1 text-color-secondary">정답 입력</label>
                    <InputText
                      ref="answerInput"
                      v-model="currentAnswer"
                      placeholder="정답을 입력하세요"
                      class="w-full text-center text-lg p-3"
                    />
                  </div>
                </div>

                <!-- 하단 액션: 버튼 -->
                <div class="w-full max-w-md mt-4">
                  <Button
                    label="제출하기"
                    icon="pi pi-check"
                    class="w-full font-bold py-3"
                    :disabled="!currentAnswer.trim() || submitting"
                    :loading="submitting"
                    @click="submitAnswer"
                  />
                </div>
              </template>

              <!-- 2) 한 문제에 대한 정답/오답 카드 -->
              <template v-else>
                <!-- 상단 콘텐츠: 결과 정보 -->
                <div class="flex-1 flex flex-col justify-center items-center w-full max-w-md gap-2">
                   <!-- 결과 상태 -->
                  <div
                    class="text-2xl font-extrabold"
                    :class="quizStore.lastAnswerResult.correct ? 'text-green-500' : 'text-red-500'"
                  >
                    {{ quizStore.lastAnswerResult.correct ? '정답! ' : '오답!' }}
                  </div>

                  <!-- 정답 문구 -->
                  <div
                    v-if="quizStore.lastAnswerResult.correctAnswers && quizStore.lastAnswerResult.correctAnswers.length"
                    class="text-lg bg-surface-100 p-3 rounded-xl w-full"
                  >
                    <div class="font-bold mb-1 text-color-secondary text-sm">정답</div>
                    <div class="font-bold text-xl text-primary">
                      {{ quizStore.lastAnswerResult.correctAnswers.join(', ') }}
                    </div>
                  </div>

                  <!-- 사용자가 입력한 답 -->
                  <div
                    v-if="quizStore.lastAnswerResult.userAnswer"
                    class="text-sm text-color-secondary"
                  >
                    내가 쓴 답: <span class="font-medium text-color-main">"{{ quizStore.lastAnswerResult.userAnswer }}"</span>
                  </div>
                </div>

                <!-- 하단 액션: 다음 버튼 -->
                <div class="w-full max-w-md mt-2">
                  <Button
                    v-if="!isLastQuestion"
                    label="다음 문제"
                    icon="pi pi-arrow-right"
                    class="w-full font-bold py-3"
                    @click="handleNextAfterAnswer"
                  />
                  <Button
                    v-else
                    label="결과 보기"
                    icon="pi pi-chart-bar"
                    class="w-full font-bold py-3 p-button-success"
                    @click="goToResult"
                  />
                </div>
              </template>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useQuizStore } from '@/stores/quiz'
import {
  submitQuiz as submitQuizAPI,
  getPlayContext,
} from '@/api/submission'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const quizStore = useQuizStore()

const currentAnswer = ref('')
const submitting = ref(false)

// InputText DOM 접근용 ref
const answerInput = ref<any>(null)

const currentQuestion = computed(() => {
  if (!quizStore.currentQuiz) return null
  return quizStore.currentQuiz.questions[quizStore.currentQuestionIndex]
})

const isLastQuestion = computed(() => {
  if (!quizStore.currentQuiz) return false
  return (
    quizStore.currentQuestionIndex ===
    quizStore.currentQuiz.questions.length - 1
  )
})

/**
 * 답 입력창에 포커스를 주는 헬퍼
 * - PrimeVue InputText는 컴포넌트일 수 있으니 $el 및 내부 input까지 처리
 */
const focusAnswerInput = () => {
  nextTick(() => {
    const target = answerInput.value?.$el ?? answerInput.value
    if (!target) return

    const inputEl =
      target instanceof HTMLInputElement
        ? target
        : target.querySelector?.('input')

    inputEl?.focus()
  })
}

/**
 * 엔터 키 입력 공통 처리
 * - 정답 카드가 아직 안 떠 있으면: 제출
 * - 정답 카드가 떠 있으면:
 *   - 마지막 문제가 아니면: 다음 문제
 *   - 마지막 문제면: 결과 페이지로
 */
const handleEnter = () => {
  if (!currentQuestion.value) return

  // 아직 정답 카드 안 뜬 상태 -> 제출 단계
  if (!quizStore.showAnswerCard) {
    if (!currentAnswer.value.trim() || submitting.value) {
      return
    }
    submitAnswer()
    return
  }

  // 정답 카드가 떠 있는 상태
  if (!isLastQuestion.value) {
    handleNextAfterAnswer()
  } else {
    goToResult()
  }
}

/**
 * 전역 keydown 핸들러 (엔터 키를 잡아서 handleEnter로 위임)
 */
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    e.preventDefault()
    handleEnter()
  }
}

/**
 * 한 문제에 대한 답안을 제출하고 → 서버에 채점 요청 → 정답/오답 카드 표시
 */
const submitAnswer = async () => {
  if (!currentQuestion.value || !currentAnswer.value.trim()) {
    toast.add({
      severity: 'warn',
      summary: 'Warning',
      detail: 'Please enter an answer',
      life: 3000,
    })
    return
  }

  const trimmedAnswer = currentAnswer.value.trim()

  // 프론트 쪽 상태에도 답안 저장
  quizStore.submitAnswer({
    questionId: currentQuestion.value.id,
    answerText: trimmedAnswer,
  })

  submitting.value = true
  try {
    const payload = {
      submissionId: quizStore.submissionId,
      questionId: currentQuestion.value.id,
      answerText: trimmedAnswer,
      totalQuestions: quizStore.currentQuiz?.questions?.length || undefined,
    }

    const result = await submitQuizAPI(Number(route.params.id), payload)

    // 전체 누적 결과 저장
    quizStore.setQuizResult(result)

    // 이번 문제 정답/오답 정보 저장 (필드명은 실제 응답에 맞게 조정)
    quizStore.setLastAnswerResult({
      correct: result.correct ?? result.isCorrect ?? null,
      correctAnswers: result.correctAnswers ?? result.acceptedAnswers ?? [],
      userAnswer: trimmedAnswer,
    })

    // 다음 입력을 위해 입력칸 clear
    currentAnswer.value = ''
  } catch (error: any) {
    console.error(error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to submit quiz',
      life: 3000,
    })
  } finally {
    submitting.value = false
  }
}

/**
 * 정답/오답 카드에서 "다음 문제" 클릭 시
 */
const handleNextAfterAnswer = () => {
  quizStore.nextQuestion() // 내부에서 showAnswerCard = false

  const q = currentQuestion.value
  if (!q) return

  const existing = quizStore.userAnswers.find(
    (a) => a.questionId === q.id,
  )
  currentAnswer.value = existing?.answerText || ''

  // 다음 문제로 넘어왔으니 다시 입력창에 포커스
  focusAnswerInput()
}

/**
 * 마지막 문제 정답/오답 카드에서 "결과 보기" 클릭 시
 */
const goToResult = () => {
  router.push(`/quiz/${route.params.id}/result`)
}

onMounted(async () => {
  if (!quizStore.currentQuiz) {
    try {
      const quizId = Number(route.params.id)
      const context = await getPlayContext(quizId)
      quizStore.startQuiz(
        {
          id: context.quizId ?? quizId,
          title: context.title,
          description: context.description,
          thumbnailUrl: context.thumbnailUrl,
        },
        context.questions || [],
      )
    } catch (error: any) {
      console.error(error)
      toast.add({
        severity: 'error',
        summary: 'Error',
        detail: '퀴즈 정보를 불러오지 못했습니다.',
        life: 3000,
      })
      router.push('/')
      return
    }
  }

  const q = currentQuestion.value
  if (!q) return
  const existing = quizStore.userAnswers.find(
    (a) => a.questionId === q.id,
  )
  currentAnswer.value = existing?.answerText || ''

  // 첫 진입 시 입력창 포커스
  focusAnswerInput()

  // 전역 엔터 키 핸들러 등록
  window.addEventListener('keydown', handleKeydown)
})

/**
 * 다음 문제의 이미지를 미리 로드(Preloading)하여
 * 문제 전환 시 깜빡임이나 지연을 최소화합니다.
 */
watch(
  () => quizStore.currentQuestionIndex,
  (newIndex) => {
    if (!quizStore.currentQuiz) return

    // 다음 문제 인덱스
    const nextIndex = newIndex + 1
    if (nextIndex < quizStore.currentQuiz.questions.length) {
      const nextQuestion = quizStore.currentQuiz.questions[nextIndex]
      if (nextQuestion?.imageUrl) {
        const img = new Image()
        img.src = nextQuestion.imageUrl
      }
    }
  },
  { immediate: true },
)

onUnmounted(() => {
  // 전역 핸들러 정리
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.quiz-play-container {
  min-height: calc(100vh - 200px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.quiz-card {
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid var(--color-border);
  box-shadow: none;
}

.quiz-card :deep(.p-card-body) {
  height: 300px; /* 카드 바디 높이 고정 */
  display: flex;
  flex-direction: column;
}

.quiz-card :deep(.p-card-content) {
  flex: 1;
  padding: 1.5rem;
  background: var(--color-background-soft);
  color: var(--color-heading);
  display: flex;
  flex-direction: column;
  justify-content: space-between; /* 콘텐츠와 버튼 고정 */
}

.thumbnail-frame {
  background: transparent;
  display: flex;
  justify-content: center;
  padding: 0 0 1rem;
}

.thumbnail-image {
  width: 606px;
  height: 344px; /* 기존 고정 높이 복구 */
  max-width: 100%;
  object-fit: contain;
  border-radius: 12px;
  border: 3px solid var(--text-main);
  background: var(--bg-surface);
}

.question-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word; /* 긴 단어로 인한 레이아웃 깨짐 방지 */
}
</style>
