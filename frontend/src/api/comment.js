import apiClient from './axios'

const BASE_URL = '/quizzes'

/**
 * 댓글 관련 API
 */

// 댓글 목록 조회 (페이지네이션)
export const fetchComments = async (quizId, page = 1, size = 20) => {
  const response = await apiClient.get(`${BASE_URL}/${quizId}/comments`, {
    params: { page, size },
  })

  const data = response.data?.data ?? response.data

  // 가능한 모든 위치에서 배열 후보를 모아 가장 먼저 나오는 배열을 사용
  const candidates = [
    data?.content,
    data?.comments,
    data?.items,
    data?.data?.content,
    data?.data?.comments,
    data?.data?.items,
    data?.data,
    data,
  ]

  const comments =
    candidates.find((c) => Array.isArray(c)) ?? []

  const hasNext =
    typeof data?.hasNext === 'boolean'
      ? data.hasNext
      : typeof data?.last === 'boolean'
      ? !data.last
      : typeof data?.totalPages === 'number'
      ? page < data.totalPages
      : Array.isArray(comments)
      ? comments.length === size
      : false

  return { comments, hasNext }
}

// 이전 이름 호환
export const getComments = fetchComments

// 대댓글 조회
export const fetchReplies = async (commentId) => {
  const response = await apiClient.get(`/comments/${commentId}/replies`)
  const data = response.data?.data ?? response.data

  const candidates = [
    data?.comments,
    data?.items,
    data?.data,
    data,
  ]
  const replies = candidates.find((c) => Array.isArray(c)) ?? []
  return replies
}

// 댓글 작성 (게스트는 nickname/password 포함)
export const createComment = async (quizId, payload) => {
  const response = await apiClient.post(
    `${BASE_URL}/${quizId}/comments`,
    payload,
  )
  return response.data.data
}

// 댓글 수정 (게스트는 password 포함)
export const updateComment = async (commentId, payload) => {
  const response = await apiClient.patch(`/comments/${commentId}`, payload)
  return response.data.data
}

// 댓글 삭제 (게스트는 password 포함)
export const deleteComment = async (commentId, password) => {
  await apiClient.delete(`/comments/${commentId}`, {
    data: password ? { password } : undefined,
  })
}

// 댓글 좋아요 토글
export const toggleCommentLike = async (commentId) => {
  const response = await apiClient.post(`/comments/${commentId}/likes`)
  return response.data.data
}

