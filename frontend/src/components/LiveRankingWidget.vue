<template>
  <div class="ranking-widget">
    <div class="ranking-header">
      <div class="title">
        <i class="pi pi-trophy text-yellow-500 text-xl"></i>
        <span>Ranking</span>
      </div>

      <div class="header-actions">
        <div class="period-toggle">
          <button
            :class="['period-btn', { active: period === 'TODAY' }]"
            @click="setPeriod('TODAY')"
            type="button"
          >
            오늘
          </button>
          <button
            :class="['period-btn', { active: period === 'WEEK' }]"
            @click="setPeriod('WEEK')"
            type="button"
          >
            주간
          </button>
        </div>
        <span class="live-badge">LIVE</span>
      </div>
    </div>

    <div class="ranking-table">
      <div v-if="loading" class="table-body">
        <div v-for="i in 5" :key="i" class="table-row">
           <Skeleton width="2rem" height="1.5rem" class="mr-2" style="background-color: var(--skeleton-bg)"></Skeleton>
           <Skeleton shape="circle" size="2.5rem" class="mr-3" style="background-color: var(--skeleton-bg)"></Skeleton>
           <div class="flex flex-col flex-1 gap-1">
             <Skeleton width="60%" height="1rem" style="background-color: var(--skeleton-bg)"></Skeleton>
             <Skeleton width="40%" height="0.8rem" style="background-color: var(--skeleton-bg)"></Skeleton>
           </div>
        </div>
      </div>

      <div v-else-if="error" class="state-row error">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ error }}</span>
      </div>

      <div v-else-if="rankings.length === 0" class="state-row">
        <i class="pi pi-info-circle"></i>
        <span>데이터가 없습니다.</span>
      </div>

      <div v-else class="table-body">
        <div
          v-for="(row, idx) in rankings"
          :key="row.userId ?? idx"
          :class="['table-row', rankClass(idx + 1)]"
        >
          <div class="col-rank rank-cell">
            <span class="rank-hash">#{{ idx + 1 }}</span>
            <i v-if="idx < 3" class="pi pi-crown text-sm ml-1" :class="getRankIconColor(idx + 1)"></i>
          </div>

          <div class="col-avatar">
            <div class="avatar" :style="avatarStyle(displayName(row))">
              <img
                v-if="avatarUrl(row)"
                :src="avatarUrl(row)"
                :alt="displayName(row)"
                @error="row.profileImageUrl = ''"
              />
              <span v-else>{{ initial(displayName(row)) }}</span>
            </div>
          </div>

          <!-- ✅ 닉네임 + 점수(더 크게, 옆으로 붙임) -->
          <div class="col-main main-cell">
            <div class="name-score">
              <span class="nick-text">{{ displayName(row) }}</span>
              <span class="score-big">{{ row.score?.toLocaleString() }}</span>
            </div>

            <!-- 필요하면 아래 주석 해제: 정확도/푼 퀴즈 같은 서브정보 -->
            <!-- <div class="sub-line">{{ formattedQuiz(row) }}</div> -->
          </div>
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
import { getRanking } from "@/api/ranking";
import { resolveImageUrl } from "@/lib/image";
import Skeleton from "primevue/skeleton";

const rankings = ref([]);
const loading = ref(false);
const error = ref("");
const period = ref("TODAY");
const lastUpdated = ref(null);
const timerId = ref(null);

const formatDateTime = (date) => {
  if (!date) return "-";
  const d = typeof date === "string" ? new Date(date) : date;
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(
    d.getMinutes()
  )}:${pad(d.getSeconds())}`;
};

const lastUpdatedText = computed(() => formatDateTime(lastUpdated.value));

const formatAccuracy = (acc) => {
  if (acc === null || acc === undefined) return "-";
  return (acc * 100).toFixed(acc * 100 >= 100 ? 0 : 1);
};

const formattedQuiz = (row) => {
  const accText = `${formatAccuracy(row.accuracy)}%`;
  return `${accText} · ${row.solvedQuizCount ?? 0}퀴즈`;
};

const avatarUrl = (row) => {
  if (!row?.profileImageUrl) return "";
  return resolveImageUrl(row.profileImageUrl);
};

const displayName = (row) => {
  const name = (row?.nickname ?? "").trim();
  if (name) return name;
  if (row?.userId) return `사용자#${row.userId}`;
  return "익명";
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

const fetchRanking = async () => {
  loading.value = true;
  error.value = "";
  try {
    const data = await getRanking(period.value, 5);
    rankings.value = data || [];
    lastUpdated.value = new Date();
  } catch (e) {
    error.value = "랭킹을 불러오지 못했습니다.";
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
  const interval = period.value === "TODAY" ? 20000 : 60000;
  timerId.value = setInterval(fetchRanking, interval);
};

const setPeriod = (next) => {
  if (period.value === next) return;
  period.value = next;
};

watch(period, () => {
  fetchRanking();
  setupPolling();
});

onMounted(() => {
  fetchRanking();
  setupPolling();
});

onUnmounted(() => {
  clearTimer();
});
</script>

<style scoped>
/* ✅ 컨테이너 쿼리: 위젯 폭 기준 반응형 */
.ranking-widget {
  position: relative;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;

  padding: 22px;
  border-radius: 20px;

  container-type: inline-size;
  container-name: ranking;

  background: var(--bg-surface);
  border: 1px solid var(--border);
  box-shadow: none;
  overflow: hidden;
}

:global([data-theme="dark"] .ranking-widget) {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  box-shadow: none;
}

/* 헤더 */
.ranking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  min-height: 48px;
  flex-wrap: wrap;
}

