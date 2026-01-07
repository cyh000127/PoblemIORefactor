<template>
  <div class="mypage-container">
    <div class="container mx-auto px-4">
      <!-- 프로필 헤더 -->
      <ProfileHeader :user="me" :quizCount="myQuizCount">
        <template #actions>
          <!-- 관리자 버튼 -->
          <Button v-if="me?.role === 'ROLE_ADMIN'" icon="pi pi-shield" rounded outlined severity="danger" @click="goToAdmin" aria-label="관리자 페이지" v-tooltip.bottom="'관리자 페이지'" />
          <!-- 설정 버튼 (/mypage/edit 이동) -->
          <Button icon="pi pi-cog" rounded outlined @click="goToEditProfile" aria-label="프로필 설정" />
        </template>
      </ProfileHeader>

      <!-- Navbar 스타일의 탭 네비게이션 -->
      <div class="tab-navbar">
        <button :class="['tab-button', { active: activeTab === 'my' }]" @click="activeTab = 'my'">내 퀴즈 목록</button>
        <button :class="['tab-button', { active: activeTab === 'following' }]" @click="activeTab = 'following'">팔로우한 유저들의 퀴즈</button>
        <button :class="['tab-button', { active: activeTab === 'liked' }]" @click="activeTab = 'liked'">좋아요한 퀴즈</button>
      </div>

      <!-- 내 퀴즈 목록 탭 -->
      <div v-show="activeTab === 'my'" class="tab-content">
        <div v-if="myQuizzesLoading" class="text-center py-8">
          <i class="pi pi-spin pi-spinner text-4xl"></i>
        </div>

        <div v-else-if="myQuizzes.length === 0" class="text-center py-8 text-color-secondary">
          <p class="text-xl mb-4">아직 퀴즈가 없네요! 퀴즈를 만들어 친구들과 공유해보세요.</p>
          <Button label="퀴즈 만들기" icon="pi pi-plus" @click="goToCreateQuiz" />
        </div>

        <div v-else class="quiz-grid-container">
          <div v-for="quiz in myQuizzes" :key="quiz.id" class="quiz-card cursor-pointer" @click="goToQuiz(quiz.id)">
            <div class="quiz-thumbnail">
              <img :src="quiz.thumbnailUrl || '/placeholder.svg'" :alt="quiz.title" class="quiz-thumbnail-img" />
              <div v-if="quiz.hidden" class="hidden-badge">
                <i class="pi pi-eye-slash mr-1"></i>
                숨김 처리됨
              </div>
            </div>
            <div class="quiz-meta">
              <h3 class="quiz-title">{{ quiz.title }}</h3>
              <div class="quiz-stat">
                <i class="pi pi-heart text-xs"></i>
                <span>{{ quiz.likeCount || 0 }}</span>
              </div>
            </div>

            <!-- 버튼 클릭 시에는 카드 클릭 이벤트 안 타게 stop -->
            <div class="quiz-actions" @click.stop>
              <Button label="수정" icon="pi pi-pencil" severity="secondary" outlined size="small" class="flex-1 text-xs edit-btn" @click="goToEdit(quiz.id)" />
              <Button label="삭제" icon="pi pi-trash" severity="danger" outlined size="small" class="text-xs" @click="handleDelete(quiz.id)" />
            </div>
          </div>
        </div>
      </div>

      <!-- 팔로우한 유저들의 퀴즈 탭 -->
      <div v-show="activeTab === 'following'" class="tab-content">
        <div v-if="followingQuizzesLoading" class="text-center py-8">
          <i class="pi pi-spin pi-spinner text-4xl"></i>
        </div>

        <div v-else-if="followingQuizzes.length === 0" class="text-center py-8 text-color-secondary">
          <h2 class="text-2xl font-bold mb-4">아직 팔로우하는 유저가 없어요.</h2>
        </div>

        <div v-else class="quiz-grid-container">
          <div v-for="quiz in followingQuizzes" :key="quiz.id" class="quiz-card cursor-pointer" @click="goToQuiz(quiz.id)">
            <div class="quiz-thumbnail">
              <img :src="quiz.thumbnailUrl || '/placeholder.svg'" :alt="quiz.title" class="quiz-thumbnail-img" />
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

      <!-- 좋아요한 퀴즈 탭 -->
      <div v-show="activeTab === 'liked'" class="tab-content">
        <div v-if="likedQuizzesLoading" class="text-center py-8">
          <i class="pi pi-spin pi-spinner text-4xl"></i>
        </div>

        <div v-else-if="likedQuizzes.length === 0" class="text-center py-8 text-color-secondary">
          <h2 class="text-2xl font-bold mb-4">아직 좋아하는 퀴즈가 없네요!</h2>
          <p class="text-xl">퀴즈 풀러 가기</p>
        </div>

        <div v-else class="quiz-grid-container">
          <div v-for="quiz in likedQuizzes" :key="quiz.id" class="quiz-card cursor-pointer" @click="goToQuiz(quiz.id)">
            <div class="quiz-thumbnail">
              <img :src="quiz.thumbnailUrl || '/placeholder.svg'" :alt="quiz.title" class="quiz-thumbnail-img" />
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
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { useToast } from "primevue/usetoast";
import { useConfirm } from "primevue/useconfirm";
import { useAuthStore } from "@/stores/auth";
import { getFollowingQuizzes, getMyLikedQuizzes } from "@/api/user";
import { getMyQuizzes, deleteQuiz } from "@/api/quiz";
import ProfileHeader from "@/components/user/ProfileHeader.vue";
import { resolveImageUrl } from "@/lib/image";

