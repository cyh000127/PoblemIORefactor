# 📸 Problem.io — 이미지 기반 퀴즈 플랫폼

![ProblemIO](readmeImageSet/main.jpg)

- Problem.io는 **이미지 한 장으로 직관적·시각적인 퀴즈를 만들고 풀 수 있는 UGC 기반 플랫폼**입니다.
- Google Gemini 기반의 **생성형 AI(GenAI)** 기술을 도입하여 퀴즈 제작 경험을 혁신하고, 좋아요·팔로우·댓글 등 **SNS형 상호작용**을 통해 제작자와 플레이어가 자연스럽게 연결되는 커뮤니티를 지향합니다.

## 주요 기능 (Key Features)

### 1) AI 자동 썸네일 생성
- **Gemini 2.0 Flash** 모델 연동
- 퀴즈 제목과 설명만 입력하면 AI가 최적의 썸네일 2장을 즉시 생성 (Cinematic/Vibrant 스타일)
- 사용자는 마음에 드는 스타일을 선택하여 바로 적용 가능

### 2) 직관적인 퀴즈 제작
- 이미지 업로드 및 보기(객관식) 설정
- 복수 정답 처리 및 트랜잭션 기반의 안전한 저장
- 공개/비공개 설정 지원

### 3) 퀴즈 풀이 & 즉시 피드백
- 이미지 중심의 몰입감 있는 UI
- 정답 선택 시 즉시 O/X 피드백 및 해설 제공
- 최종 점수 및 소요 시간 기록

### 4) 소셜 커뮤니티 (SNS-like)
- **좋아요 & 댓글**: 퀴즈에 대한 반응과 의견 공유
- **팔로우 & 피드**: 관심 있는 제작자를 팔로우하고 새 퀴즈를 피드에서 확인
- **마이페이지**: 내가 푼 퀴즈, 만든 퀴즈, 팔로우 내역 등을 한눈에 관리

## 아키텍처 & 스택 (Tech Stack)
![System Architecture](readmeImageSet/systemArchitecture.png) 

### Backend
- **Core**: Spring Boot 3.5, JDK 17
- **Security**: Spring Security + JWT (Stateless Authentication)
- **Persistence**: MyBatis (Complex Query Control), MySQL
- **AI Service**: **Google Gemini 2.0 Flash** (via GMS), **Caffeine Cache** (Temporary Image Storage)
- **Infra**: AWS S3 (Image Storage), Google SMTP (Email Auth)

### Frontend
- **Framework**: Vue 3 + Vite
- **State Management**: Pinia (Store)
- **UI Component**: PrimeVue, Tailwind CSS (Custom)
- **Network**: Axios (Interceptors for Auto-JWT Injection)

### DB
![DB_ERD](readmeImageSet/ProblemIO_ERD.png) 
## 프로젝트 구조

```
ProblemIO/
├─ frontend/       # Vue 3 + Vite Client
│  ├─ src/api      # API Services (Axios)
│  ├─ src/components
│  │  ├─ layout    # Header, NavBar, Sidebar
│  │  ├─ common    # Shared UI Components
│  │  ├─ quiz      # Quiz Playing Widgets
│  │  └─ comment   # Comment & Reply System
│  ├─ src/views    # Page Routings
│  │  ├─ auth      # Login / Register
│  │  ├─ quiz      # List / Create / Solve / Result
│  │  ├─ user      # Profile / MyPage
│  │  └─ challenge # Ranking & Challenge
│  └─ src/stores   # Pinia Stores (Auth, Quiz, User)
├─ backend/        # Spring Boot Server
│  ├─ src/main/java/com/problemio
│  │  ├─ ai       # Gemini Client & Thumbnail Service ✨
│  │  ├─ quiz     # Quiz CRUD & Like
│  │  ├─ submission # Scoring & Ranking
│  │  ├─ user     # Auth & Profile & Follow
│  │  └─ global   # Config, Exception, Security, S3
│  └─ resources/mapper # MyBatis XML Mappers
└─ README.md       # Project Documentation
```
## 사용자 흐름   

### 1. 회원가입 및 로그인 (Authentication)
1. **회원가입**: 이메일, 닉네임, 비밀번호(복잡도 검증) 입력 후 가입.
2. **로그인**: 가입한 계정으로 로그인하여 JWT(Access/Refresh) 토큰 발급.
3. **프로필 설정**: 마이페이지에서 닉네임 수정 및 아바타/테마 아이템 장착.   

### 2. AI 퀴즈 제작 (Creation Flow)
1. **퀴즈 만들기 진입**: 제목과 설명을 입력.
2. **AI 썸네일 생성**: **"AI 썸네일 생성"** 버튼 클릭 시, Gemini가 2가지 스타일(Cinematic/Vibrant)의 이미지를 제안.
3. **썸네일 선택**: 마음에 드는 이미지를 선택하거나 직접 업로드.
4. **문제 출제**: 문제 이미지 업로드 및 정답(복수 가능) 설정 후 최종 발행.   

### 3. 퀴즈 풀이 및 소셜 (Play & Social)
1. **퀴즈 풀이**: 메인 피드에서 퀴즈 선택 -> 이미지 확인 후 정답 입력 -> 즉시 채점(O/X).
2. **결과 확인**: 모든 문항 풀이 후 최종 점수와 소요 시간 확인.
3. **소셜 상호작용**: "좋아요" 클릭 및 댓글 작성으로 제작자와 소통.
4. **팔로우**: 관심 있는 제작자를 팔로우하여 피드 구독.   

