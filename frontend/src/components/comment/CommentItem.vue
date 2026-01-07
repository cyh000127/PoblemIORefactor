<template>
  <div class="comment-item">
    <div class="meta">
      <div 
        class="author"
        :class="{ 'cursor-pointer hover:underline': !!comment.userId }"
        @click="onAuthorClick"
      >
        {{ comment.nickname || "익명" }}
      </div>
      <div class="date">{{ formatDate(comment.createdAt) }}</div>
    </div>

    <!-- 내용 -->
    <p v-if="!isEditing" class="content">{{ comment.content }}</p>

    <!-- 인라인 편집 영역 -->
    <div v-else class="edit-area">
      <textarea
        v-model="editContent"
        class="edit-textarea"
        rows="3"
        placeholder="내용을 수정하세요"
        :disabled="saving"
      />
      <div class="edit-row">
        <input
          v-if="isGuest"
          v-model="password"
          type="password"
          class="guest-password"
          placeholder="비밀번호 (게스트 전용)"
          :disabled="saving"
        />
        <span v-if="error" class="error-text">{{ error }}</span>
      </div>
      <div class="edit-actions">
        <Button
          label="취소"
          severity="secondary"
          size="small"
          :disabled="saving"
          @click="cancelEdit"
        />
        <Button
          label="저장"
          size="small"
          :loading="saving"
          @click="saveEdit"
        />
      </div>
    </div>

    <!-- 하단 버튼 -->
    <div class="actions">
      <!-- 좋아요 -->
      <button @click="toggleLike" class="action-btn like-btn">
        <span :class="heartClass" :style="heartStyle">♥</span>
        <span>{{ comment.likeCount }}</span>
      </button>

      <!-- 수정 -->
      <button
        v-if="canEdit"
        class="action-btn text-blue-600"
        @click="startEdit"
      >
        수정
      </button>

      <!-- 삭제 -->
      <button
        v-if="canDelete"
        class="action-btn text-red-600"
        @click="remove"
      >
        삭제
      </button>

      <!-- 답글 토글 -->
      <button
        v-if="replyCountToShow > 0 || repliesOpen || showReplyForm"
        class="action-btn text-teal"
        @click="toggleReplies"
      >
        {{ repliesOpen ? "답글 접기" : `답글 ${replyCountToShow}개 보기` }}
      </button>

      <button class="action-btn text-teal" @click="toggleReplyForm">
        {{ showReplyForm ? "답글 취소" : "답글 달기" }}
      </button>
    </div>

    <!-- 답글 입력 -->
    <div v-if="showReplyForm" class="reply-input">
      <CommentInput
        :quiz-id="comment.quizId"
        :parent-comment-id="resolveCommentId()"
        button-label="답글 작성"
        placeholder="답글을 입력하세요..."
        @submitted="handleReplySubmitted"
      />
    </div>

    <!-- 답글 리스트 -->
    <div v-if="repliesOpen" class="replies">
      <div v-if="repliesLoading" class="reply-loading">불러오는 중...</div>
      <CommentItem
        v-else
        v-for="(reply, idx) in replies"
        :key="reply.id ?? reply.commentId ?? idx"
        :comment="reply"
        @updated="emit('updated')"
      />
      <div v-if="repliesOpen && !repliesLoading && replies.length === 0" class="no-replies">
        아직 답글이 없습니다.
      </div>
    </div>

    
    <!-- 유저 팝오버 -->
    <UserPopover ref="userPopoverRef" />
  </div>

  <!-- 게스트 비밀번호 확인 모달 -->
  <Dialog
    v-model:visible="showPasswordModal"
    :header="modalMode === 'delete' ? '댓글 삭제' : '비밀번호 확인'"
    modal
    :closable="false"
    :dismissableMask="!deleting"
    :style="{ width: '22rem' }"
  >
    <div class="flex flex-col gap-3">
      <p class="m-0 text-sm text-gray-600">
        {{ 
          isAdmin 
            ? '관리자 권한으로 댓글을 삭제하시겠습니까?' 
            : `게스트로 작성된 댓글입니다. ${modalMode === 'delete' ? '삭제' : '수정'}하려면 비밀번호를 입력하세요.`
        }}
      </p>
      <InputText
        v-if="!isAdmin"
        v-model="password"
        type="password"
        placeholder="비밀번호"
        :disabled="deleting"
        @keyup.enter.exact.prevent="confirmModal"
      />
      <div v-if="error" class="text-sm text-red-500">{{ error }}</div>
      <div class="flex justify-end gap-2">
        <Button
          label="취소"
          severity="secondary"
          :disabled="deleting"
          @click="closeModal"
        />
        <Button
          :label="modalMode === 'delete' ? '삭제' : '확인'"
          :severity="modalMode === 'delete' ? 'danger' : 'primary'"
          :loading="deleting"
          @click="confirmModal"
        />
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { computed, ref } from "vue";
import { toggleCommentLike, deleteComment, updateComment, fetchReplies } from "@/api/comment";
import { useAuthStore } from "@/stores/auth";
import CommentInput from "./CommentInput.vue";
import UserPopover from "@/components/common/UserPopover.vue";

