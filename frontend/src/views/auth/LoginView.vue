<template>
  <div class="login-container">
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
            <form @submit.prevent="handleLogin" class="flex flex-col gap-4">
              <div class="flex flex-col gap-2">
                <label for="email" class="text-sm font-medium">이메일</label>
                <InputText id="email" v-model="email" type="email"  required class="w-full" />
              </div>
              <div class="flex flex-col gap-2">
                <label for="password" class="text-sm font-medium">비밀번호</label>
                <Password id="password" v-model="password" :feedback="false" toggleMask  required class="w-full" inputClass="w-full" />
              </div>
              <Button type="submit" label="로그인" :loading="loading" class="w-full" size="large" />
            </form>
            <Divider />
            <div class="text-center text-sm">
              <span class="text-color-secondary">아이디가 없으신가요?</span>
              <router-link to="/signup" class="text-primary font-semibold no-underline">회원 가입</router-link>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useToast } from "primevue/usetoast";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const route = useRoute();
const toast = useToast();
const authStore = useAuthStore();

const email = ref("");
const password = ref("");
const loading = ref(false);

const handleLogin = async () => {
  loading.value = true;
  try {
    await authStore.loginUser(email.value, password.value);
    toast.add({ severity: "success", summary: "로그인", detail: "로그인 성공", life: 3000 });

    const redirect = (route.query.redirect as string) || "/";
    router.push(redirect);
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "실패",
      detail: error.response?.data?.message || "로그인에 실패했습니다.",
      life: 3000,
    });
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-container {
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

.text-center {
  text-align: center;
}

.m-0 {
  margin: 0;
}

.mb-2 {
  margin-bottom: 0.5rem;
}

.mb-4 {
  margin-bottom: 1rem;
}

.no-underline {
  text-decoration: none;
}

.shadow-5 {
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06), 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}


</style>
