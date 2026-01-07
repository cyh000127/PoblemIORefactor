<template>
  <section class="comment-section" :class="{ compact: !!parentCommentId }">
    <div class="comment-card">
      <!-- 비로그인 게스트 정보 -->
      <div v-if="!isAuthenticated" class="guest-row">
        <InputText
          v-model="nickname"
          class="flex-1"
          placeholder="닉네임"
          :disabled="submitting"
        />
        <InputText
          v-model="password"
          type="password"
          class="guest-password"
          placeholder="비밀번호"
          :disabled="submitting"
        />
      </div>

      <div class="comment-input-wrapper">
        <!-- 일반 textarea (기본) -->
        <textarea
          v-if="!usePrimeTextarea"
          ref="textareaRef"
          v-model="content"
          class="comment-textarea"
          rows="3"
          :placeholder="placeholderText"
          :disabled="submitting"
          @keyup.enter.exact.prevent="submit"
        />
        <!-- PrimeVue Textarea 대응 -->
        <Textarea
          v-else
          ref="textareaRef"
          v-model="content"
          class="comment-textarea prime"
          autoResize
          rows="3"
          :placeholder="placeholderText"
          :disabled="submitting"
          @keyup.enter.exact.prevent="submit"
        />

        <!-- 경고 문구 오버레이 -->

      </div>

      <div class="action-row">
        <Button
          :label="buttonLabel"
          class="comment-submit-btn"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="submit"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from "vue";
import { useAuthStore } from "@/stores/auth";
import { createComment } from "@/api/comment";
import Textarea from "primevue/textarea";

const props = defineProps({
  quizId: Number,
  parentCommentId: { type: Number, default: null },
  placeholder: { type: String, default: "" },
  buttonLabel: { type: String, default: "" },
});

const emit = defineEmits(["submitted"]);

const authStore = useAuthStore();
const isAuthenticated = authStore.isAuthenticated;

const content = ref("");
const nickname = ref("");
const password = ref("");
const submitting = ref(false);
const textareaRef = ref(null);
const usePrimeTextarea = ref(false); // 필요 시 true로 토글

const placeholderText = computed(() =>
  props.placeholder || (props.parentCommentId ? "댓글 작성 시 IP가 기록되며 사이트 이용 제한이나 요청에 따라 법적 조치가 취해질 수 있습니다" : "댓글 작성 시 IP가 기록되며 사이트 이용 제한이나 요청에 따라 법적 조치가 취해질 수 있습니다")
);
const buttonLabel = computed(() =>
  props.buttonLabel || (props.parentCommentId ? "답글 작성" : "작성하기")
);

const canSubmit = computed(() => {
  if (!content.value.trim()) return false;
  if (authStore.isAuthenticated) return true;
  return nickname.value.trim() && password.value.trim();
});

async function submit() {
  if (!canSubmit.value || submitting.value) return;

  const payload = {
    content: content.value,
    parentCommentId: props.parentCommentId ?? null,
  };

  if (!authStore.isAuthenticated) {
    payload.nickname = nickname.value.trim();
    payload.password = password.value.trim();
  }

  submitting.value = true;
  try {
    await createComment(props.quizId, payload);
    content.value = "";
    if (!authStore.isAuthenticated) {
      password.value = "";
    }
    emit("submitted");
  } catch (err) {
    console.error("댓글 작성 실패", err);
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.comment-section {
  margin-bottom: 12px;
}

.comment-card {
  background: var(--color-background-soft);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.compact .comment-card {
  padding: 12px;
}

.guest-row {
  display: flex;
  gap: 8px;
}

.guest-password {
  width: 160px;
}

:deep(.guest-row .p-inputtext),
.guest-row .guest-password,
.guest-row .flex-1 {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  color: var(--text-main);
}

.comment-input-wrapper {
  position: relative;
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: 10px;
}

.comment-warning {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  padding: 12px;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.5;
  pointer-events: none; /* 입력을 가로막지 않음 */
  background: var(--color-background-mute);
  border-radius: 10px;
}

.comment-textarea {
  width: 100%;
  min-height: 110px;
  border-radius: 10px;
  border: none;
  padding: 12px;
  box-sizing: border-box;
  background: transparent;
  color: var(--text-main);
  resize: vertical;

  /* 스크롤바 숨김 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.comment-textarea::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Opera */
}

.comment-textarea.prime {
  background: transparent;
  box-shadow: none;
}

/* PrimeVue 내부 textarea를 완전히 투명/무테로 맞춤 */
:deep(.comment-textarea.prime.p-inputtextarea) {
  background: transparent;
  border: none;
  box-shadow: none;
  color: var(--text-main);
  
  /* 스크롤바 숨김 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

:deep(.comment-textarea.prime.p-inputtextarea::-webkit-scrollbar) {
  display: none;
}

/* 테마 오버라이드는 이제 변수로 처리되므로 제거됨 */

.action-row {
  display: flex;
  justify-content: flex-end;
}

.comment-submit-btn {
  width: 110px;
}
</style>
