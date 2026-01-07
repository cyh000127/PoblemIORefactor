<template>
  <div class="ranking-widget">
    <div class="ranking-header">
      <div class="title">
        <i class="pi pi-trophy text-amber-400 text-xl" style="color: var(--accent) !important"></i>
        <span>랭킹</span>
      </div>
      <div class="header-actions">
        <span class="live-badge">LIVE</span>
      </div>
    </div>

    <div class="ranking-table">
      <div v-if="loading" class="state-row">
        <i class="pi pi-spin pi-spinner"></i>
        <span>랭킹을 불러오는 중...</span>
      </div>

      <div v-else-if="error" class="state-row error">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ error }}</span>
      </div>

      <div v-else-if="topRankings.length === 0" class="state-row">
        <i class="pi pi-info-circle"></i>
        <span>아직 랭킹 데이터가 없습니다.</span>
      </div>

      <div v-else class="table-body">
        <div v-for="(row, idx) in topRankings" :key="row.userId ?? idx" :class="['table-row', rankClass(row.ranking)]">
          <div class="col-rank rank-cell">
            <span class="rank-hash">#{{ row.ranking }}</span>
            <i v-if="row.ranking <= 3" class="pi pi-crown text-sm ml-1" :class="getRankIconColor(row.ranking)"></i>
          </div>

          <div class="col-avatar">
            <div class="avatar" :style="avatarStyle(displayName(row))">
              <img v-if="avatarUrl(row)" :src="avatarUrl(row)" :alt="displayName(row)" @error="row.profileImageUrl = ''" />
              <span v-else>{{ initial(displayName(row)) }}</span>
            </div>
          </div>

          <div class="col-main main-cell">
            <div class="name-score">
              <span class="nick-text">{{ displayName(row) }}</span>
              <span class="score-big">{{ row.score?.toLocaleString() }}</span>
            </div>
            <div class="sub-line">{{ formatPlayTime(row.playTime) }}</div>
          </div>
        </div>
      </div>

      <!-- My Ranking (Sticky Bottom) -->
      <div v-if="myRanking && !isMyRankingInTop" class="my-ranking-divider">
        <div class="divider-line"></div>
        <span class="divider-text">내 순위</span>
        <div class="divider-line"></div>
      </div>

      <div v-if="myRanking && !isMyRankingInTop" :class="['table-row', 'my-rank']">
        <div class="col-rank rank-cell">
          <span class="rank-hash">#{{ myRanking.ranking > 0 ? myRanking.ranking : "-" }}</span>
        </div>

        <div class="col-avatar">
          <div class="avatar" :style="avatarStyle(displayName(myRanking))">
            <img v-if="avatarUrl(myRanking)" :src="avatarUrl(myRanking)" :alt="displayName(myRanking)" @error="myRanking.profileImageUrl = ''" />
            <span v-else>{{ initial(displayName(myRanking)) }}</span>
          </div>
        </div>

        <div class="col-main main-cell">
          <div class="name-score">
            <span class="nick-text">{{ displayName(myRanking) }}</span>
            <span class="score-big">{{ myRanking.score?.toLocaleString() }}</span>
          </div>
          <div class="sub-line">{{ formatPlayTime(myRanking.playTime) }}</div>
        </div>
      </div>
    </div>

    <div class="ranking-footer">
      <span class="last-updated">마지막 업데이트 · {{ lastUpdatedText }}</span>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, computed, watch } from "vue";
import { getLeaderboard } from "@/api/challenge";
import { resolveImageUrl } from "@/lib/image";
import { useAuthStore } from "@/stores/auth";

const props = defineProps({
  challengeId: {
    type: Number,
    required: true,
  },
});

const topRankings = ref([]);
const myRanking = ref(null);
const loading = ref(false);
const error = ref("");
const lastUpdated = ref(null);
const timerId = ref(null);

