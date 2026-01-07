import apiClient from "./axios";

/**
 * 인증 관련 API
 */

// 기본 API 경로
const BASE_URL = "/auth";

// 이메일 인증 코드 발송
export const sendEmailVerification = async (email) => {
  const response = await apiClient.post(`/auth/email/send?email=${encodeURIComponent(email)}`);
  return response.data;
};

// 이메일 인증 코드 확인
export const verifyEmailCode = async (email, code) => {
  const response = await apiClient.post("/auth/email/verify", {
    email,
    code,
  });
  return response.data;
};

// 회원가입
export const signup = async (data) => {
  const response = await apiClient.post(`${BASE_URL}/signup`, {
    email: data.email,
    password: data.password,
    nickname: data.nickname,
  });
  return response.data.data;
};

// 로그인
export const login = async (email, password) => {
  const response = await apiClient.post(`${BASE_URL}/login`, {
    email,
    password,
  });

  // 토큰 저장
  if (response.data.data?.accessToken) {
    localStorage.setItem("accessToken", response.data.data.accessToken);
  }

  return response.data.data;
};

// [추가됨] 토큰 재발급 (수동 호출용)
export const reissue = async () => {
  const response = await apiClient.post(`${BASE_URL}/reissue`);

  if (response.data.data?.accessToken) {
    localStorage.setItem("accessToken", response.data.data.accessToken);
  }
  return response.data.data;
};

// 로그아웃
export const logout = async () => {
  try {
    await apiClient.post(`${BASE_URL}/logout`);
  } finally {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("user");
  }
};
