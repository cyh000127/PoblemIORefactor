<template>
  <div class="challenge-widget">
    <div class="ranking-header">
      <div class="title">
        <i class="pi pi-bolt text-primary text-xl"></i>
        <span>Challenges</span>
      </div>
      <Button label="전체 조회" text size="small" @click="router.push('/challenges')" />
    </div>
    <ChallengeList
      :challenges="challenges"
      :loading="loading"
      :sidebarMode="true"
      @select="goToChallenge"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { getChallenges } from "@/api/challenge";
import ChallengeList from "@/components/challenge/ChallengeList.vue";
import Button from "primevue/button";

const router = useRouter();
const challenges = ref<any[]>([]);
const loading = ref(false);

const loadChallenges = async () => {
  loading.value = true;
  try {
    const data = await getChallenges();
    const now = new Date();
    // 마감되지 않은 챌린지 중 최신순으로 2개만 표시
    challenges.value = data
      .filter((c: any) => !c.endAt || new Date(c.endAt) > now)
      .slice(0, 2);
  } catch (error: any) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const goToChallenge = (challengeId: number) => {
  router.push(`/challenges/${challengeId}`);
};

onMounted(() => {
  loadChallenges();
});
</script>

<style scoped>
/* 챌린지 위젯 스타일링 (랭킹 위젯과 일치) */
.challenge-widget {
  position: relative;
  width: 100%;
  padding: 22px;
  border-radius: 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  box-shadow: none;
  overflow: hidden;
}

:global([data-theme="dark"] .challenge-widget) {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  box-shadow: none;
}

.ranking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  min-height: 48px;
}

.title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 2000;
  letter-spacing: 1px;
  color: var(--color-heading);
  font-size: 1.1rem;
  text-shadow: none;
}

.flame {
  filter: drop-shadow(0 0 6px rgba(255, 68, 68, 0.65));
}
</style>
