<template>
  <ProfileBackground :user="user" :previewThemeId="previewThemeId" :previewConfig="previewConfig" class="mb-8 p-6 shadow-4 profile-card-content">
    <div class="flex flex-col md:flex-row gap-6 items-center md:items-start">
      <UserAvatar :user="user" class="w-32 h-32 flex-shrink-0" />

      <div class="flex-1 text-center md:text-left min-w-0 w-full">
        <!-- 닉네임 / 상태메시지 + 우측 상단 버튼 슬롯 -->
        <div class="flex flex-col md:flex-row justify-between items-center md:items-start gap-4 md:gap-0">
          <div class="min-w-0 w-full md:w-auto">
            <!-- 오버플로우 방지를 위한 truncate 클래스 추가 -->
            <h1 class="text-3xl font-bold mb-2 truncate">{{ user?.nickname || "닉네임 없음" }}</h1>
            <p v-if="user?.statusMessage" class="text-lg opacity-80 mb-4 break-words">
              {{ user.statusMessage }}
            </p>
          </div>

          <!-- 액션 버튼 슬롯 (마이페이지 전용) -->
          <div v-if="$slots.actions" class="flex gap-2 flex-shrink-0">
            <slot name="actions"></slot>
          </div>
        </div>

        <!-- 통계 영역 -->
        <div class="flex justify-center md:justify-start gap-8 mt-2">
          <div class="stat-box">
            <p class="text-2xl font-bold m-0">{{ user?.quizCount ?? quizCount ?? 0 }}</p>
            <p class="text-sm opacity-80 m-0">퀴즈 수</p>
          </div>
          <div class="stat-box">
            <p class="text-2xl font-bold m-0">{{ user?.followerCount ?? 0 }}</p>
            <p class="text-sm opacity-80 m-0">팔로워</p>
          </div>
          <div class="stat-box">
            <p class="text-2xl font-bold m-0">{{ user?.followingCount ?? 0 }}</p>
            <p class="text-sm opacity-80 m-0">팔로잉</p>
          </div>
        </div>
      </div>
    </div>
  </ProfileBackground>
</template>

<script setup>
import {} from "vue";
import ProfileBackground from "@/components/user/ProfileBackground.vue";
import UserAvatar from "@/components/common/UserAvatar.vue";
import Button from "primevue/button";

const props = defineProps({
  user: {
    type: Object,
    required: true,
    default: () => ({}),
  },
  previewThemeId: {
    type: [String, Number],
    default: null,
  },
  previewConfig: {
    type: Object,
    default: null,
  },
  quizCount: {
    type: Number,
    default: 0,
  },
});
</script>

<style scoped>
/* 프로필 카드 내부 텍스트 색상 강제 상속 */
.profile-card-content :deep(p),
.profile-card-content :deep(span),
.profile-card-content :deep(h1) {
  color: inherit;
}

/* 통계 부분 */
.stat-box {
  width: 60px;
  text-align: center;
}
</style>
