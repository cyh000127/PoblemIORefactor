<template>
  <div class="comment-section">

    <!-- 댓글 입력 -->
    <CommentInput
      :quiz-id="quizId"
      @submitted="resetAndLoad"
    />

    <hr class="my-4" />

    <!-- 댓글 리스트 -->
    <CommentList
      :comments="comments"
      @updated="resetAndLoad"
    />

    <!-- 무한 스크롤 센티넬 -->
    <div ref="sentinelRef" class="infinite-sentinel">
      <div v-if="loadingMore" class="loading">불러오는 중...</div>
      <div v-else-if="!hasNext && comments.length > 0" class="end">모든 댓글을 불러왔습니다.</div>
    </div>

  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from "vue";
import CommentInput from "./CommentInput.vue";
import CommentList from "./CommentList.vue";
import { fetchComments } from "@/api/comment";

const props = defineProps({
  quizId: { type: Number, required: true },
});

const comments = ref([]);
const page = ref(1);
const size = ref(20);
const hasNext = ref(true);
const loadingMore = ref(false);
const sentinelRef = ref(null);
let observer = null;

const resetAndLoad = async () => {
  comments.value = [];
  page.value = 1;
  hasNext.value = true;
  await loadMore();
};

const loadMore = async () => {
  if (loadingMore.value || !hasNext.value) return;
  loadingMore.value = true;
  try {
    const result = await fetchComments(props.quizId, page.value, size.value);

    const list = Array.isArray(result?.comments)
      ? result.comments
      : Array.isArray(result?.data?.comments)
      ? result.data.comments
      : Array.isArray(result?.content)
      ? result.content
      : Array.isArray(result)
      ? result
      : [];

    comments.value = [...comments.value, ...list];

    hasNext.value = typeof result?.hasNext === "boolean"
      ? result.hasNext
      : list.length === size.value;

    page.value += 1;
  } catch (err) {
    console.error("댓글 불러오기 실패", err);
    hasNext.value = false;
  } finally {
    loadingMore.value = false;
  }
};

watch(
  () => props.quizId,
  () => resetAndLoad()
);

// 최초 로딩
resetAndLoad();

onMounted(async () => {
  observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        loadMore();
      }
    });
  });

  await nextTick();
  if (sentinelRef.value) {
    observer.observe(sentinelRef.value);
  }
});

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect();
  }
});
</script>

<style scoped>
.comment-section {
  padding: 10px 0;
}

.infinite-sentinel {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  color: #6b7280;
  font-size: 13px;
}

.loading {
  color: #374151;
}

.end {
  color: #9ca3af;
}
</style>