defineOptions({ name: "CommentItem" });

const props = defineProps({
  comment: { type: Object, required: true },
});

const emit = defineEmits(["updated"]);
const authStore = useAuthStore();

const likedByMe = computed(() =>
  props.comment.likedByMe ??
  props.comment.liked ??
  props.comment.isLiked ??
  props.comment.liked_by_me ??
  props.comment.liked_by_user
);
const isGuest = computed(
  () => !props.comment.userId && (props.comment.isGuest ?? true)
);
const isAdmin = computed(() => authStore.user?.role === 'ROLE_ADMIN');

const canEdit = computed(() => {
  if (props.comment.userId) {
    return !!props.comment.mine;
  }
  return true;
});

const canDelete = computed(() => {
  if (isAdmin.value) return true;
  if (props.comment.userId) {
    return !!props.comment.mine;
  }
  return true;
});

const heartClass = computed(() =>
  likedByMe.value ? "liked-heart" : "unliked-heart"
);
const heartStyle = computed(() =>
  likedByMe.value
    ? { color: "#ef4444" } // 강제 빨간색 (다크/라이트 동일)
    : { color: "var(--color-text)" }
);
const replyCountToShow = computed(() => {
  const base =
    props.comment.replyCount ??
    props.comment.repliesCount ??
    props.comment.childrenCount ??
    0;
  return repliesLoaded.value ? replies.value.length : base;
});

const showPasswordModal = ref(false);
const deleting = ref(false);
const password = ref("");
const error = ref("");
const isEditing = ref(false);
const editContent = ref(props.comment.content ?? "");
const saving = ref(false);
const replies = ref([]);
const repliesOpen = ref(false);
const repliesLoaded = ref(false);
const repliesLoading = ref(false);
const showReplyForm = ref(false);
const modalMode = ref("delete"); // 'delete' | 'edit'
const authorizedForEdit = ref(false);

function resolveCommentId() {
  return props.comment.id ?? props.comment.commentId;
}

function formatDate(str) {
  return new Date(str).toLocaleString("ko-KR");
}

/** 좋아요 토글 */
async function toggleLike() {
  const commentId = resolveCommentId();
  if (!commentId) return;

  if (!authStore.isAuthenticated) {
    window.alert("로그인 후 좋아요를 누를 수 있습니다.");
    return;
  }

  await toggleCommentLike(commentId);
  emit("updated");
}

/** 삭제 */
async function remove() {
  const commentId = resolveCommentId();
  if (!commentId) return;

  // 관리자 우회
  if (isAdmin.value) {
     modalMode.value = "delete";
     showPasswordModal.value = true;
     return;
  } else {
    // 작성자만 삭제 허용
    if (props.comment.userId && !props.comment.mine) {
      return;
    }

    // 게스트면 모달로 비밀번호 입력
    if (isGuest.value) {
      modalMode.value = "delete";
      showPasswordModal.value = true;
      return;
    }
  }

  await deleteComment(commentId);
  emit("updated");
}

async function confirmModal() {
  if (modalMode.value === "edit") {
    if (!password.value.trim()) {
      error.value = "비밀번호를 입력하세요.";
      return;
    }
    authorizedForEdit.value = true;
    isEditing.value = true;
    error.value = "";
    showPasswordModal.value = false;
    return;
  }

  // 관리자는 비밀번호 불필요
  if (!isAdmin.value && !password.value.trim()) {
    error.value = "비밀번호를 입력하세요.";
    return;
  }

  const commentId = resolveCommentId();
  if (!commentId) return;

  deleting.value = true;
  error.value = "";
  try {
    await deleteComment(commentId, password.value.trim());
    emit("updated");
    closeModal();
  } catch (err) {
    error.value = "삭제에 실패했습니다. 비밀번호를 확인하세요.";
  } finally {
    deleting.value = false;
  }
}

