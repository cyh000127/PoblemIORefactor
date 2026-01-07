<template>
  <div class="quiz-detail-container">
    <div class="container mx-auto px-4">
      <div v-if="loading" class="text-center py-8">
        <i class="pi pi-spin pi-spinner text-4xl"></i>
      </div>

      <div v-else-if="quiz" class="flex flex-col gap-6">
        <!-- 1. 이미지 섹션 (분리됨) -->
        <div class="thumbnail-frame">
          <img :src="quiz.thumbnailUrl || '/placeholder.svg'" :alt="quiz.title" class="thumbnail-image" />
        </div>

        <!-- 2. 콘텐츠 카드 (제목 & 설명) -->
        <Card class="quiz-card">
          <template #content>
            <div class="flex flex-col gap-6 items-center text-center py-4">
              <!-- Title & Description -->
              <div class="flex flex-col gap-3 max-w-2xl">
                <h1 class="text-3xl font-extrabold m-0 leading-tight">
                  {{ quiz.title }}
                </h1>
                <p class="text-color-secondary text-lg leading-relaxed whitespace-pre-line">
                  {{ quiz.description }}
                </p>
              </div>

              <!-- 통합 메타 정보 행 (중앙 정렬) -->
              <div class="flex flex-wrap items-center justify-center gap-4 mt-2 text-color-secondary text-sm">
                <!-- 작성자 -->
                <div class="flex items-center gap-2 cursor-pointer hover:text-primary transition-colors" @click="openAuthorPopover">
                   <UserAvatar :user="quiz.author" size="small" />
                   <span class="font-bold">{{ quiz.author?.nickname }}</span>
                </div>
                
                <span class="opacity-30">|</span>

                <!-- 통계 -->
                <span>문제 {{ quiz.questions?.length || 0 }}개</span>
                
                <span class="opacity-30">|</span>
                
                <span>조회 {{ quiz.playCount || 0 }}</span>

                <span class="opacity-30">|</span>

                <!-- 좋아요 버튼 (심플) -->
                <div 
                  class="flex items-center gap-1 transition-colors"
                  :class="isMyQuiz ? 'opacity-50 cursor-default' : 'cursor-pointer hover:text-red-500'"
                  @click.stop="!isMyQuiz && handleLike()"
                >
                  <i :class="isLiked ? 'pi pi-heart-fill text-red-500' : 'pi pi-heart'"></i>
                  <span>{{ quiz.likeCount || 0 }}</span>
                </div>
              </div>

              <!-- 시작 버튼 -->
              <div class="start-button-grid mt-6 w-full max-w-md">
                <Button
                  v-for="option in questionOptions"
                  :key="option"
                  :label="getOptionLabel(option)"
                  icon="pi pi-play"
                  size="large"
                  class="start-button font-bold text-lg py-3 shadow-sm w-full"
                  :loading="startLoadingOption === option"
                  @click="startQuizWithLimit(option)"
                />
              </div>
            </div>
          </template>
        </Card>

        <!-- 댓글 섹션 (퀴즈 시작 화면에서 노출) -->
        <Card>
          <template #content>
            <div class="flex justify-between items-center mb-3">
              <h3 class="text-xl font-bold m-0">댓글</h3>
              <span class="text-sm text-color-secondary"
                >퀴즈 시작 전에 자유롭게 소통하세요</span
              >
            </div>
            <CommentSection :quiz-id="quizId" />
          </template>
        </Card>

        <!-- ✅ 유저 팝오버 컴포넌트 -->
        <UserPopover ref="userPopoverRef" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useToast } from "primevue/usetoast";
import { useConfirm } from "primevue/useconfirm";
import { useAuthStore } from "@/stores/auth";
import { useQuizStore } from "@/stores/quiz";
import { getQuiz, getQuizQuestions, likeQuiz, unlikeQuiz } from "@/api/quiz";
import { followUser, unfollowUser } from "@/api/user";
import UserPopover from "@/components/common/UserPopover.vue";
import UserAvatar from '@/components/common/UserAvatar.vue' // 유저 아바타 불러오기 
import CommentSection from "@/components/comment/CommentSection.vue";

const route = useRoute();
const router = useRouter();
const toast = useToast();
const confirm = useConfirm();
const authStore = useAuthStore();
const quizStore = useQuizStore();

const quiz = ref<any | null>(null);
const loading = ref(false);
const isLiked = ref(false);
const isFollowed = ref(false);
const quizId = computed(() => Number(route.params.id));
const totalQuestionCount = computed(() =>
  quiz.value?.questions?.length ??
  quiz.value?.questionCount ??
  0
);
const questionOptions = computed(() => {
  const total = totalQuestionCount.value;
  if (total >= 50) return [10, 20, 30, 50];
  if (total >= 30) return [10, 20, 30];
  if (total >= 20) return [10, 20];
  if (total >= 10) return [Math.min(10, total)];
  return [Math.max(total, 1)];
});
const startLoadingOption = ref<number | null>(null);

