import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";

// 성능 향상을 위한 지연 로딩
const HomeView = () => import("@/views/HomeView.vue");
const LoginView = () => import("@/views/auth/LoginView.vue");
const SignupView = () => import("@/views/auth/SignupView.vue");
const QuizDetailView = () => import("@/views/quiz/QuizDetailView.vue");
const QuizPlayView = () => import("@/views/quiz/QuizPlayView.vue");
const QuizResultView = () => import("@/views/quiz/QuizResultView.vue");
const QuizCreateView = () => import("@/views/quiz/QuizCreateView.vue");
const QuizEditView = () => import("@/views/quiz/QuizEditView.vue");
const UserPageView = () => import("@/views/user/UserPageView.vue");
const MyPageView = () => import("@/views/user/MyPageView.vue");
const ProfileEditView = () => import("@/views/user/ProfileEditView.vue");
const ChallengeListView = () => import("@/views/challenge/ChallengeListView.vue");
const ChallengePlayView = () => import("@/views/challenge/ChallengePlayView.vue");
const ChallengeResultView = () => import("@/views/challenge/ChallengeResultView.vue");
const ChallengeDetailView = () => import("@/views/challenge/ChallengeDetailView.vue");

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 인증 라우트
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: { requiresAuth: false },
    },
    {
      path: "/signup",
      name: "signup",
      component: SignupView,
      meta: { requiresAuth: false },
    },
    // 메인
    {
      path: "/",
      name: "home",
      component: HomeView,
      meta: { requiresAuth: false },
    },
    // 챌린지 라우트
    {
      path: "/challenges",
      name: "challenges",
      component: ChallengeListView,
      meta: { requiresAuth: false },
    },
    {
      path: "/challenges/:id",
      name: "challenge-detail",
      component: ChallengeDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: "/challenges/:id/play",
      name: "challenge-play",
      component: ChallengePlayView,
      meta: { requiresAuth: true },
    },
    {
      path: "/challenges/:id/result",
      name: "challenge-result",
      component: ChallengeResultView,
      meta: { requiresAuth: true },
    },
    // 퀴즈 라우트
    {
      path: "/quiz/:id",
      name: "quiz-detail",
      component: QuizDetailView,
      meta: { requiresAuth: false },
      props: true,
    },
    {
      path: "/quiz/:id/play",
      name: "quiz-play",
      component: QuizPlayView,
      meta: { requiresAuth: false },
      props: true,
    },
    {
      path: "/quiz/:id/result",
      name: "quiz-result",
      component: QuizResultView,
      meta: { requiresAuth: false },
      props: true,
    },
    {
      path: "/quiz/create",
      name: "quiz-create",
      component: QuizCreateView,
      meta: { requiresAuth: true },
    },
    {
      path: "/quiz/edit/:id",
      name: "quiz-edit",
      component: QuizEditView,
      meta: { requiresAuth: true },
      props: true,
    },
    // 사용자 라우트
    {
      path: "/users/:id",
      name: "user-profile",
      component: UserPageView,
      meta: { requiresAuth: false },
      props: true,
    },
    {
      path: "/mypage",
      name: "mypage",
      component: MyPageView,
      meta: { requiresAuth: true },
    },
    {
      path: "/mypage/edit",
      name: "profile-edit",
      component: ProfileEditView,
      meta: { requiresAuth: true },
    },
    {
      path: "/admin",
      name: "admin",
      component: () => import("@/views/admin/AdminPageView.vue"),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    // 이전 라우트 리다이렉트
    {
      path: "/feed",
      redirect: "/",
    },
    {
      path: "/auth/login",
      redirect: "/login",
    },
    {
      path: "/auth/signup",
      redirect: "/signup",
    },
    {
      path: "/create",
      redirect: "/quiz/create",
    },
    {
      path: "/profile",
      redirect: "/mypage",
    },
    // 404 처리를 위한 Catch-all
    {
      path: "/:pathMatch(.*)*",
      redirect: "/",
    },
  ],
  scrollBehavior() {
    // 페이지 이동 시 무조건 최상단으로 스크롤 이동
    return { top: 0, left: 0 };
  },
});

// 네비게이션 가드
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();

  // 로그인 페이지로 이동할 때 redirect 파라미터가 없으면 직전 페이지를 기억 (signup 페이지 제외)
  if (to.name === "login" && !to.query.redirect && from && from.name !== "login" && from.name !== "signup") {
    return next({
      ...to,
      query: { ...to.query, redirect: from.fullPath || "/" },
      replace: true,
    });
  }

  // 인증이 필요한 라우트인지 확인
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    // 원래 가려던 경로를 query로 전달
    return next({
      name: "login",
      query: { redirect: to.fullPath },
    });
  }

  // [관리자 보안] 관리자 권한 확인
  if (to.meta.requiresAdmin && authStore.user?.role !== "ROLE_ADMIN") {
    // alert('관리자 권한이 필요합니다.'); // UX상 조용히 홈으로 보내거나 404처럼 처리하는게 나을 수 있음
    return next({ name: "home" });
  }

  next();
});

export default router;
