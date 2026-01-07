<template>
  <div class="ranking-widget">
    <div class="ranking-header">
      <div class="title">
        <i class="pi pi-trophy text-yellow-500 text-xl"></i>
        <span>Challenge Ranking</span>
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

  // Check if it's a number
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
  return "Guest"; // For dummy rank
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
  if (rank === 1) return "text-yellow-500";
  if (rank === 2) return "text-gray-400";
  if (rank === 3) return "text-orange-400";
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

  // Don't show loading on polling, only first load
  if (!lastUpdated.value) loading.value = true;
  error.value = "";

  try {
    const data = await getLeaderboard(props.challengeId);
    // data structure: { topRankings: [], myRanking: {} }
    topRankings.value = data.topRankings || [];

    // Merge backend ranking data with local auth data for better display
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
  // Poll every 10 seconds (matches backend cache)
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
  padding: 20px;
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
  grid-template-columns: 50px 40px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 10px;
  background: var(--color-background-mute);
  transition: transform 0.1s;
}

.table-row.rank-gold {
  background: linear-gradient(135deg, rgba(255, 215, 0, 0.15), rgba(255, 223, 0, 0.05));
  border: 1px solid rgba(255, 215, 0, 0.3);
}
.table-row.rank-silver {
  background: linear-gradient(135deg, rgba(192, 192, 192, 0.15), rgba(220, 220, 220, 0.05));
  border: 1px solid rgba(192, 192, 192, 0.3);
}
.table-row.rank-bronze {
  background: linear-gradient(135deg, rgba(205, 127, 50, 0.15), rgba(210, 180, 140, 0.05));
  border: 1px solid rgba(205, 127, 50, 0.3);
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
  font-size: 0.95rem;
}

.score-big {
  color: #7c3aed;
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

/* My Ranking Section */
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
  border: 2px solid #7c3aed;
  background: rgba(124, 58, 237, 0.05);
}
</style>
