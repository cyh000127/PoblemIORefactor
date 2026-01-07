<template>
  <Menubar :model="menuItems" class="app-header">
    <template #start>
      <router-link to="/" class="flex align-items-center gap-2 no-underline text-dark logo-link">
        <img src="/problemio-logo-light.png" alt="Problem.io" class="logo-img logo-light" />
        <img src="/problemio-logo.png" alt="Problem.io" class="logo-img logo-dark" />
      </router-link>
    </template>

    <template #end>
      <div class="flex align-items-center gap-3">
        <template v-if="!authStore.isAuthenticated">
          <Button label="퀴즈 만들기" icon="pi pi-plus" class="nav-ghost" text @click="handleCreateQuiz" />
          <Button label="로그인" icon="pi pi-sign-in" class="nav-ghost" @click="goToLogin" />
        </template>

        <template v-else>
          <Button label="퀴즈 만들기" icon="pi pi-plus" class="nav-ghost" text @click="goToCreateQuiz" />

          <Button @click="goToMyPage" class="nav-ghost flex align-items-center gap-2 p-button-text p-0" aria-label="My Page">
            <UserAvatar class="mr-1 font-bold" :class="{ 'surface-200 text-700': !authStore.user?.profileImageUrl }" size="medium" />

            <span class="text-color">{{ authStore.user?.nickname || "User" }}</span>
          </Button>

          <Button label="로그아웃" icon="pi pi-sign-out" class="logout-btn" @click="handleLogout" />
        </template>
      </div>
    </template>
  </Menubar>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { useToast } from "primevue/usetoast";
import { useConfirm } from "primevue/useconfirm";
import UserAvatar from "@/components/common/UserAvatar.vue"; // 유저 아바타 불러오기

const router = useRouter();
const authStore = useAuthStore();
const toast = useToast();
const confirm = useConfirm();

const searchKeyword = ref("");
const menuItems = ref([]); // 빈 배열이라도 ref로 감싸는 것이 안전함

// [비로그인용] 퀴즈 생성 클릭 시 → 로그인 유도
const handleCreateQuiz = () => {
  if (!authStore.isAuthenticated) {
    confirmLoginRedirect();
  } else {
    router.push("/quiz/create");
  }
};

// 공통: 로그인 리다이렉트 확인창
const confirmLoginRedirect = () => {
  confirm.require({
    message: "로그인 창으로 이동하시겠습니까?",
    header: "로그인이 필요한 서비스입니다.    ",
    icon: "pi pi-exclamation-triangle",
    accept: () => {
      router.push("/login");
    },
  });
};

// 단순 이동 함수들
const goToLogin = () => router.push("/login");
const goToCreateQuiz = () => router.push("/quiz/create");
const goToMyPage = () => router.push("/mypage");

// 로그아웃 핸들러
const handleLogout = () => {
  confirm.require({
    message: "로그아웃 하시겠습니까?",
    header: "로그아웃",
    icon: "pi pi-sign-out",
    acceptClass: "p-button-danger",
    accept: () => {
      authStore.logoutUser();
      toast.add({
        severity: "success",
        summary: "로그아웃",
        detail: "로그아웃 되었습니다",
        life: 3000,
      });
    },
  });
};
</script>

<style scoped>
.w-20rem {
  width: 20rem;
}


:deep(.p-button.p-button-text) {
  color: var(--text-main) !important;
}
:deep(.p-button.p-button-text:hover) {
  background-color: var(--bg-surface-hover);
}

.nav-ghost {
  border: none !important;
  background: transparent !important;
  color: var(--text-main) !important;
  font-weight: 600;
}

/* 모든 네비게이션 텍스트에 제목 색상 강제 적용 */
.app-header,
.app-header :deep(*) {
  color: var(--text-main) !important;
}

.nav-ghost:hover {
  background: var(--bg-surface-hover) !important;
  color: var(--text-main) !important;
  border: none !important;
}

.app-header {
  background: transparent !important;
  border-bottom: none !important;
  backdrop-filter: none; /* 블러 효과도 제거 */
}

.app-header :deep(.p-menubar-root-list),
.app-header :deep(.p-menubar-button) {
  color: var(--text-main);
}

.app-header :deep(.p-inputtext) {
  background: var(--bg-surface);
  color: var(--text-main);
  border-color: var(--border);
}

.logo-link {
  height: 44px;
}

.logo-img {
  height: 44px;
  width: auto;
  object-fit: contain;
}

.logo-dark {
  display: none;
}

:global([data-theme="dark"] .logo-light) {
  display: none;
}

:global([data-theme="dark"] .logo-dark) {
  display: block;
}

.logout-btn {
  border: none !important;
  background: transparent !important;
  color: var(--text-main) !important;
  font-weight: 600;
  transition: background-color 0.15s ease, color 0.15s ease, transform 0.12s ease;
}

.logout-btn:hover {
  background: var(--bg-surface-hover) !important;
  color: var(--text-main) !important;
  transform: translateY(-2px);
  box-shadow: none;
}

.text-dark {
  color: var(--text-main);
}

/* 다크 테마 헤더 조화 */
:global([data-theme="dark"] .p-menubar) {
  background-color: transparent !important;
  border-color: transparent !important;
  color: var(--text-main) !important;
}

:global([data-theme="dark"] .p-menubar .p-menuitem-link) {
  color: var(--text-main) !important;
}

:global([data-theme="dark"] .p-menubar .p-menuitem-link:hover) {
  background: var(--bg-surface-hover) !important;
}

:global([data-theme="dark"] .p-menubar .p-inputtext) {
  background: var(--bg-surface) !important;
  color: var(--text-main) !important;
  border-color: var(--border) !important;
}

:global([data-theme="dark"] .p-menubar .p-inputtext:focus) {
  border-color: var(--primary) !important;
  box-shadow: none;
}

:global([data-theme="dark"] .nav-ghost) {
  color: var(--text-main) !important;
}

:global([data-theme="dark"] .nav-ghost:hover) {
  background: var(--bg-surface-hover) !important;
  color: var(--text-main) !important;
}

:global([data-theme="dark"] .logout-btn) {
  color: var(--text-main) !important;
}

:global([data-theme="dark"] .logout-btn:hover) {
  background: var(--bg-surface-hover) !important;
  color: var(--text-main) !important;
  box-shadow: none;
}
</style>
