# Problem.io Frontend

Vue 3 + PrimeVue 기반의 이미지 퀴즈 플랫폼 프론트엔드 애플리케이션입니다.

## 기술 스택

- **Vue 3** - 프론트엔드 프레임워크
- **Vue Router** - 라우팅
- **PrimeVue** - UI 컴포넌트 라이브러리
- **Axios** - HTTP 클라이언트
- **Vite** - 빌드 도구

## 프로젝트 구조

```
frontend/
├── src/
│   ├── api/              # API 서비스 파일
│   │   ├── axios.js      # Axios 인스턴스 설정
│   │   ├── auth.js       # 인증 관련 API
│   │   ├── user.js       # 사용자 관련 API
│   │   ├── quiz.js       # 퀴즈 관련 API
│   │   ├── comment.js    # 댓글 관련 API
│   │   └── submission.js # 풀이 제출 관련 API
│   ├── views/            # 페이지 컴포넌트
│   │   ├── Home.vue
│   │   ├── Feed.vue
│   │   ├── Quiz.vue
│   │   ├── Profile.vue
│   │   ├── Create.vue
│   │   └── auth/
│   │       ├── Login.vue
│   │       └── Signup.vue
│   ├── router/           # 라우터 설정
│   │   └── index.ts
│   ├── lib/              # 유틸리티
│   │   └── utils.ts
│   ├── App.vue
│   └── main.js
├── public/               # 정적 파일
└── package.json
```

## 설치 및 실행

### 1. 의존성 설치

```bash
npm install
```

### 2. 환경 변수 설정

`.env.example` 파일을 참고하여 `.env` 파일을 생성하고 API Base URL을 설정하세요.

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### 3. 개발 서버 실행

```bash
npm run dev
```

### 4. 빌드

```bash
npm run build
```

### 5. 프리뷰

```bash
npm run preview
```

## 주요 기능

- ✅ 사용자 인증 (일반 로그인/회원가입, 카카오 OAuth)
- ✅ 퀴즈 목록 조회 및 검색
- ✅ 퀴즈 상세 조회 및 풀이
- ✅ 퀴즈 생성/수정/삭제
- ✅ 좋아요 및 댓글 기능
- ✅ 사용자 프로필 및 팔로우
- ✅ 마이페이지

## API 연동

모든 API 호출은 `src/api/` 디렉토리의 서비스 파일을 통해 이루어집니다.

- `auth.js`: 인증 관련 API
- `user.js`: 사용자 관련 API
- `quiz.js`: 퀴즈 관련 API
- `comment.js`: 댓글 관련 API
- `submission.js`: 풀이 제출 관련 API

Axios 인스턴스는 자동으로 JWT 토큰을 헤더에 추가하며, 401 에러 시 자동으로 로그인 페이지로 리다이렉트합니다.

## PrimeVue 컴포넌트

이 프로젝트는 PrimeVue를 사용하여 UI를 구성합니다. 주요 사용 컴포넌트:

- Button
- Card
- InputText
- Textarea
- Dialog
- Toast
- ProgressBar
- Avatar
- Badge
- Divider
- FileUpload

## 라이선스

MIT