### 4. 챌린지 및 랭킹 (Challenge & Competition)
1. **챌린지 참여**: 진행 중인 타임어택 챌린지 입장.
2. **경쟁**: 제한 시간 내에 빠르게 정답 맞히기.
3. **랭킹 확인**: 점수 > 시간 > 제출일시 순으로 산정된 실시간 순위표(Leaderboard) 확인.   

### 5. 관리자 기능 (Admin - Optional)
1. **콘텐츠 관리**: 부적절한 퀴즈 숨김(Hide) 처리.
2. **챌린지 생성**: 기존 퀴즈를 선택하여 기간 한정 챌린지 이벤트 개설.
3. **댓글 관리**: 악성 댓글 삭제 조치.   

## 실제 서비스 화면  
### 1. 메인 화면
- 다크 모드
![Home_Dark](readmeImageSet/01_Home_Dark.png)   
- 라이트 모드
![Home_Light](readmeImageSet/01_Home_Light.png)    
- 회원가입 및 로그인
![Sign-Up and Login](readmeImageSet/signupAndLogin.gif)  

### 2. 마이페이지
- 마이페이지 진입 및 유저별 테마 설정
![My_Page_Theme_Change](readmeImageSet/changeTheme.gif)  

### 3. 퀴즈 제작
- AI 기반 나만의 썸네일 제작  
![AI_Thumbnail](readmeImageSet/AI_image.gif)

- 퀴즈 제작
![Quiz_Create](readmeImageSet/QuizCreate.png)
![Quiz_Editor](readmeImageSet/QuizEditor.gif)

### 4. 퀴즈 진입 및 풀기
![Quiz_Play](readmeImageSet/quizPlay.gif)

### 5. 댓글 작성
![Comment](readmeImageSet/comment.gif)

### 6. 랭킹
![Ranking](readmeImageSet/ranking.gif)

### 7. 관리자 기능
![Admin](readmeImageSet/admin.gif)

### 8. 챌린지
- 타임어택
![Challenge](readmeImageSet/timeAttack.gif) 

   
# 회고 및 추후 개선 사항

## 김영규
- DB 설계 및 성능 최적화 주도
- Mono Repo 기반 Front/Back 전반 개발 참여
- Quiz 생성·제출·채점 로직 및 비회원 플레이/댓글 플로우 구현
- 실시간 랭킹 시스템 설계·구현
- AI 기반 퀴즈 썸네일 생성 기능 도입
- 서비스 전반 Light/Dark 테마 UI 시스템 구현
> 프로젝트를 진행하며 가장 크게 느낀 점은 기술 선택이 곧 설계 사고방식을 바꾼다는 것이었다.   
이전까지는 JPA 중심으로 개발을 해왔기 때문에, 객체 중심 설계와 연관관계 매핑, 영속성 컨텍스트에 의존한 개발 방식이 익숙했다. 하지만 이번 프로젝트에서는 MyBatis를 처음으로 본격 사용하며, 쿼리 한 줄이 곧 성능과 직결되는 환경을 직접 마주하게 되었다.   
  DB 설계와 인덱스 전략을 먼저 고민하지 않으면 조회 성능이 즉시 문제로 드러났고, 이는 자연스럽게 데이터 흐름과 쿼리 실행 계획을 깊이 이해하려는 태도로 이어졌다. 특히 Quiz 제출·채점, 실시간 랭킹과 같이 트래픽이 집중되는 영역에서는 JPA의 추상화보다 MyBatis의 명시성이 더 큰 장점으로 느껴졌다.   
  또한 Mono Repo 환경에서 프론트엔드와 백엔드를 함께 다루며, 단순 기능 구현을 넘어 비회원 플로우, 테마 시스템, AI 기능 도입까지 사용자 경험 전반을 고려한 개발의 중요성을 체감했다. 이번 프로젝트는 단순히 기술을 적용한 경험이 아니라, 왜 이 기술을 선택해야 하는지 스스로 설명할 수 있게 된 전환점이었다.


## 최윤혁
- Front/Back 기초 구조 설계
- Mono Repo 기반 Front/Back 전반 개발 참여
- User / Admin / Challenge / Follow 등 기능 구현
- 이메일 인증 기반 회원가입·로그인 구현
- Admin 페이지 및 사용자 UI(Popover, My Page) 구현
- 챌린지 모드 및 챌린지 랭킹 표시 기능 구현
> 프로젝트를 진행하며 가장 인상 깊었던 부분은 JWT 기반 인증 구조를 실제 서비스 흐름에 적용해 본 경험이었다.   
단순히 로그인 기능을 구현하는 것을 넘어, 이메일 인증을 포함한 회원가입 단계부터 토큰 발급, 인증 상태 유지까지 전 과정을 설계하며 인증 로직이 서비스 전반에 미치는 영향을 체감할 수 있었다.   
특히 Front와 Back을 함께 다루는 Mono Repo 환경에서, 인증 상태에 따라 접근 가능한 기능과 화면이 달라지는 구조를 구현하면서 보안과 사용자 경험을 동시에 고려해야 한다는 점을 배웠다.   
또한 Admin, Challenge, Follow와 같은 도메인을 구현하며 권한에 따른 기능 분리의 중요성을 느꼈고, JWT를 기반으로 한 인증 정보가 각 도메인에서 일관되게 활용될 수 있도록 구조를 맞추는 과정이 의미 있었다.   
이번 프로젝트를 통해 인증 기능을 단순한 로그인 수단이 아닌, 서비스 전체 구조를 관통하는 핵심 요소로 바라보는 시각을 갖게 되었다.  

## 향후 계획

- 이미지 리사이징/압축으로 로딩 속도 개선

- WebSocket을 이용한 실시간 알림 및 랭킹 환경 개선

- 생성형 AI를 이용한 퀴즈 자동 생성 기능 추가

- Docker를 활용한 새로운 아키텍처 도입
