import instance from './axios'

// 관리자용 퀴즈 목록 조회 (숨김 포함)
export const getAdminQuizzes = async (params) => {
  const response = await instance.get('/admin/quizzes', { params })
  return response.data
}

// 퀴즈 숨김 처리 토글
export const toggleQuizVisibility = async (quizId) => {
  const response = await instance.patch(`/admin/quizzes/${quizId}/hide`)
  return response.data
}

// 챌린지 생성
export const createChallenge = async (challengeData) => {
  const response = await instance.post('/admin/challenges', challengeData)
  return response.data
}

// 커스텀 아이템 생성
export const createCustomItem = async (itemData) => {
    const response = await instance.post('/admin/items', itemData)
    return response.data
}

// 커스텀 아이템 목록 조회
export const getCustomItems = async () => {
    const response = await instance.get('/admin/items')
    return response.data
}

// 아이템 유저 할당
export const assignItemToUser = async (itemId, userId) => {
    const response = await instance.post(`/admin/items/${itemId}/assign/${userId}`)
    return response.data
}
// 아이템 이미지 업로드
export const uploadItemImage = async (file, type) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);
    
    // FormData 사용 시 axios가 Content-Type을 자동 설정함
    const response = await instance.post('/admin/items/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data; // 상대 경로 문자열 반환
}

// 아이템 수정
export const updateCustomItem = async (itemId, itemData) => {
    const response = await instance.put(`/admin/items/${itemId}`, itemData)
    return response.data
}

// 할당된 유저 조회
export const getAssignedUsers = async (itemId) => {
    const response = await instance.get(`/admin/items/${itemId}/users`)
    return response.data
}

// 유저 할당 취소
export const revokeUserItem = async (itemId, userId) => {
    const response = await instance.delete(`/admin/items/${itemId}/users/${userId}`)
    return response.data
}

// 아이템 삭제
export const deleteCustomItem = async (itemId) => {
    const response = await instance.delete(`/admin/items/${itemId}`)
    return response.data
}