function closeModal() {
  if (deleting.value) return;
  showPasswordModal.value = false;
  password.value = "";
  error.value = "";
}

function startEdit() {
  // 작성자만 편집 허용
  if (props.comment.userId && !props.comment.mine) {
    return;
  }
  isEditing.value = true;
  editContent.value = props.comment.content ?? "";
  error.value = "";
  password.value = "";
  if (isGuest.value) {
    isEditing.value = false;
    modalMode.value = "edit";
    showPasswordModal.value = true;
  } else {
    authorizedForEdit.value = true;
  }
}

function cancelEdit() {
  if (saving.value) return;
  isEditing.value = false;
  editContent.value = props.comment.content ?? "";
  error.value = "";
  password.value = "";
}

async function saveEdit() {
  if (!editContent.value.trim()) {
    error.value = "내용을 입력하세요.";
    return;
  }

  const commentId = resolveCommentId();
  if (!commentId) return;

  if (isGuest.value) {
    if (!authorizedForEdit.value && !password.value.trim()) {
      error.value = "비밀번호를 입력하세요.";
      return;
    }
    if (!password.value.trim()) {
      error.value = "비밀번호를 입력하세요.";
      return;
    }
  }

  saving.value = true;
  error.value = "";
  try {
    await updateComment(commentId, {
      content: editContent.value.trim(),
      ...(isGuest.value ? { password: password.value.trim() } : {}),
    });
    emit("updated");
    isEditing.value = false;
  } catch (err) {
    error.value = "수정에 실패했습니다. 비밀번호를 확인하세요.";
  } finally {
    saving.value = false;
  }
}

async function loadReplies() {
  if (repliesLoading.value || repliesLoaded.value) return;
  repliesLoading.value = true;
  try {
    const result = await fetchReplies(resolveCommentId());
    replies.value = Array.isArray(result) ? result : [];
    repliesLoaded.value = true;
  } catch (err) {
    console.error("대댓글 불러오기 실패", err);
  } finally {
    repliesLoading.value = false;
  }
}

async function toggleReplies() {
  repliesOpen.value = !repliesOpen.value;
  if (repliesOpen.value && !repliesLoaded.value) {
    await loadReplies();
  }
}

function toggleReplyForm() {
  showReplyForm.value = !showReplyForm.value;
  if (showReplyForm.value && !repliesLoaded.value) {
    // 답글을 보여줄 준비
    repliesOpen.value = true;
    loadReplies();
  }
}


async function handleReplySubmitted() {
  showReplyForm.value = false;
  repliesLoaded.value = false;
  await loadReplies();
  emit("updated");
}

// 팝오버 ref
const userPopoverRef = ref(null);

// 유저 닉네임 클릭 핸들러
const onAuthorClick = (event) => {
  if (!props.comment.userId) return; // 게스트면 무시
  userPopoverRef.value?.open(event, props.comment.userId);
};
</script>


<style scoped>
.comment-item {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 12px;
  background: var(--color-background-soft);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.author {
  font-weight: 700;
  color: var(--color-heading);
}

.date {
  color: var(--color-text);
}

.content {
  margin: 0;
  white-space: pre-line;
  color: var(--color-heading);
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 13px;
}

.action-btn {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  color: var(--color-text);
}

.edit-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edit-textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 10px;
  font-size: 14px;
  resize: vertical;
  background: var(--color-background-soft);
  color: var(--color-heading);
}

.edit-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.guest-password {
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
  background: var(--color-background-soft);
  color: var(--color-heading);
}

.error-text {
  color: #ef4444;
  font-size: 12px;
}

.edit-actions {
  display: flex;
  gap: 8px;
}

.replies {
  margin-top: 10px;
  padding-left: 14px;
  border-left: 2px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-input {
  margin-top: 8px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-background-soft);
}

.reply-loading,
.no-replies {
  font-size: 13px;
  color: var(--color-text);
}

.text-teal {
  color: var(--color-primary);
}

.like-btn .liked-heart {
  color: #ef4444;
  font-weight: 700;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.15);
  /* 다크/라이트 공통 강제 */
  color: #ef4444;
}

.like-btn .unliked-heart {
  color: var(--color-text);
}
</style>
