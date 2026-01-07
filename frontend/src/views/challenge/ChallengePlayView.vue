<template>
  <div class="challenge-play-container">
    <div class="container mx-auto px-4">
      <div v-if="loading" class="text-center py-8">
        <i class="pi pi-spin pi-spinner text-4xl"></i>
      </div>

      <div v-else class="flex flex-col lg:flex-row justify-center gap-6 items-start relative">
        <!-- 메인 게임 영역 -->
        <div class="w-full max-w-3xl flex flex-col gap-6">
            <!-- 상단 타이머 및 정보 -->


        <Card>
          <template #content>
            <div class="flex flex-col gap-6">
              <!-- 상단 타이머 및 정보 (내부로 이동됨) -->
              <div class="mb-2">
                   <div class="flex justify-between items-center">
                      <div class="font-bold text-xl">
                          {{ challengeTitle }}
                      </div>
                  <div v-if="challengeType === 'SURVIVAL'" class="text-2xl font-bold text-primary">
                      <i class="pi mr-2"></i>
                      현재 문제 {{ currentIndex + 1 }}
                  </div>
                  <div v-else class="text-2xl font-mono font-bold" :class="timerColorClass">
                      <i class="pi pi-stopwatch mr-2"></i>
                      {{ formattedTimer }}
                  </div>
              </div>
              </div>

              <!-- 문제 풀이 화면 -->
              <div v-if="currentQuestion.imageUrl" class="question-image-container aspect-video bg-surface-100 overflow-hidden border-round flex items-center justify-center mb-4">
                <img :src="resolveImageUrl(currentQuestion.imageUrl)" :alt="currentQuestion.description" class="w-full h-full object-contain" />
              </div>

              <div v-if="currentQuestion?.description" class="text-xl">
                {{ currentQuestion.description }}
              </div>

              <div class="flex flex-col gap-3">
                <label class="text-lg font-semibold">정답:</label>
                <InputText
                  ref="answerInput"
                  v-model="currentAnswer"
                  placeholder="정답을 입력해주세요..."
                  class="w-full"
                  @keydown.enter="submitAnswer"
                  :disabled="submitting"
                />
              </div>

              <div class="flex">
                <Button
                  label="제출"
                  icon="pi pi-check"
                  class="flex-1"
                  :disabled="!currentAnswer.trim() || submitting"
                  :loading="submitting"
                  @click="submitAnswer"
                >
                </Button>
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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Card from 'primevue/card'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import { startChallenge, submitAnswer as submitAnswerAPI } from '@/api/challenge'
import { resolveImageUrl } from "@/lib/image"
import ChallengeRankingWidget from "@/components/challenge/ChallengeRankingWidget.vue"

const route = useRoute()
const router = useRouter()
const toast = useToast()

const loading = ref(true)
const submitting = ref(false)
const challengeId = route.params.id
const challengeTitle = ref("")
const challengeType = ref("") 
const submissionId = ref(null)
const questions = ref([])
const currentIndex = ref(0)
const timeLimit = ref(0)
const startTime = ref(null)

// 타이머 변수
const timerValue = ref(0) // 초 단위 (남은 시간 또는 경과 시간)
const timerInterval = ref(null)

const currentAnswer = ref("")
const answerInput = ref(null)

const currentQuestion = computed(() => questions.value[currentIndex.value])

// 타이머 형식 (MM:SS)
const formattedTimer = computed(() => {
    const min = Math.floor(timerValue.value / 60).toString().padStart(2, '0')
    const sec = (timerValue.value % 60).toString().padStart(2, '0')
    
    return `${min}:${sec}`
})



const timerColorClass = computed(() => {
    if (challengeType.value === 'TIME_ATTACK' && timerValue.value < 10) {
        return 'text-red-500'
    }
    return 'text-primary'
})

const startTimer = () => {
    if (timerInterval.value) clearInterval(timerInterval.value)
    
    if (challengeType.value === 'TIME_ATTACK') {
        timerValue.value = timeLimit.value
        timerInterval.value = setInterval(() => {
            timerValue.value--
            if (timerValue.value <= 0) {
                handleTimeOver()
            }
        }, 1000)
    } else {
        // 서바이벌: 카운트 업
        timerValue.value = 0
        timerInterval.value = setInterval(() => {
            timerValue.value++
        }, 1000)
    }
}

const handleTimeOver = () => {
    clearInterval(timerInterval.value)
    toast.add({ severity: 'error', summary: '시간 초과!', detail: '제한 시간이 초과되었습니다!', life: 3000 })
    // 자동 제출 로직이 필요하다면 추가, 혹은 바로 결과 화면으로
    router.push(`/challenges/${challengeId}/result`)
}

const initChallenge = async () => {
    try {
        const data = await startChallenge(challengeId)
        submissionId.value = data.submissionId
        questions.value = data.questions
        challengeType.value = data.challengeType
        timeLimit.value = data.timeLimit || 0
        
        // 타이머 시작
        startTimer()
        
        loading.value = false
        focusInput()
    } catch (error) {
        console.error(error)
        toast.add({ severity: 'error', summary: 'Error', detail: '챌린지를 시작할 수 없습니다.', life: 3000 })
        router.push('/challenges')
    }
}

const submitAnswer = async () => {
    if (!currentAnswer.value.trim() || submitting.value) return
    
    submitting.value = true
    try {
        const payload = {
            submissionId: submissionId.value, // ChallengeStartResponse에서 반환됨
            questionId: currentQuestion.value.id,
            answerText: currentAnswer.value.trim()
        }
        
        const { data: result } = await submitAnswerAPI(challengeId, payload)
        
        // 로직 분기
        if (challengeType.value === 'SURVIVAL') {
            if (!result.correct) {
                // 게임 오버
                clearInterval(timerInterval.value)
                toast.add({ severity: 'error', summary: 'Game Over', detail: '오답입니다! 챌린지가 종료됩니다.', life: 2000 })
                setTimeout(() => {
                    router.replace(`/challenges/${challengeId}/result`)
                }, 1000)
                return
            }
        }
        
        // 다음 문제
        if (currentIndex.value < questions.value.length - 1) {
            currentIndex.value++
            currentAnswer.value = ""
            focusInput()
        } else {
            // 모든 문제 해결
            clearInterval(timerInterval.value)
            toast.add({ severity: 'success', summary: 'Clear!', detail: '모든 문제를 풀었습니다!', life: 2000 })
            setTimeout(() => {
                router.replace(`/challenges/${challengeId}/result`)
            }, 1000)
        }
        
    } catch (error) {
        console.error(error)
        // 백엔드에서 에러 발생 시 특정 에러 코드(예: TIME_OVER) 로직 확인
        toast.add({ severity: 'error', summary: 'Error', detail: '제출 중 오류가 발생했습니다.', life: 3000 })
    } finally {
        submitting.value = false
    }
}

const focusInput = () => {
    nextTick(() => {
        // PrimeVue InputText에 맞춰 조정된 간단한 DOM 포커스 로직
        const el = document.querySelector('input.p-inputtext') as HTMLElement
        if (el) el.focus()
    })
}

onMounted(() => {
    initChallenge()
})

onUnmounted(() => {
    if (timerInterval.value) clearInterval(timerInterval.value)
})
</script>

<style scoped>
.challenge-play-container {
  min-height: calc(100vh - 200px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}
.aspect-video {
  aspect-ratio: 16 / 9;
}
</style>
