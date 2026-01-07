<template>
  <div class="signup-container">
    <Toast />
    <div class="flex items-center justify-center min-h-screen px-4">
      <Card class="w-full max-w-md shadow-5">
        <template #header>
          <div class="p-6 text-center">
            <router-link to="/" class="no-underline">
              <h1 class="text-3xl font-bold text-primary m-0">Problem.io</h1>
            </router-link>
          </div>
        </template>
        <template #content>
          <div class="flex flex-col gap-4">
            <div>
              <h2 class="text-2xl font-bold mb-2">회원 가입</h2>
            </div>

            <form @submit.prevent="handleSignup" class="flex flex-col gap-4">
              <!-- 이메일 입력 (인증 제거됨) -->
              <div class="flex flex-col gap-2">
                <label for="email" class="text-sm font-medium">이메일 (ID)</label>
                <div class="flex gap-2">
                  <InputText
                    id="이메일"
                    v-model="formData.email"
                    type="이메일"
                    placeholder="you@example.com"
                    :class="{ 'p-invalid': errors.email }"
                    class="flex-1"
                    @blur="validateEmail"
                    @input="clearError('email')"
                  />
                  <!-- 인증 버튼 제거됨 -->
                </div>
                <small v-if="errors.email" class="p-error">{{ errors.email }}</small>
                <small v-else class="text-color-secondary">사용하실 이메일을 입력해주세요.</small>
              </div>

              <!-- 인증번호 입력 섹션 제거됨 -->

              <!-- 닉네임 입력 (중복 확인 포함) -->
              <div class="flex flex-col gap-2">
                <label for="nickname" class="text-sm font-medium">닉네임</label>
                <div class="flex gap-2">
                  <InputText
                    id="닉네임"
                    v-model="formData.nickname"
                    type="text"
                    placeholder="닉네임을 입력해주세요"
                    :class="{ 'p-invalid': errors.nickname }"
                    class="flex-1"
                    @blur="validateNickname"
                    @input="handleNicknameInput"
                  />
                  <Button
                    type="button"
                    label="중복 확인"
                    :loading="nicknameState.checking"
                    :disabled="!formData.nickname || !!errors.nickname || nicknameState.isChecked"
                    @click="handleCheckNickname"
                    severity="secondary"
                  />
                </div>
                <small v-if="errors.nickname" class="p-error">{{ errors.nickname }}</small>
                <small v-else-if="nicknameState.isChecked" class="text-green-500">✓ 사용 가능한 닉네임입니다.</small>
                <small v-else class="text-color-secondary">2-10자 사이로 입력해주세요.</small>
              </div>



              <!-- 비밀번호 입력 -->
              <div class="flex flex-col gap-2">
                <label for="password" class="text-sm font-medium">비밀번호</label>
                <Password
                  id="password"
                  v-model="formData.password"
                  :feedback="true"
                  toggleMask
                  placeholder="••••••••"
                  :class="{ 'p-invalid': errors.password }"
                  class="w-full"
                  inputClass="w-full"
                  @blur="validatePassword"
                  @input="clearError('password')"
                />
                <small v-if="errors.password" class="p-error">{{ errors.password }}</small>
                <small v-else class="text-color-secondary">최소 8자 이상, 영문/숫자/특수문자 각 1개 이상 포함</small>
              </div>

              <!-- 비밀번호 확인 -->
              <div class="flex flex-col gap-2">
                <label for="confirmPassword" class="text-sm font-medium">비밀번호 확인</label>
                <Password
                  id="confirmPassword"
                  v-model="formData.confirmPassword"
                  :feedback="false"
                  toggleMask
                  placeholder="••••••••"
                  :class="{ 'p-invalid': errors.confirmPassword }"
                  class="w-full"
                  inputClass="w-full"
                  @blur="validateConfirmPassword"
                  @input="clearError('confirmPassword')"
                />
                <small v-if="errors.confirmPassword" class="p-error">{{ errors.confirmPassword }}</small>
              </div>

              <!-- 회원가입 버튼 -->
              <Button type="submit" label="회원가입" :loading="loading" :disabled="!isFormValid" class="w-full" size="large" />
            </form>

            <Divider />

            <div class="text-center text-sm">
              <span class="text-color-secondary">계정이 이미 있으신가요?</span>
              <router-link to="/auth/login" class="text-primary font-semibold no-underline">로그인</router-link>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { useToast } from "primevue/usetoast";
