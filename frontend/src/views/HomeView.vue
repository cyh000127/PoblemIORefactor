<template>
  <div class="home-container">
    <div class="container page-container">
      <!-- ✅ 검색바(그대로 유지) -->
      <div class="home-search-row">
        <div class="search-bar-wide w-full">
          <Button class="search-filter-btn" icon="pi pi-filter" rounded text @click="toggleFilterPanel($event)"></Button>
          <OverlayPanel ref="filterPanel">
            <div class="search-filter-menu">
              <div
                v-for="opt in filterOptions"
                :key="opt.value"
                class="search-filter-item"
                :class="{ 'is-active': searchSort === opt.value }"
                @click="selectSearchSort(opt.value)"
              >
                <i v-if="searchSort === opt.value" class="pi pi-check"></i>
                <span>{{ opt.label }}</span>
              </div>
            </div>
          </OverlayPanel>

          <span class="p-input-icon-right search-input-wrapper">
            <InputText
              v-model="searchKeyword"
              placeholder="검색어를 입력하세요."
              class="search-input w-full"
              @keyup.enter="handleSearch"
            />
            <i class="pi pi-search search-icon" @click="handleSearch"></i>
          </span>
        </div>
      </div>

      <!-- ✅ 메인: (왼쪽) 콘텐츠 + (오른쪽) 랭킹 사이드바 -->
      <div class="home-layout">
        <!-- 왼쪽: 필터 + 카드 + 페이지네이션 -->
        <section class="home-main">
          <div class="home-controls">
            <div class="flex gap-1 filter-group">
              <Button
                icon="pi pi-heart"
                :label="'인기'"
                :class="['filter-button', { 'is-active': sort === 'popular' }]"
                :severity="sort === 'popular' ? undefined : 'secondary'"
                :outlined="sort !== 'popular'"
                @click="sort = 'popular'"
              />
              <Button
                icon="pi pi-eye"
                :label="'조회'"
                :class="['filter-button', { 'is-active': sort === 'views' }]"
                :severity="sort === 'views' ? undefined : 'secondary'"
                :outlined="sort !== 'views'"
                @click="sort = 'views'"
              />
              <Button
                icon="pi pi-clock"
                :label="'최신'"
                :class="['filter-button', { 'is-active': sort === 'latest' }]"
                :severity="sort === 'latest' ? undefined : 'secondary'"
                :outlined="sort !== 'latest'"
                @click="sort = 'latest'"
              />
            </div>
          </div>

          <div v-if="loading" class="quiz-grid-container mb-6">
            <div v-for="i in 8" :key="i" class="quiz-card p-3">
              <Skeleton height="200px" class="mb-3 rounded-xl" style="background-color: var(--skeleton-bg)"></Skeleton>
              <Skeleton width="60%" height="1.5rem" class="mb-2" style="background-color: var(--skeleton-bg)"></Skeleton>
              <div class="flex gap-2">
                 <Skeleton width="4rem" height="1rem" style="background-color: var(--skeleton-bg)"></Skeleton>
                 <Skeleton width="4rem" height="1rem" style="background-color: var(--skeleton-bg)"></Skeleton>
              </div>
            </div>
          </div>

          <div v-else-if="quizzes.length === 0" class="text-center py-8 empty-state">
            <p class="text-color-secondary text-xl">퀴즈를 찾지 못했어요.</p>
          </div>

          <div v-else class="quiz-grid-container mb-6">
            <div v-for="quiz in quizzes" :key="quiz.id" class="quiz-card cursor-pointer" @click="goToQuiz(quiz.id)">
              <div class="quiz-thumbnail">
                <img :src="quiz.thumbnailUrl || '/placeholder.svg'" :alt="quiz.title" class="quiz-thumbnail-img" />
              </div>
              <div class="quiz-body">
                <h3 class="quiz-title">{{ quiz.title }}</h3>
                <div class="quiz-meta-row">
                  <span class="quiz-meta-item">
                    <i class="pi pi-eye"></i>
                    <span>{{ quiz.playCount || 0 }}</span>
                  </span>
                  <span class="quiz-meta-item">
                    <i class="pi pi-heart"></i>
                    <span>{{ quiz.likeCount || 0 }}</span>
                  </span>
                  <span class="quiz-meta-item">
                    <i class="pi pi-comments"></i>
                    <span>{{ quiz.commentCount ?? 0 }}</span>
                  </span>
                </div>
                <p class="quiz-description">{{ quiz.description || quiz.desc || quiz.summary || "설명이 없습니다." }}</p>
              </div>
            </div>
          </div>

          <Paginator
            v-if="totalPages > 1"
            :first="(currentPage - 1) * pageSize"
            :rows="pageSize"
            :totalRecords="totalElements"
            @page="onPageChange"
            class="mb-6"
          />
        </section>

        <!-- 오른쪽: 랭킹 사이드바(레이아웃 흐름 안에 포함) -->
        <aside class="home-aside">
          <div class="ranking-panel">
            <LiveRankingWidget />
            
            <div class="mt-6">
                <HomeChallengeWidget />
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import { useToast } from "primevue/usetoast";
import { getQuizzes } from "@/api/quiz";
import OverlayPanel from "primevue/overlaypanel";
import LiveRankingWidget from "@/components/LiveRankingWidget.vue";
import HomeChallengeWidget from "@/components/challenge/HomeChallengeWidget.vue";
import Paginator from "primevue/paginator";
import Skeleton from "primevue/skeleton";