// 팝오버 ref
const userPopoverRef = ref<any | null>(null);

// 로그인한 유저 ID
const currentUserId = computed(() => authStore.user?.id ?? null);

// 내가 만든 퀴즈인지 여부 (백엔드 QuizResponse.userId 사용)
const isMyQuiz = computed(() => {
  if (!quiz.value || !currentUserId.value) return false;
  return quiz.value.userId === currentUserId.value;
});

const loadQuiz = async () => {
  loading.value = true;
  try {
    const data = await getQuiz(Number(route.params.id));
    quiz.value = data;
    isLiked.value = data.isLikedByMe || false;
    isFollowed.value = data.isFollowedByMe || false;
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: "Failed to load quiz",
      life: 3000,
    });
  } finally {
    loading.value = false;
  }
};

// 작성자 프로필 클릭 → 팝오버 오픈
const openAuthorPopover = (event: MouseEvent) => {
  if (!quiz.value?.author?.id) return;
  userPopoverRef.value?.open(event, quiz.value.author.id);
};

const handleLike = async () => {
  // 1. 내 퀴즈면 프론트에서도 바로 막기
  if (isMyQuiz.value) {
    toast.add({
      severity: "warn",
      summary: "알림",
      detail: "내가 만든 퀴즈에는 좋아요를 누를 수 없습니다.",
      life: 3000,
    });
    return;
  }

  // 2. 비로그인인 경우 로그인 유도 (기존 로직 유지)
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
    if (isLiked.value) {
      await unlikeQuiz(Number(route.params.id));
      isLiked.value = false;
      quiz.value.likeCount = Math.max(0, (quiz.value.likeCount || 0) - 1);
    } else {
      await likeQuiz(Number(route.params.id));
      isLiked.value = true;
      quiz.value.likeCount = (quiz.value.likeCount || 0) + 1;
    }
  } catch (error: any) {
    if (error.code === "Q003") {
      toast.add({
        severity: "warn",
        summary: "알림",
        detail: error.message || "내가 만든 퀴즈에는 좋아요를 누를 수 없습니다.",
        life: 3000,
      });
      return;
    }

    toast.add({
      severity: "error",
      summary: "Error",
      detail: error.message || "Failed to toggle like",
      life: 3000,
    });
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
    if (isFollowed.value) {
      await unfollowUser(quiz.value.author.id);
      isFollowed.value = false;
    } else {
      await followUser(quiz.value.author.id);
      isFollowed.value = true;
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

const startQuizWithLimit = async (count: number) => {
  if (!quiz.value || startLoadingOption.value !== null) return;
  startLoadingOption.value = count;
  try {
    const questions = await getQuizQuestions(quizId.value, count);
    quizStore.startQuiz(quiz.value, questions);
    router.push(`/quiz/${route.params.id}/play`);
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: error?.message || "퀴즈 시작에 실패했습니다.",
      life: 3000,
    });
  } finally {
    startLoadingOption.value = null;
  }
};

onMounted(() => {
  loadQuiz();
});

// 버튼 라벨 헬퍼
const getOptionLabel = (option: number) => {
  const total = totalQuestionCount.value;
  // 10 미만 또는 10~19 구간: 숫자 대신 "문제 풀기" 노출
  if (total < 20) return "문제 풀기";
  return `${option}개 풀기`;
};
</script>

<style scoped>
.quiz-detail-container {
  min-height: calc(100vh - 200px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.aspect-video {
  aspect-ratio: 16 / 9;
}

.quiz-card {
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid var(--color-border);
  box-shadow: none; /* User prefers flat design usually, or keep minimal */
}

.quiz-card .p-card-content {
  padding: 1.5rem;
  background: var(--color-background-soft);
  color: var(--color-heading);
}

:deep(.quiz-card .p-card-title),
:deep(.quiz-card .p-card-subtitle),
:deep(.quiz-card p) {
  color: var(--color-heading);
}

.thumbnail-frame {
  background: transparent;
  display: flex;
  justify-content: center;
  padding: 2rem 1rem;
}

.thumbnail-image {
  width: 606px;
  height: 344px;
  max-width: 100%; /* Mobile responsive */
  object-fit: cover; /* Fill the area */
  border-radius: 12px;
  border: 3px solid var(--text-main);
  background: var(--bg-surface); /* Fallback/Loading bg */
}
.start-button-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); /* Slightly wider buttons */
  gap: 1rem;
  max-width: 800px;
  margin: 0 auto; /* Center the grid */
  width: 100%;
}
.start-button {
  width: 100%;
}
.view-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: var(--bg-surface-hover);
  color: var(--text-main);
  font-size: 0.9rem;
}


</style>
