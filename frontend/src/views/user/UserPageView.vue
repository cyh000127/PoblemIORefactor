<template>

  <div class="user-profile-container">
    <div class="container mx-auto px-4">
      <div v-if="loading" class="text-center py-8">
        <i class="pi pi-spin pi-spinner text-4xl"></i>
      </div>

      <div v-else-if="user" class="flex flex-col gap-6">
        <!-- 프로필 헤더 -->
        <ProfileBackground :user="user" class="shadow-4 profile-card-content p-6">
            <div class="flex flex-col md:flex-row gap-6 items-center md:items-start">
              <UserAvatar :user="user" class="w-32 h-32" />
              <div class="flex-1 w-full text-center md:text-left">
                <!-- 닉네임 & 팔로우 버튼 행 -->
                <div class="flex flex-col md:flex-row justify-between items-center md:items-start">
                  <div>
                    <h1 class="text-3xl font-bold mb-2">{{ user.nickname }}</h1>
                    <p v-if="user.statusMessage" class="text-lg opacity-80 mb-4">
                      {{ user.statusMessage }}
                    </p>
                  </div>
                  
                  <!-- 팔로우 버튼 -->
                  <Button
                    v-if="authStore.isAuthenticated && authStore.user?.id !== user.id"
                    :label="isFollowing ? 'Following' : 'Follow'"
                    :icon="isFollowing ? 'pi pi-check' : 'pi pi-user-plus'"
                    :severity="isFollowing ? 'secondary' : undefined"
                    :outlined="isFollowing"
                    @click="handleFollow"
                    class="shrink-0 mb-4 md:mb-0"
                  />
                </div>
                
                <!-- 통계 영역 -->
                <div class="flex justify-center md:justify-start gap-8 mt-2">
                  <div class="stat-box">
                    <p class="text-2xl font-bold m-0">{{ user.quizCount || 0 }}</p>
                    <p class="text-sm opacity-80 m-0">퀴즈 수</p>
                  </div>
                  <div class="stat-box">
                    <p class="text-2xl font-bold m-0">{{ user.followerCount || 0 }}</p>
                    <p class="text-sm opacity-80 m-0">팔로워</p>
                  </div>
                  <div class="stat-box">
                    <p class="text-2xl font-bold m-0">{{ user.followingCount || 0 }}</p>
                    <p class="text-sm opacity-80 m-0">팔로잉</p>
                  </div>
                </div>
              </div>
            </div>
        </ProfileBackground>

        <!-- 유저 퀴즈 목록 -->
        <div>
          <h2 class="text-2xl font-bold mb-4">퀴즈</h2>
          <div v-if="loadingQuizzes" class="text-center py-8">
            <i class="pi pi-spin pi-spinner text-4xl"></i>
          </div>
          <div v-else-if="quizzes.length === 0" class="text-center py-8 text-color-secondary">
            <p>문제가 아직 없네요.</p>
          </div>
          <div v-else class="quiz-grid-container">
            <div 
              v-for="quiz in quizzes" 
              :key="quiz.id" 
              class="quiz-card cursor-pointer" 
              @click="goToQuiz(quiz.id)"
            >
              <div class="quiz-thumbnail">
                <img 
                  :src="quiz.thumbnailUrl || '/placeholder.svg'" 
                  :alt="quiz.title" 
                  class="quiz-thumbnail-img" 
                />
              </div>
              <div class="quiz-meta">
                <h3 class="quiz-title">{{ quiz.title }}</h3>
                <div class="quiz-stat">
                  <i class="pi pi-heart text-xs"></i>
                  <span>{{ quiz.likeCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useToast } from "primevue/usetoast";
import { useConfirm } from "primevue/useconfirm";
import { useAuthStore } from "@/stores/auth";
import { getUserProfile, followUser, unfollowUser, getUserQuizzes } from "@/api/user";
import UserAvatar from '@/components/common/UserAvatar.vue'
import ProfileBackground from '@/components/user/ProfileBackground.vue';
import { resolveImageUrl } from "@/lib/image";

const route = useRoute();
const router = useRouter();
const toast = useToast();
const confirm = useConfirm();
const authStore = useAuthStore();

const user = ref(null);
const quizzes = ref([]);
const loading = ref(false);
const loadingQuizzes = ref(false);
const isFollowing = ref(false);

const loadUserProfile = async () => {
  loading.value = true;
  try {
    const data = await getUserProfile(Number(route.params.id));
    user.value = data;
    isFollowing.value = data.isFollowedByMe || false;
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: "Failed to load user profile",
      life: 3000,
    });
    router.push("/");
  } finally {
    loading.value = false;
  }
};

const loadUserQuizzes = async () => {
  loadingQuizzes.value = true;
  try {
    quizzes.value = await getUserQuizzes(Number(route.params.id));
    // quizCount는 이제 getUserProfile에서 가져옵니다.
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: "Failed to load quizzes",
      life: 3000,
    });
  } finally {
    loadingQuizzes.value = false;
  }
};

const handleFollow = async () => {
  if (!authStore.isAuthenticated) {
    confirm.require({
      message: "로그인이 필요합니다. 로그인 페이지로 이동하시겠습니까?",
      header: "로그인 필요",
      icon: "pi pi-exclamation-triangle",
      accept: () => {
        router.push("/login");
      },
    });
    return;
  }

  try {
    if (isFollowing.value) {
      await unfollowUser(Number(route.params.id));
      isFollowing.value = false;
      user.value.followerCount = Math.max(0, (user.value.followerCount || 0) - 1);
    } else {
      await followUser(Number(route.params.id));
      isFollowing.value = true;
      user.value.followerCount = (user.value.followerCount || 0) + 1;
    }
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: "Failed to toggle follow",
      life: 3000,
    });
  }
};

const goToQuiz = (quizId: number) => {
  router.push(`/quiz/${quizId}`);
};

onMounted(() => {
  loadUserProfile();
  loadUserQuizzes();
});


</script>

<style scoped>
.user-profile-container {
  min-height: calc(100vh - 200px);
  padding: 1rem 0 3rem;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 통계 */
.stat-box {
  width: 80px;          
  text-align: center;
}

/* 그리드 레이아웃 */
.quiz-grid-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  padding: 0.5rem;
}

@media (max-width: 768px) {
  .quiz-grid-container {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .quiz-grid-container {
    grid-template-columns: 1fr;
  }
}

/* 퀴즈 카드 스타일 */
.quiz-card {
  background: var(--color-background-soft);
  border-radius: 18px;
  border: 1px solid var(--color-border);
  padding: 0.6rem 0.6rem 0.3rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  box-shadow: var(--surface-glow);
  transition: transform 0.2s, box-shadow 0.2s;
}

.quiz-card:hover {
  transform: translateY(-4px);
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg, 0 16px 28px rgba(0, 0, 0, 0.08));
}

.quiz-thumbnail {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 14px;
  background: var(--bg-surface-hover);
}

.quiz-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform 0.2s ease;
}

.quiz-card:hover .quiz-thumbnail-img {
  transform: scale(1.03);
}

.quiz-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0 0.4rem 0.4rem;
}

.quiz-title {
  font-size: 1rem;
  font-weight: 700;
  margin: 0;
  color: var(--color-heading);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quiz-stat {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  background: var(--bg-surface-hover);
  color: var(--color-heading);
  font-size: 0.85rem;
}
</style>