const router = useRouter();
const toast = useToast();

const quizzes = ref<any[]>([]);
const loading = ref(false);
const searchKeyword = ref("");

type SortOption = "popular" | "latest" | "views";
const searchSort = ref<SortOption>("popular");
const sort = ref<SortOption>("popular");

const filterOptions: { label: string; value: SortOption }[] = [
  { label: "인기순", value: "popular" },
  { label: "조회순", value: "views" },
  { label: "최신순", value: "latest" },
];

const filterPanel = ref<any>();
const currentPage = ref(1);
const pageSize = ref(12);
const totalPages = ref(0);
const totalElements = ref(0);

const loadQuizzes = async () => {
  loading.value = true;
  try {
    const response = await getQuizzes({
      page: currentPage.value,
      size: pageSize.value,
      sort: sort.value,
      keyword: searchKeyword.value,
    });
    quizzes.value = response.content || [];
    totalPages.value = response.totalPages || 0;
    totalElements.value = response.totalElements || 0;
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: "Failed to load quizzes",
      life: 3000,
    });
  } finally {
    loading.value = false;
  }
};

const goToQuiz = (quizId: number) => {
  router.push(`/quiz/${quizId}`);
};

const handleSearch = () => {
  // ✅ 비동기 검색 (페이지 이동 X)
  currentPage.value = 1;
  // 검색바의 정렬 옵션을 메인 정렬 상태에 반영 (선택 사항, 사용자 경험상 동기화 추천)
  if (searchSort.value) {
      sort.value = searchSort.value;
  }
  loadQuizzes();
};

const onPageChange = (event: any) => {
  currentPage.value = event.page + 1;
  loadQuizzes();
};

const toggleFilterPanel = (event: Event) => {
  filterPanel.value?.toggle(event);
};

const selectSearchSort = (value: SortOption) => {
  searchSort.value = value;
  filterPanel.value?.hide();
  handleSearch();
};

watch(sort, () => {
  currentPage.value = 1;
  loadQuizzes();
});

onMounted(() => {
  loadQuizzes();
});
</script>

<style scoped>
.home-container {
  min-height: calc(100vh - 200px);
  padding: 1rem 0 2rem;
}

.container {
  width: 100%;
}

.page-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 12px;
  min-width: 0;
}

/* ✅ 검색바 */
.home-search-row {
  margin-top: 1.5rem;
  margin-bottom: 1.25rem;
  display: flex;
  justify-content: flex-start;
  width: 100%;
}

.search-bar-wide {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 0.75rem;
}