// Auth 관련 API (회원가입)
import { signup } from "@/api/auth";
// User Store (닉네임 중복 확인)
import { useUserStore } from "@/stores/user";

const router = useRouter();
const toast = useToast();
const userStore = useUserStore(); // Store 인스턴스 생성

// 폼 데이터
const formData = ref({
  email: "",
  verificationCode: "",
  nickname: "",
  password: "",
  confirmPassword: "",
});

// 이메일 인증 상태 제거
// const emailState = ref({...});

// 닉네임 중복 확인 상태
const nicknameState = ref({
  isChecked: false,
  checking: false,
});

// 에러 메시지
const errors = ref<Record<string, string>>({});

// 로딩 상태 (회원가입 제출)
const loading = ref(false);

// 정규식
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const bannedNicknames = ["admin", "관리자", "운영자"];

// 계산된 속성 (Computed Properties)
const isEmailValid = computed(() => emailRegex.test(formData.value.email));

// const isVerificationCodeValid = computed(...) // 제거

const isPasswordStrong = computed(() => {
  const pwd = formData.value.password;
  if (pwd.length < 8) return false;
  const hasLetter = /[a-zA-Z]/.test(pwd);
  const hasNumber = /[0-9]/.test(pwd);
  const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(pwd);
  return hasLetter && hasNumber && hasSpecial;
});

// 전체 폼 유효성 검사 (가입 버튼 활성화 조건)
const isFormValid = computed(() => {
  return (
    // emailState.value.isVerified && // 제거
    isEmailValid.value &&
    nicknameState.value.isChecked && // 닉네임 중복 확인 필수
    formData.value.nickname.length >= 2 &&
    formData.value.nickname.length <= 10 &&
    isPasswordStrong.value &&
    formData.value.password === formData.value.confirmPassword &&
    formData.value.password.length > 0
  );
});

// --- 유효성 검사 함수 ---

const validateEmail = () => {
  if (!formData.value.email) {
    errors.value.email = "이메일을 입력해주세요.";
    return false;
  }
  if (!isEmailValid.value) {
    errors.value.email = "올바른 이메일 형식이 아닙니다.";
    return false;
  }
  delete errors.value.email;
  return true;
};

const validateNickname = () => {
  const nickname = formData.value.nickname;
  if (!nickname) {
    errors.value.nickname = "닉네임을 입력해주세요.";
    return false;
  }
  if (nickname.length < 2 || nickname.length > 10) {
    errors.value.nickname = "닉네임은 2-10자 사이여야 합니다.";
    return false;
  }
  // 금칙어 확인
  for (const banned of bannedNicknames) {
     if (nickname.includes(banned)) {
        errors.value.nickname = `"${banned}" 단어는 사용할 수 없습니다.`;
        return false;
     }
  }

  delete errors.value.nickname;
  return true;
};

const validatePassword = () => {
  const pwd = formData.value.password;
  if (!pwd) {
    errors.value.password = "비밀번호를 입력해주세요.";
    return false;
  }
  if (pwd.length < 8 || pwd.length > 20) {
    errors.value.password = "비밀번호는 8자 이상 20자 이하여야 합니다.";
    return false;
  }
  if (!isPasswordStrong.value) {
    errors.value.password = "영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.";
    return false;
  }
  delete errors.value.password;
  return true;
};

