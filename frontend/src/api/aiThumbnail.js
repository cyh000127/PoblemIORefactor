import apiClient from './axios'

export const createCandidates = async (title, description) => {
  const response = await apiClient.post('/ai/quiz-thumbnails/candidates', {
    title,
    description,
  })
  return response.data.data
}

export const confirmCandidate = async (candidateId) => {
  const response = await apiClient.post('/ai/quiz-thumbnails/confirm', {
    candidateId,
  })
  return response.data.data
}
