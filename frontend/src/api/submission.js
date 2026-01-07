import apiClient from './axios'

// 퀴즈 풀이 제출 (한 문제씩)
export const submitQuiz = async (quizId, payload) => {
  // payload: { submissionId?, questionId, answerText }
  const token = localStorage.getItem('accessToken')
  const headers = token ? { Authorization: `Bearer ${token}` } : {}

  const response = await apiClient.post(
    `/quizzes/${quizId}/submissions`,
    payload,
    { headers }
  )
  return response.data.data   // QuizAnswerResponse
}

// 풀이 결과 상세 조회 (전체 통계용 – 나중에 사용)
export const getSubmission = async (submissionId) => {
  const response = await apiClient.get(`/submissions/${submissionId}`)
  return response.data.data
}

// 퀴즈 플레이 컨텍스트 (문제·정답 셋업용)
export const getPlayContext = async (quizId) => {
  const token = localStorage.getItem('accessToken')
  const headers = token ? { Authorization: `Bearer ${token}` } : {}

  const response = await apiClient.get(`/quizzes/${quizId}/play-context`, {
    headers,
  })
  return response.data.data
}
