import axios from '@/api/axios'

// 챌린지 목록 조회
export const getChallenges = async () => {
    const { data } = await axios.get('/challenges')
    return data
}

// 챌린지 상세 조회
export const getChallenge = async (id) => {
    const { data } = await axios.get(`/challenges/${id}`)
    return data
}

// 챌린지 시작 (로그인 필요)
// 반환: ChallengeStartResponse (submissionId, timeLimit, questions 등)
export const startChallenge = async (id) => {
    const { data } = await axios.post(`/challenges/${id}/start`)
    return data
}

// 답안 제출 (로그인 필요)
// 요청: { submissionId, questionId, answerText }
export const submitAnswer = async (id, request) => {
    const response = await axios.post(`/challenges/${id}/submit`, request)
    return { 
        data: response.data, 
        location: response.headers['location'] 
    }
}

// 챌린지 결과 조회 (로그인 필요)
export const getChallengeResult = async (id) => {
    const { data } = await axios.get(`/challenges/${id}/result`)
    return data
}

// 챌린지 랭킹 조회
export const getLeaderboard = async (id) => {
    const { data } = await axios.get(`/challenges/${id}/leaderboard`)
    return data
}
