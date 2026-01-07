<template>
  <Card class="h-full hover:shadow-lg transition-duration-200 cursor-pointer" :class="{ 'opacity-75 grayscale-0': isExpired }" @click="handleClick">
    <template #header>
      <div v-if="challenge.challengeType === 'TIME_ATTACK'" class="challenge-badge badge-time-attack">TIME ATTACK</div>
      <div v-else-if="challenge.challengeType === 'SURVIVAL'" class="challenge-badge badge-survival">SURVIVAL</div>
      <div v-else class="challenge-badge badge-event">EVENT</div>
    </template>

    <template #title>
      <div class="text-xl font-bold mb-2">{{ challenge.title }}</div>
    </template>

    <template #content>
      <p class="text-color-secondary line-clamp-2 h-3rem">
        {{ challenge.description }}
      </p>

      <div class="mt-4 flex gap-2" v-if="challenge.challengeType === 'TIME_ATTACK'">
        <div class="challenge-badge badge-time-attack border-round-sm flex align-items-center gap-2">
          <i class="pi pi-clock" style="font-size: 1rem; line-height: 1"></i>
          <strong style="line-height: 1; font-size: 0.9rem">{{ formatTime(challenge.timeLimit) }}</strong>
        </div>
      </div>
    </template>

    <template #footer>
      <Button
        :label="isExpired ? '이미 종료됨 (결과 보기)' : '도전하기'"
        :class="['w-full', isExpired ? 'opacity-80' : '']"
        :icon="isExpired ? 'pi pi-chart-bar' : 'pi pi-bolt'"
        :severity="isExpired ? 'secondary' : 'primary'"
      />
    </template>
  </Card>
</template>

<script setup>
import { computed } from "vue";
import Card from "primevue/card";
import Button from "primevue/button";
import Tag from "primevue/tag";

const props = defineProps({
  challenge: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(["click"]);

const isExpired = computed(() => {
  if (!props.challenge.endAt) return false;
  const end = new Date(props.challenge.endAt);
  const now = new Date();
  return now > end;
});

const handleClick = () => {
  // if (isExpired.value) return; // 만료되어도 클릭 허용
  emit("click");
};

const formatTime = (seconds) => {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}분 ${s}초`;
};
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.challenge-badge {
  padding: 0.25rem 0.75rem;
  font-weight: 700;
  text-align: center;
  border-radius: 0; /* 헤더 모양 */
}

/* 타임어택: 강조 (골드) */
.badge-time-attack {
  background-color: var(--bg-surface-hover);
  color: var(--accent);
  border: 1px solid var(--accent);
}

/* 서바이벌: 기본 (부드러운 청록색) */
.badge-survival {
  background-color: var(--bg-surface-hover);
  color: var(--primary);
  border: 1px solid var(--primary);
}

/* 이벤트: 회색 */
.badge-event {
  background-color: var(--bg-surface-hover);
  color: var(--text-sub);
  border: 1px solid var(--border);
}

/* 다크 모드 오버라이드 (변수를 통한 단순화) */
:global([data-theme="dark"]) .badge-time-attack {
  background-color: rgba(255, 200, 49, 0.1); /* 낮은 불투명도 강조 */
  color: var(--accent);
}

:global([data-theme="dark"]) .badge-survival {
  background-color: rgba(100, 189, 172, 0.1); /* 낮은 불투명도 기본 */
  color: var(--primary);
}

:global([data-theme="dark"]) .badge-event {
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-sub);
}
</style>
