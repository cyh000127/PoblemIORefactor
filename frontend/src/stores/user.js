import { defineStore } from "pinia";
import { ref } from "vue";
import { checkNickname as apiCheckNickname } from "@/api/user";

export const useUserStore = defineStore("user", () => {
  // ----------------------------------------------------------------
  // State
  // ----------------------------------------------------------------
  const userInfo = ref(null);

  // ----------------------------------------------------------------
  // Actions
  // ----------------------------------------------------------------

  /**
   * 닉네임 중복 확인
   * @param {string} nickname - 중복 확인할 닉네임
   * @returns {Promise<any>} - API 응답 결과
   */
  const checkNickname = async (nickname) => {
    try {
      // API 호출 (api/user.js의 checkNickname 사용)
      const response = await apiCheckNickname(nickname);
      return response;
    } catch (error) {
      // 에러 발생 시 컴포넌트에서 처리할 수 있도록 throw
      throw error;
    }
  };

  const setUserInfo = (user) => {
    userInfo.value = user;
  };

  return {
    userInfo,
    checkNickname,
    setUserInfo,
  };
});