const formatDateTime = (date) => {
  if (!date) return "-";
  const d = typeof date === "string" ? new Date(date) : date;
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const lastUpdatedText = computed(() => formatDateTime(lastUpdated.value));

const formatPlayTime = (ms) => {
  if (ms === null || ms === undefined) return "-";

  // 숫자인지 확인
  const val = Number(ms);
  if (isNaN(val)) return "-";

  return `${val.toFixed(3)}s`;
};

const avatarUrl = (row) => {
  if (!row?.profileImageUrl) return "";
  return resolveImageUrl(row.profileImageUrl);
};

const displayName = (row) => {
  const name = (row?.nickname ?? "").trim();
  if (name) return name;
  if (row?.userId) return `사용자#${row.userId}`;
  return "Guest"; // 더미 랭크용
};

const rankEmoji = (rank) => {
  return "";
};

const rankClass = (rank) => {
  if (rank === 1) return "rank-top rank-gold";
  if (rank === 2) return "rank-top rank-silver";
  if (rank === 3) return "rank-top rank-bronze";
  return "";
};

const getRankIconColor = (rank) => {
  if (rank === 1) return "text-[var(--primary)]";
  if (rank === 2) return "text-[var(--accent)]";
  if (rank === 3) return "text-[var(--text-sub)]";
  return "";
};

const initial = (name) => (name ? name.charAt(0) : "?");

const avatarStyle = (name) => {
  const colors = ["#2dd4bf", "#60a5fa", "#f472b6", "#fbbf24", "#22d3ee"];
  const code = name ? name.charCodeAt(0) : 0;
  return { background: colors[code % colors.length] };
};

const isMyRankingInTop = computed(() => {
  if (!myRanking.value || !topRankings.value) return false;
  return topRankings.value.some((r) => r.userId === myRanking.value.userId);
});

const authStore = useAuthStore();

const fetchRanking = async () => {
  if (!props.challengeId) return;

  // 폴링 시 로딩 표시 안 함, 첫 로드 시에만 표시
  if (!lastUpdated.value) loading.value = true;
  error.value = "";

  try {
    const data = await getLeaderboard(props.challengeId);
    // 데이터 구조: { topRankings: [], myRanking: {} }
    topRankings.value = data.topRankings || [];

    // 더 나은 표시를 위해 백엔드 랭킹 데이터와 로컬 인증 데이터 병합
    if (data.myRanking) {
      myRanking.value = {
        ...data.myRanking,
        nickname: authStore.user?.nickname || data.myRanking.nickname,
        profileImageUrl: authStore.user?.profileImageUrl || data.myRanking.profileImageUrl,
      };
    } else {
      myRanking.value = null;
    }

    lastUpdated.value = new Date();
  } catch (e) {
    if (!lastUpdated.value) error.value = "랭킹을 불러오지 못했습니다.";
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const clearTimer = () => {
  if (timerId.value) {
    clearInterval(timerId.value);
    timerId.value = null;
  }
};

const setupPolling = () => {
  clearTimer();
  // 10초마다 폴링 (백엔드 캐시와 일치)
  timerId.value = setInterval(fetchRanking, 10000);
};

watch(
  () => props.challengeId,
  (newId) => {
    if (newId) {
      fetchRanking();
      setupPolling();
    }
  }
);

onMounted(() => {
  fetchRanking();
  setupPolling();
});

onUnmounted(() => {
  clearTimer();
});
</script>

<style scoped>
.ranking-widget {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  padding: 20px; /* 원래 패딩 */
  border-radius: 16px;
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.ranking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  font-size: 1.1rem;
  color: var(--color-heading);
}

.live-badge {
  padding: 4px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3);
}

.ranking-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.state-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  color: var(--color-text-muted);
  justify-content: center;
}

.state-row.error {
  color: #ef4444;
}

.table-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.table-row {
  display: grid;
  grid-template-columns: 50px 40px minmax(0, 1fr); /* 원래 그리드 */
  align-items: center;
  gap: 8px;
  padding: 10px; /* 원래 패딩 */
  border-radius: 10px;
  background: var(--color-background-mute);
  transition: transform 0.1s;
}

/* 업데이트된 색상 (비비드 시안) */
/* 업데이트된 색상 (테마 변수) */
.table-row.rank-gold {
  background: color-mix(in srgb, var(--primary), transparent 85%);
  border: 1px solid color-mix(in srgb, var(--primary), transparent 40%);
  color: var(--color-heading);
}
.table-row.rank-silver {
  background: color-mix(in srgb, var(--accent), transparent 88%);
  border: 1px solid color-mix(in srgb, var(--accent), transparent 50%);
}
.table-row.rank-bronze {
  background: color-mix(in srgb, var(--text-sub), transparent 90%);
  border: 1px solid color-mix(in srgb, var(--text-sub), transparent 70%);
}

:global([data-theme="dark"] .table-row.rank-gold) {
  background: color-mix(in srgb, var(--primary), transparent 85%);
  border-color: color-mix(in srgb, var(--primary), transparent 50%);
  box-shadow: 0 4px 20px color-mix(in srgb, var(--primary), transparent 90%);
}

:global([data-theme="dark"] .table-row.rank-silver) {
  background: color-mix(in srgb, var(--accent), transparent 90%);
  border-color: color-mix(in srgb, var(--accent), transparent 60%);
}

:global([data-theme="dark"] .table-row.rank-bronze) {
  background: color-mix(in srgb, var(--text-sub), transparent 92%);
  border-color: color-mix(in srgb, var(--text-sub), transparent 80%);
}

.rank-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 700;
  font-size: 1rem;
  color: var(--color-heading);
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-weight: 700;
  font-size: 0.9rem;
  color: #1a1a1a;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.col-main {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.name-score {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.nick-text {
  font-weight: 700;
  color: var(--color-heading);
  font-size: 0.95rem; /* 원래 폰트 크기 */
}

.score-big {
  color: var(--primary); /* 비비드 시안으로 업데이트됨 */
  font-weight: 800;
  font-family: monospace;
}

.sub-line {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  text-align: right;
}

.ranking-footer {
  margin-top: 12px;
  text-align: right;
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

/* 내 랭킹 섹션 */
.my-ranking-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
.divider-line {
  flex: 1;
  height: 1px;
  background: var(--color-border);
}
.table-row.my-rank {
  border: 2px solid var(--primary); /* 비비드 시안으로 업데이트됨 */
  background: var(--bg-surface-hover); /* 업데이트된 배경 */
}
</style>