const router = useRouter();
const toast = useToast();
const confirm = useConfirm();
const authStore = useAuthStore();
const me = computed(() => authStore.user);

const goToEditProfile = () => {
  router.push("/mypage/edit");
};

const goToAdmin = () => {
  router.push("/admin");
};

// 활성 탭 상태
const activeTab = ref("my");

// 내 퀴즈 목록
const myQuizzes = ref([]);
const myQuizCount = computed(() => myQuizzes.value.length);
const myQuizzesLoading = ref(false);

// 팔로우한 유저들의 퀴즈들
const followingQuizzes = ref([]);
const followingQuizzesLoading = ref(false);

// 좋아요한 퀴즈들
const likedQuizzes = ref([]);
const likedQuizzesLoading = ref(false);

// 내 퀴즈 목록 로드
const loadMyQuizzes = async () => {
  myQuizzesLoading.value = true;
  try {
    myQuizzes.value = await getMyQuizzes();
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: "Failed to load my quizzes",
      life: 3000,
    });
  } finally {
    myQuizzesLoading.value = false;
  }
};

// 팔로우한 유저들의 퀴즈들 로드
const loadFollowingQuizzes = async () => {
  followingQuizzesLoading.value = true;
  try {
    followingQuizzes.value = await getFollowingQuizzes();
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: "Failed to load following users' quizzes",
      life: 3000,
    });
  } finally {
    followingQuizzesLoading.value = false;
  }
};

// 좋아요한 퀴즈들 로드
const loadLikedQuizzes = async () => {
  likedQuizzesLoading.value = true;
  try {
    likedQuizzes.value = await getMyLikedQuizzes();
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: "Failed to load liked quizzes",
      life: 3000,
    });
  } finally {
    likedQuizzesLoading.value = false;
  }
};

// 모든 데이터 로드
const loadAllData = async () => {
  await Promise.all([loadMyQuizzes(), loadFollowingQuizzes(), loadLikedQuizzes()]);
};

const goToQuiz = (quizId: number) => {
  router.push(`/quiz/${quizId}`);
};

const goToEdit = (quizId: number) => {
  router.push(`/quiz/edit/${quizId}`);
};

const goToCreateQuiz = () => {
  router.push("/quiz/create");
};

const handleDelete = (quizId: number) => {
  confirm.require({
    message: "한번 지우면 되돌릴 수 없어요!",
    header: "진짜 퀴즈를 지울거에요? ",
    icon: "pi pi-exclamation-triangle",
    accept: async () => {
      try {
        await deleteQuiz(quizId);
        toast.add({
          severity: "success",
          summary: "Success",
          detail: "퀴즈가 삭제되었습니다",
          life: 3000,
        });
        await Promise.all([loadMyQuizzes(), authStore.refreshUser()]);
      } catch (error: any) {
        toast.add({
          severity: "error",
          summary: "Error",
          detail: "퀴즈 삭제에 실패했습니다",
          life: 3000,
        });
      }
    },
  });
};

const formatDate = (dateString: string) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleDateString();
};

onMounted(() => {
  loadAllData();
});
</script>

<style scoped>
/* 프로필 카드 내부 텍스트 색상 강제 상속 */
.profile-card-content p,
.profile-card-content span,
.profile-card-content h1 {
  color: inherit;
}

/* 통계 부분 */
.stat-box {
  width: 60px;
  text-align: center;
}

.mypage-container {
  min-height: calc(100vh - 200px);
  padding: 1rem 0 3rem;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.tab-navbar {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--surface-border);
  margin-bottom: 2rem;
  background: var(--surface-ground);
}

.tab-button {
  flex: 1;
  padding: 1rem 1.5rem;
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-color-secondary);
  transition: all 0.3s ease;
  position: relative;
  border-radius: 12px 12px 0 0;
}

.tab-button:hover {
  background: var(--surface-hover);
  color: var(--text-color);
}

.tab-button.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
  font-weight: 600;
}

.tab-content {
  padding: 1rem 0;
  animation: fadeIn 0.3s ease;
  /* background: var(--color-background-soft); */
  /* border-radius: 0 0 18px 18px; */
  /* box-shadow: var(--surface-glow); */
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

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

.quiz-thumbnail {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 14px;
  position: relative;
  background: var(--bg-surface-hover);
}

.quiz-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  image-rendering: auto;
  transition: transform 0.2s ease;
}

.hidden-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: rgba(0, 0, 0, 0.7);
  color: #ef4444;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: bold;
  display: flex;
  align-items: center;
  z-index: 10;
  border: 1px solid rgba(239, 68, 68, 0.5);
  backdrop-filter: blur(4px);
}

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
  box-shadow: var(--shadow-lg, 0 16px 28px rgba(0, 0, 0, 0.08));
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

.quiz-actions {
  display: flex;
  gap: 0.5rem;
  padding: 0 0.4rem 0.4rem;
}

:global([data-theme="dark"] .edit-btn) {
  color: var(--text-main) !important;
  border-color: var(--text-main) !important;
}
</style>