const validateConfirmPassword = () => {
  if (!formData.value.confirmPassword) {
    errors.value.confirmPassword = "비밀번호 확인을 입력해주세요.";
    return false;
  }
  if (formData.value.password !== formData.value.confirmPassword) {
    errors.value.confirmPassword = "비밀번호가 일치하지 않습니다.";
    return false;
  }
  delete errors.value.confirmPassword;
  return true;
};




const clearError = (field: string) => {
  delete errors.value[field];
};

// --- 핸들러 ---
// 이메일 인증 관련 핸들러 제거됨
// const handleSendVerificationCode ...
// const handleCodeInput ...
// const handleVerifyCode ...

// 4. 닉네임 입력 감지 (입력 변경 시 중복확인 초기화)
const handleNicknameInput = () => {
  clearError("nickname");
  nicknameState.value.isChecked = false; // 내용이 바뀌면 다시 확인해야 함
};

// 5. 닉네임 중복 확인 (Store 사용)
const handleCheckNickname = async () => {
  if (!validateNickname()) return;

  nicknameState.value.checking = true;
  try {
    // Store 액션 호출
    await userStore.checkNickname(formData.value.nickname);

    nicknameState.value.isChecked = true;
    toast.add({ severity: "success", summary: "성공", detail: "사용 가능한 닉네임입니다.", life: 3000 });
  } catch (error: any) {
    nicknameState.value.isChecked = false;
    const errorMessage = error.response?.data?.message || "이미 사용 중인 닉네임입니다.";
    errors.value.nickname = errorMessage;
    toast.add({ severity: "error", summary: "불가능", detail: errorMessage, life: 3000 });
  } finally {
    nicknameState.value.checking = false;
  }
};

// 6. 최종 회원가입
const handleSignup = async () => {
  // 최종 유효성 검사
  if (!validateEmail() || !validateNickname() || !validatePassword() || !validateConfirmPassword()) {
    toast.add({ severity: "warn", summary: "확인", detail: "입력 정보를 확인해주세요.", life: 3000 });
    return;
  }

  // if (!emailState.value.isVerified) {
  //   toast.add({ severity: "warn", summary: "확인", detail: "이메일 인증을 완료해주세요.", life: 3000 });
  //   return;
  // }

  if (!nicknameState.value.isChecked) {
    toast.add({ severity: "warn", summary: "확인", detail: "닉네임 중복 확인을 해주세요.", life: 3000 });
    return;
  }

  loading.value = true;
  try {
    await signup({
      email: formData.value.email,
      password: formData.value.password,
      nickname: formData.value.nickname,
    });
    toast.add({ severity: "success", summary: "환영합니다!", detail: "회원가입이 완료되었습니다.", life: 3000 });

    setTimeout(() => {
      router.push("/auth/login");
    }, 1000);
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || error.response?.data?.data?.message || error.response?.data || "회원가입 실패";
    toast.add({ severity: "error", summary: "오류", detail: errorMessage, life: 3000 });
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.signup-container {
  min-height: 100vh;
  background: var(--bg-main);
}

.max-w-md {
  max-width: 28rem;
}

.w-full {
  width: 100%;
}

.min-h-screen {
  min-height: 50vh;
}

.px-4 {
  padding-left: 1rem;
  padding-right: 1rem;
}

.p-6 {
  padding: 1.5rem;
}

.text-3xl {
  font-size: 1.875rem;
  line-height: 2.25rem;
}

.text-2xl {
  font-size: 1.5rem;
  line-height: 2rem;
}

.text-sm {
  font-size: 0.875rem;
  line-height: 1.25rem;
}

.font-bold {
  font-weight: 700;
}

.font-semibold {
  font-weight: 600;
}

.font-medium {
  font-weight: 500;
}

.text-center {
  text-align: center;
}

.m-0 {
  margin: 0;
}

.mb-2 {
  margin-bottom: 0.5rem;
}

.no-underline {
  text-decoration: none;
}

.shadow-5 {
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06), 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.flex-1 {
  flex: 1 1 0%;
}

.text-green-500 {
  color: #10b981;
}
</style>
