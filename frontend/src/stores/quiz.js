// frontend/src/stores/quiz.js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useQuizStore = defineStore('quiz', () => {
  // Quiz Creation State
  const quizForm = ref({
    title: '',
    description: '',
    thumbnailUrl: '',
    questions: [],
  })

  // Quiz Play State
  const currentQuiz = ref(null)
  const currentQuestionIndex = ref(0)
  const userAnswers = ref([])
  const quizResult = ref(null)

  // 서버에서 받은 submissionId (플레이 중 동안 재사용)
  const submissionId = ref(null)

  // 🔥 [추가] 마지막 문제 정답/오답 카드 제어용 상태
  const showAnswerCard = ref(false)

  // 🔥 [추가] 현재 문제에 대한 정답/오답 정보
  const lastAnswerResult = ref({
    correct: null,
    correctAnswers: [],
    userAnswer: '',
  })

  // ----- Actions -----

  function addQuestion() {
    quizForm.value.questions.push({
      questionOrder: quizForm.value.questions.length + 1,
      description: '',
      imageUrl: '',
      answers: [],
    })
  }

  function removeQuestion(index) {
    quizForm.value.questions.splice(index, 1)
    quizForm.value.questions.forEach((q, idx) => {
      q.questionOrder = idx + 1
    })
  }

  function resetQuizForm() {
    quizForm.value = {
      title: '',
      description: '',
      thumbnailUrl: '',
      questions: [],
    }
  }

  // 퀴즈 시작 시 초기화
  function startQuiz(quiz, questionList) {
    const sourceQuestions = questionList && questionList.length > 0
      ? questionList
      : (quiz.questions || [])
    // 질문 순서를 플레이할 때만 랜덤하게 섞는다 (원본 퀴즈는 보존)
    const shuffledQuestions = [...sourceQuestions]
    for (let i = shuffledQuestions.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1))
        ;[shuffledQuestions[i], shuffledQuestions[j]] = [shuffledQuestions[j], shuffledQuestions[i]]
    }
    // 섞인 순서에 맞춰 questionOrder를 재부여
    const randomizedQuiz = {
      ...quiz,
      questions: shuffledQuestions.map((q, idx) => ({
        ...q,
        questionOrder: idx + 1,
      })),
    }

    currentQuiz.value = randomizedQuiz
    currentQuestionIndex.value = 0
    userAnswers.value = []
    quizResult.value = null
    submissionId.value = null

    showAnswerCard.value = false
    lastAnswerResult.value = {
      correct: null,
      correctAnswers: [],
      userAnswer: '',
    }
  }

  // 사용자 답변 저장
  function submitAnswer(answer) {
    if (!answer || !answer.questionId) return

    const idx = userAnswers.value.findIndex(
      (a) => a.questionId === answer.questionId,
    )

    if (idx !== -1) {
      userAnswers.value[idx].answerText = answer.answerText
    } else {
      userAnswers.value.push({
        questionId: answer.questionId,
        answerText: answer.answerText,
      })
    }

    // 🔥 정답 카드 표시 상태 On (데이터 설정 후 변경해야 함)
    // showAnswerCard.value = true  <-- 여기서 제거
  }

  // 🔥 컴포넌트에서 서버 응답 기반으로 정답/오답 데이터 저장
  function setLastAnswerResult(result) {
    lastAnswerResult.value = result
    // 🔥 데이터가 준비되었을 때 카드를 보여준다
    showAnswerCard.value = true
  }

  // 다음 문제 이동
  function nextQuestion() {
    if (
      currentQuiz.value &&
      currentQuestionIndex.value < currentQuiz.value.questions.length - 1
    ) {
      currentQuestionIndex.value++
    }

    // 정답 카드 닫기
    showAnswerCard.value = false

    // 🔥 이전 정답 정보 초기화 (중요: 이렇게 해야 다음 문제에서 카드 열릴 때 "이전 데이터"가 안 보임)
    lastAnswerResult.value = {
      correct: null,
      correctAnswers: [],
      userAnswer: '',
    }
  }

  // 서버 결과 저장
  function setQuizResult(result) {
    quizResult.value = result
    if (result && result.submissionId) {
      submissionId.value = result.submissionId
    }
  }

  function resetQuizPlay() {
    currentQuiz.value = null
    currentQuestionIndex.value = 0
    userAnswers.value = []
    quizResult.value = null
    submissionId.value = null

    showAnswerCard.value = false
    lastAnswerResult.value = {
      correct: null,
      correctAnswers: [],
      userAnswer: '',
    }
  }

  return {
    // State
    quizForm,
    currentQuiz,
    currentQuestionIndex,
    userAnswers,
    quizResult,
    submissionId,

    // 🔥 추가된 State
    showAnswerCard,
    lastAnswerResult,

    // Actions
    addQuestion,
    removeQuestion,
    resetQuizForm,
    startQuiz,
    submitAnswer,
    setLastAnswerResult, // 추가된 함수
    nextQuestion,
    setQuizResult,
    resetQuizPlay,
  }
})