.search-filter-btn.p-button {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  background: var(--bg-surface); /* --color-bg-card에서 --bg-surface(흰색)로 변경됨 */
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
}

:global([data-theme="dark"] .search-filter-btn.p-button) {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  color: var(--text-main);
}

.search-input-wrapper {
  flex: 1;
  position: relative;
}

.search-input-wrapper .search-input {
  width: 100%;
  border-radius: 999px;
  padding-left: 1.25rem;
  padding-right: 2.5rem;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.search-input-wrapper .search-icon {
  cursor: pointer;
  position: absolute;
  right: 30px;
  top: 50%;
  transform: translateY(-50%);
}

:global([data-theme="dark"] .search-input-wrapper .search-input) {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  color: var(--text-main);
  box-shadow: none;
}

/* ✅ 핵심: 2컬럼 레이아웃 (왼쪽: 카드 / 오른쪽: 랭킹) */
.home-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  gap: 18px;
  /* align-items: start; (제거) */
}

.home-main {
  min-width: 0;
}

.home-aside {
  position: relative;
}

/* ✅ 랭킹 패널은 "컨테이너 안"에서 카드처럼 보이게 */
.ranking-panel {
  position: sticky;
  top: 24px;
  z-index: 10;
  height: fit-content;
}

/* ✅ 화면 좁아지면 랭킹을 아래로 내리거나 숨김(선택) */
@media (max-width: 1200px) {
  .home-layout {
    grid-template-columns: 1fr;
  }
  .ranking-panel {
    position: static;
    margin-top: 10px;
  }
}

/* ✅ 필터 줄 */
.home-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem 0 1.5rem;
  width: 100%;
}

.filter-group {
  display: inline-flex;
  gap: 0.5rem;
  flex-wrap: nowrap;
}

.filter-button.p-button {
  color: var(--color-text-muted);
  background: var(--bg-surface); /* --color-bg-card에서 --bg-surface(흰색)로 변경됨 */
  border-color: var(--color-border);
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 18px;
  line-height: 1;
}

:global(.filter-button.p-button.is-active) {
  background: var(--bg-surface-hover);
  border-color: var(--primary);
  color: var(--primary);
  box-shadow: none;
}

:global([data-theme="dark"] .filter-button.p-button.is-active) {
  background: var(--bg-surface-hover);
  border-color: var(--primary);
  color: var(--primary);
  box-shadow: none;
}

/* ✅ 카드: 4열 → 3열 */
.quiz-grid-container {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  width: 100%;
}

@media (max-width: 960px) {
  .quiz-grid-container {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .quiz-grid-container {
    grid-template-columns: 1fr;
  }
}

/* 카드 UI(기존 유지) */
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

:global([data-theme="dark"] .quiz-thumbnail) {
  background: var(--bg-main);
  border: 1px solid var(--border);
}

.quiz-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform 0.2s ease;
}

.quiz-card {
  background: var(--color-background-soft);
  border-radius: 18px;
  border: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

:global([data-theme="dark"] .quiz-card) {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  box-shadow: none;
}

.quiz-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
  border-color: rgba(148, 163, 184, 0.6);
}

.quiz-card:hover .quiz-thumbnail-img {
  transform: scale(1.03);
}

.quiz-body {
  padding: 0 0.4rem 0.6rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.quiz-title {
  font-size: 1rem;
  font-weight: 700;
  margin: 0 0 0.3rem;
  color: var(--color-heading);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.quiz-meta-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.quiz-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}

.quiz-description {
  margin: 0.1rem 0 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-state {
  background: var(--bg-surface);
  border-radius: 14px;
  padding: 2rem;
}

.search-filter-menu {
  min-width: 160px;
  padding: 6px 0;
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: 8px;
}

.search-filter-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-heading);
}

.search-filter-item:hover {
  background-color: var(--color-background-mute);
}

.search-filter-item.is-active {
  font-weight: 600;
}

:root {
  --skeleton-bg: rgba(0, 0, 0, 0.06);
}

[data-theme="dark"] {
  --skeleton-bg: rgba(255, 255, 255, 0.08);
}
</style>


