<template>
  <div class="app-container">
    <NavigationBar />
    <main class="main-content">
      <router-view />
    </main>

    <Toast />
    <ConfirmDialog />
    <button class="theme-toggle" :aria-label="`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`" @click="toggleTheme">
      <i :class="theme === 'light' ? 'pi pi-moon' : 'pi pi-sun'"></i>
    </button>
    <button class="scroll-top" aria-label="Scroll to top" @click="scrollToTop">
      <i class="pi pi-arrow-up"></i>
    </button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useAuthStore } from "@/stores/auth";
import NavigationBar from "@/components/layout/NavigationBar.vue";

const authStore = useAuthStore();
const theme = ref(localStorage.getItem("theme") || "light");

const applyTheme = (mode: string) => {
  const value = mode === "dark" ? "dark" : "light";
  theme.value = value;
  document.documentElement.setAttribute("data-theme", value);
  localStorage.setItem("theme", value);
  // 테마에 관계없이 배경 이미지를 사용 (다크 모드도 오버레이와 함께 적용)
  document.body.classList.add("has-bg-image");
};

const toggleTheme = () => {
  applyTheme(theme.value === "light" ? "dark" : "light");
};

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: "smooth" });
};

onMounted(() => {
  applyTheme(theme.value);
  if (authStore.isAuthenticated) {
    authStore.refreshUser();
  }
});
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 2rem 0;
}

.theme-toggle {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-background-mute);
  color: var(--color-heading);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 1000;
}

.theme-toggle:hover {
  background: var(--color-primary);
  color: #ffffff;
  border-color: var(--color-primary);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
}

.theme-toggle:active {
  transform: translateY(1px);
}

.scroll-top {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-background-mute);
  color: var(--color-heading);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 999;
}

.scroll-top:hover {
  background: var(--color-primary);
  color: #ffffff;
  border-color: var(--color-primary);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.25);
}

.scroll-top:active {
  transform: translateY(1px);
}
</style>
