import apiClient from './axios'
import { resolveImageUrl, resolveQuizImages } from '@/lib/image'

const BASE_URL = '/quizzes'

/**
 * 공통 에러 정규화 헬퍼
 * - 서버에서 내려주는 code / errorCode / message를 하나로 모아준다.
 */
const normalizeApiError = (error) => {
  const res = error?.response?.data

  return {
    code: res?.code || res?.errorCode || null,
    message: res?.message || '요청 처리 중 오류가 발생했습니다.',
    status: error?.response?.status,
    raw: error,
  }
}

/**
 * 퀴즈 관련 API
 */

// 퀴즈 목록 조회
export const getQuizzes = async (params = {}) => {
  const { page = 1, size = 20, sort = 'latest', keyword = '' } = params
  const queryParams = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    sort,
  })

  if (keyword) {
    queryParams.append('keyword', keyword)
  }

  const response = await apiClient.get(`${BASE_URL}?${queryParams.toString()}`)
  const data = response.data.data
  if (data?.content) {
    data.content = data.content.map((quiz) => ({
      ...quiz,
      thumbnailUrl: resolveImageUrl(quiz.thumbnailUrl),
    }))
  }
  return data
}

// 퀴즈 상세 조회
export const getQuiz = async (quizId) => {
  const response = await apiClient.get(`${BASE_URL}/${quizId}`)
  return resolveQuizImages(response.data.data)
}

// 퀴즈 시작용 문제 목록 (랜덤, 제한 개수)
export const getQuizQuestions = async (quizId, limit) => {
  const params = {}
  if (limit) params.limit = limit
  const response = await apiClient.get(`${BASE_URL}/${quizId}/questions`, { params })
  return response.data.data
}

// 퀴즈 생성
export const createQuiz = async (data) => {
  const response = await apiClient.post(BASE_URL, {
    title: data.title,
    description: data.description,
    thumbnailUrl: data.thumbnailUrl,
    isPublic: data.isPublic ?? true,
    questions: data.questions.map((q) => ({
      order: q.questionOrder,
      imageUrl: q.imageUrl,
      description: q.description,
      answers: q.answers.map((a, idx) => ({
        text: a,
        sortOrder: idx + 1,
      })),
    })),
  })
  return resolveQuizImages(response.data.data)
}

// 퀴즈 수정
export const updateQuiz = async (quizId, data) => {
  const response = await apiClient.put(`${BASE_URL}/${quizId}`, {
    title: data.title,
    description: data.description,
    thumbnailUrl: data.thumbnailUrl,
    isPublic: data.isPublic,
    questions: data.questions.map((q) => ({
      order: q.questionOrder,
      imageUrl: q.imageUrl,
      description: q.description,
      answers: q.answers.map((a, idx) => ({
        text: a.text ?? a, // 기존 데이터 구조에 따라 필요시 조정
        sortOrder: a.sortOrder ?? idx + 1,
      })),
    })),
  })
  return resolveQuizImages(response.data.data)
}

// 퀴즈 삭제
export const deleteQuiz = async (quizId) => {
  await apiClient.delete(`${BASE_URL}/${quizId}`)
}

// 퀴즈 좋아요
export const likeQuiz = async (quizId) => {
  try {
    const response = await apiClient.post(`${BASE_URL}/${quizId}/like`)
    return response.data.data
  } catch (error) {
    throw normalizeApiError(error)
  }
}

// 퀴즈 좋아요 취소
export const unlikeQuiz = async (quizId) => {
  try {
    const response = await apiClient.delete(`${BASE_URL}/${quizId}/like`)
    return response.data.data
  } catch (error) {
    throw normalizeApiError(error)
  }
}

// 내가 만든 퀴즈 목록
export const getMyQuizzes = async () => {
  const response = await apiClient.get('/quizzes/me')
  return response.data.data.map((quiz) => ({
    ...quiz,
    thumbnailUrl: resolveImageUrl(quiz.thumbnailUrl),
  }))
}