.title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 2000;
  letter-spacing: 1px;
  color: var(--color-heading);
  font-size: 1.1rem;
  text-shadow: 0 4px 18px rgba(255, 115, 29, 0.35);
}

.flame {
  filter: drop-shadow(0 0 6px rgba(255, 68, 68, 0.65));
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.period-toggle {
  display: inline-flex;
  background: var(--bg-surface-hover);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 2px;
}

:global([data-theme="dark"] .period-toggle) {
  background: var(--bg-surface-hover);
  border-color: var(--border);
}

.period-btn {
  border: none;
  background: transparent;
  padding: 6px 12px;
  border-radius: 999px;
  color: var(--color-text-muted);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.period-btn.active {
  background: var(--primary);
  color: #ffffff; /* White text on Teal background */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  font-weight: 800;
}

:global([data-theme="dark"] .period-btn.active) {
  background: var(--primary);
  color: #ffffff;
  box-shadow: none;
}

.live-badge {
  padding: 4px 10px;
  border-radius: 10px;
  background: transparent;
  border: 1px solid var(--text-main);
  color: var(--text-main);
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.4px;
  box-shadow: none;
}

/* 테이블 */
.ranking-table {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.table-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

/* ✅ row 구조 변경: 점수 컬럼 제거하고, 닉네임 영역에 포함 */
.table-row {
  display: grid;
  grid-template-columns: 72px 52px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 12px 12px;
  border-radius: 14px;

  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.35);
  white-space: normal;
  min-width: 0;
}

:global([data-theme="dark"] .table-row) {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(94, 234, 212, 0.12);
}

.table-row.rank-top {
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.table-row.rank-gold {
  background: color-mix(in srgb, var(--primary), transparent 85%);
  border-color: color-mix(in srgb, var(--primary), transparent 40%);
  color: var(--color-heading);
}

.table-row.rank-silver {
  background: color-mix(in srgb, var(--accent), transparent 88%);
  border-color: color-mix(in srgb, var(--accent), transparent 50%);
}

.table-row.rank-bronze {
  background: color-mix(in srgb, var(--text-sub), transparent 90%);
  border-color: color-mix(in srgb, var(--text-sub), transparent 70%);
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

/* 셀 */
.col-rank,
.col-avatar {
  display: flex;
  align-items: center;
  min-width: 0;
}

.rank-cell {
  gap: 10px;
  color: var(--color-heading);
  font-weight: 800;
  font-size: 1.05rem;
}

.rank-hash {
  font-variant-numeric: tabular-nums;
}

.rank-emoji {
  font-size: 1.1rem; /* ✅ 무조건 보이게 */
}

.col-avatar {
  justify-content: center;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #0b1220;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.6);
  flex: 0 0 auto;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ✅ 닉네임+점수 */
.col-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.name-score {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}

.nick-text {
  font-weight: 800;
  color: var(--color-heading);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.1;
  font-size: 1.05rem;
  min-width: 0;
  flex: 1 1 auto;
}

.score-big {
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;

  /* ✅ 더 크게/눈에 띄게 */
  font-size: 1.15rem;
  letter-spacing: 0.2px;
  color: #7c3aed;
  flex: 0 0 auto;
}

:global([data-theme="dark"] .score-big) {
  color: #c084fc;
}

.rank-top .nick-text,
.rank-top .score-big {
  color: inherit;
}

.table-row.rank-top .nick-text {
  color: #0f172a;
  text-shadow: none;
}

:global([data-theme="dark"] .nick-text) {
  color: #f8fafc;
}

:global([data-theme="dark"] .table-row.rank-top .nick-text) {
  color: #f8fafc;
}

/* 선택: 서브 라인(정확도/푼퀴즈) */
/*
.sub-line {
  font-size: 0.84rem;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
*/

.state-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.state-row.error {
  color: #ef4444;
}

.ranking-footer {
  margin-top: 12px;
  font-size: 0.82rem;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  gap: 8px;
}

.last-updated {
  font-variant-numeric: tabular-nums;
}

/* ✅ 컨테이너 폭이 작아질 때도 "깨지지 않게"만 줄이되, 이모지는 숨기지 않음 */
@container ranking (max-width: 360px) {
  .ranking-widget {
    padding: 16px;
  }

  .table-body {
    padding: 12px;
    gap: 10px;
  }

  .table-row {
    grid-template-columns: 64px 44px minmax(0, 1fr);
    gap: 8px;
    padding: 10px 10px;
  }

  .avatar {
    width: 34px;
    height: 34px;
  }

  .nick-text {
    font-size: 0.98rem;
  }

  .score-big {
    font-size: 1.08rem;
  }

  .period-btn {
    padding: 5px 10px;
  }

  .live-badge {
    padding: 4px 8px;
  }
}
</style>
