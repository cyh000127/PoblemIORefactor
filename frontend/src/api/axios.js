// src/api/axios.js
import axios, { AxiosHeaders } from "axios";

// API 기본 URL 설정 (환경변수로 관리 가능) - 끝에 슬래시 제거
const envApiUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const API_BASE_URL = envApiUrl.endsWith("/") ? envApiUrl.slice(0, -1) : envApiUrl;

// Axios 인스턴스 생성
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // Refresh Token(쿠키) 송수신을 위해 필수
});

// JWT 페이로드 파싱 함수
const parseJwt = (token) => {
  try {
    return JSON.parse(atob(token.split(".")[1]));
  } catch (e) {
    return null;
  }
};

// 요청 인터셉터
apiClient.interceptors.request.use(
  async (config) => {
    // headers 객체가 없으면 AxiosHeaders 인스턴스로 생성
    if (!config.headers) {
      config.headers = new AxiosHeaders();
    }

    // 토큰 추가 & 만료 체크 (Proactive Refresh)
    let token = localStorage.getItem("accessToken");

    // Reissue 요청 자체는 체크 및 헤더 추가 제외
    if (token && !config.url.includes("/auth/reissue")) {
      const decoded = parseJwt(token);
      const currentTime = Date.now() / 1000;

      // 만료 5분 전(300초)이거나 이미 만료되었으면 재발급 시도 (Clock Skew 대응)
      if (decoded && decoded.exp < currentTime + 300) {
        // 이미 다른 요청에 의해 재발급 중이라면 대기
        if (isRefreshing) {
          await new Promise((resolve) => {
            addRefreshSubscriber((newToken) => {
              token = newToken;
              resolve();
            });
          });
        } else {
          // 아무도 재발급 중이 아니라면 내가 시도
          isRefreshing = true;
          try {
            const response = await axios.post(`${API_BASE_URL}/auth/reissue`, {}, { withCredentials: true });

            const newAccessToken = response.data.data?.accessToken;
            if (newAccessToken) {
              localStorage.setItem("accessToken", newAccessToken);
              token = newAccessToken;
              onRefreshed(newAccessToken);
            }
          } catch (error) {
            console.error("Proactive reissue failed", error);

            // 403 Forbidden (Refresh Token 유효하지 않음/만료/없음) 시 강제 로그아웃
            if (error.response && error.response.status === 403) {
              localStorage.removeItem("accessToken");
              localStorage.removeItem("user");
              window.location.href = "/login";
              return Promise.reject(error);
            }

            isRefreshing = false;
          } finally {
            isRefreshing = false;
          }
        }
      }

      config.headers.set("Authorization", `Bearer ${token}`);
    }

    // FormData 여부 판단
    const isFormData = config.data instanceof FormData;

    if (!isFormData) {
      if (!config.headers.has("Content-Type")) {
        config.headers.set("Content-Type", "application/json");
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터
let isRefreshing = false;
let refreshSubscribers = [];

const onRefreshed = (accessToken) => {
  refreshSubscribers.forEach((callback) => callback(accessToken));
  refreshSubscribers = [];
};

const addRefreshSubscriber = (callback) => {
  refreshSubscribers.push(callback);
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 401 에러(Unauthorized) 처리
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 이미 재발급 중이라면, 완료 후 실행될 콜백을 등록하고 대기
        return new Promise((resolve) => {
          addRefreshSubscriber((token) => {
            originalRequest.headers.set("Authorization", `Bearer ${token}`);
            resolve(apiClient(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const response = await axios.post(`${API_BASE_URL}/auth/reissue`, {}, { withCredentials: true });

        const newAccessToken = response.data.data?.accessToken;

        if (newAccessToken) {
          localStorage.setItem("accessToken", newAccessToken);

          // 대기 중이던 요청들 처리
          onRefreshed(newAccessToken);

          // 현재 요청 재시도
          originalRequest.headers.set("Authorization", `Bearer ${newAccessToken}`);
          return apiClient(originalRequest);
        }
      } catch (reissueError) {
        console.error("토큰 재발급 실패:", reissueError);
        localStorage.removeItem("accessToken");
        localStorage.removeItem("user");
        window.location.href = "/login";
        return Promise.reject(reissueError);
      } finally {
        isRefreshing = false;
      }
    }

    // 401이고 재시도했거나, 재발급 실패한 경우
    if (error.response?.status === 401) {
      // 상단 catch 블록에서 로그아웃 처리하므로 여기서는 패스하거나 중복 방지
      // 만약 _retry=true인 상태로 다시 401이 왔다면 (새 토큰도 거부됨 -> 정말 만료)
      if (originalRequest._retry) {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("user");
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
