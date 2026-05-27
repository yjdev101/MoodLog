# MoodLog 프로젝트 명세서

> 최초 작성: 2026-04-22 · 최종 수정: 2026-04-22

---

## 1. 프로젝트 개요

**MoodLog**는 사용자가 매일 자신의 감정을 기록하고, 누적된 데이터를 기반으로 AI가 감정 패턴을 분석·코칭해주는 감정 일기 서비스입니다.

| 항목 | 내용 |
|---|---|
| 서비스명 | MoodLog |
| 서버 포트 | 9090 |
| 배포 환경 | 로컬 (localhost) |
| 타겟 사용자 | 자신의 감정 변화를 추적하고 싶은 개인 사용자 |

**핵심 컨셉**
- 하루 한 번 감정(5단계)과 메모·태그를 기록
- 달력 뷰로 월별 감정 흐름을 시각적으로 확인
- Claude AI가 태그 통계와 기록 패턴을 분석해 코칭 제공

---

## 2. 기술 스택

### Backend
| 분류 | 기술                                      |
|---|-----------------------------------------|
| 언어 | Java 17                                 |
| 프레임워크 | Spring Boot 3.5.11                      |
| ORM | Spring Data JPA + Hibernate 6           |
| 보안 | Spring Security + JWT (jjwt 0.11.5)     |
| 소셜 로그인 | Spring Security OAuth2 Client           |
| AI 연동 | Anthropic Claude API (claude-haiku-4-5) |
| HTTP 클라이언트 | Spring WebFlux (WebClient)              |
| 빌드 도구 | Gradle                                  |

### Frontend
| 분류 | 기술 |
|---|---|
| 구성 방식 | Vanilla JavaScript (SPA 방식의 정적 HTML) |
| CSS | Tailwind CSS (CDN) |
| 폰트 | Noto Sans KR (Google Fonts) |
| 토큰 저장 | localStorage |

### Database
| 분류 | 기술 |
|---|---|
| DBMS | MySQL 8.0 |
| 커넥션 풀 | HikariCP |
| DDL 전략 | hibernate.ddl-auto: update (dev) / validate (prod) |

---

## 3. 주요 기능 목록

### 인증
- 이메일/비밀번호 회원가입 및 로그인
- 카카오 소셜 로그인 (OAuth2) — 계정 선택 화면 강제
- 구글 소셜 로그인 (OAuth2) — 계정 선택 화면 강제 (`prompt=select_account`)
- 네이버 소셜 로그인 (OAuth2) — 계정 선택 화면 강제 (`auth_type=reprompt`)
- JWT Access Token + Refresh Token 인증 (소셜 로그인 포함)
- 소셜 로그인 토큰 전달: 일회용 코드(30초 유효) → 클라이언트가 서버에 교환
- 로그아웃 (토큰 JTI를 DB 블랙리스트에 등록, 서버 재시작 후에도 유지)

### 감정 기록
- 하루 한 번 감정 기록 (중복 방지)
- 감정 5단계 선택: VERY_GOOD / GOOD / NORMAL / BAD / VERY_BAD
- 메모 작성 (선택)
- 태그 입력 (쉼표 구분, 선택)
- 당일 기록만 수정 허용

### 기록 조회
- 전체 기록 목록 조회
- 달력 뷰 (월별 네비게이션, 날짜별 감정 이모지 표시)
- 이번 달 감정 통계 (감정별 횟수, 총 기록 수)
- 상세 보기

### AI 분석
- **태그 분석**: 전체 기록에서 태그별 감정 분포를 집계하고 Claude AI가 패턴 해석. 태그 감정 성향(긍정/부정/중립)에 따라 색상 하이라이트 표시
- **감정 코칭**: 최근 7일 또는 30일 기록을 AI가 분석하여 감정 흐름 요약 / 주요 요인 분석 / 맞춤 조언 3개 카드로 분리 제공

### 메인 화면
- 로그인 상태 확인 및 닉네임 표시
- 오늘 기분 기록 여부 확인
- **이번 주 감정 흐름**: 최근 7일 감정을 이모지 도트로 시각화 (기록 없는 날은 점선 원)
- 마이페이지 / AI 기분 분석 빠른 메뉴

### 마이페이지
- 닉네임 조회 및 수정
- 상태메시지(bio) 수정
- 프로필 이미지 업로드 (서버 로컬 저장)

---

## 4. API 엔드포인트

> 인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 포함해야 합니다.

### 인증 (Auth)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/login` | 불필요 | 이메일/비밀번호 로그인 |
| POST | `/api/auth/refresh` | 불필요 | Access Token 갱신 |
| GET | `/api/auth/oauth-token` | 불필요 | 소셜 로그인 일회용 코드 → 토큰 교환 |
| POST | `/api/signup` | 불필요 | 회원가입 |
| POST | `/api/logout` | 필요 | 로그아웃 (토큰 블랙리스트 등록) |

**POST `/api/auth/login`**
```json
// 요청
{ "email": "user@example.com", "password": "Pass123$" }

// 응답
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

**POST `/api/auth/refresh`**
```json
// 요청
{ "refreshToken": "eyJ..." }

// 응답
"eyJ..." // 새 Access Token
```

**GET `/api/auth/oauth-token?code={code}`**
```json
// 응답 (30초 이내 1회만 유효)
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

---

### 소셜 로그인 (OAuth2)

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/oauth2/authorization/kakao` | 카카오 로그인 시작 |
| GET | `/oauth2/authorization/google` | 구글 로그인 시작 |
| GET | `/oauth2/authorization/naver` | 네이버 로그인 시작 |

- 로그인 성공 시 `/?code={일회용코드}` 로 리다이렉트
- 클라이언트가 `GET /api/auth/oauth-token?code=` 를 호출해 실제 토큰 수령

---

### 사용자 (User)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/mypage` | 필요 | 현재 사용자 닉네임 조회 |

---

### 프로필 (Profile)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/profile` | 필요 | 프로필 조회 |
| PUT | `/api/profile` | 필요 | 닉네임·상태메시지 수정 |
| POST | `/api/profile/image` | 필요 | 프로필 이미지 업로드 |

**GET `/api/profile`**
```json
// 응답
{ "nickname": "닉네임", "bio": "상태메시지", "profileImagePath": "/upload/abc.png" }
```

---

### 감정 기록 (Mood)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/mood/write` | 필요 | 감정 기록 작성 |
| GET | `/api/mood/list` | 필요 | 전체 기록 목록 조회 |
| GET | `/api/mood/today` | 필요 | 오늘 기록 여부 확인 |
| GET | `/api/mood/view/{id}` | 필요 | 특정 기록 상세 조회 |
| PUT | `/api/mood/edit/{id}` | 필요 | 기록 수정 (당일만 가능) |

**GET `/api/mood/today`**
```json
// 응답
{ "hasTodayRecord": true }
```

**감정 단계 값**

| 값 | 의미 |
|---|---|
| `VERY_GOOD` | 매우 좋음 |
| `GOOD` | 좋음 |
| `NORMAL` | 보통 |
| `BAD` | 나쁨 |
| `VERY_BAD` | 매우 나쁨 |

---

### AI 분석 (Analysis)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/analysis` | 필요 | 전체 태그 통계 + AI 분석 |
| GET | `/api/analysis/coaching?period=7` | 필요 | 기간별 감정 코칭 (7일 or 30일) |
| GET | `/api/analysis/stats/monthly` | 필요 | 이번 달 감정 통계 |

**GET `/api/analysis`**
```json
// 응답
{
  "tagStats": {
    "운동": { "GOOD": 3, "VERY_GOOD": 1 },
    "야근": { "BAD": 4, "VERY_BAD": 2 }
  },
  "aiComment": "운동할 때 기분이 좋아지는 패턴이 보여요..."
}
```

**GET `/api/analysis/coaching?period=7`**
```json
// 응답
{ "coaching": "1. 감정 흐름...\n2. 주요 요인...\n3. 조언...", "period": 7, "recordCount": 5 }
```

---

## 5. DB 구조

### ERD (관계 요약)

```
User (1) ──── (1) UserProfile
User (1) ──── (N) MoodRecord
User (1) ──── (1) RefreshToken
BlacklistedToken (독립)
```

---

### user 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| email | VARCHAR(255) | UNIQUE, NULL 허용 | 이메일 (소셜 로그인은 null) |
| password | VARCHAR(255) | NULL 허용 | BCrypt 암호화 비밀번호 |
| nickname | VARCHAR(255) | NOT NULL | 닉네임 |
| profile_image | VARCHAR(255) | | 소셜 로그인 프로필 이미지 URL |
| provider | VARCHAR(255) | | 소셜 제공자 (kakao / google / naver) |
| provider_id | VARCHAR(255) | | 소셜 제공자 식별자 |
| created_at | DATETIME | NOT NULL | 가입 일시 |

---

### user_profile 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 프로필 ID |
| user_id | BIGINT | FK → user.id, UNIQUE | 사용자 참조 |
| nickname | VARCHAR(255) | | 표시 닉네임 |
| bio | TEXT | | 상태메시지 |
| profile_image_path | VARCHAR(255) | | 업로드 이미지 경로 |
| created_at | DATETIME | | 생성 일시 |

---

### mood_record 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 기록 ID |
| user_id | BIGINT | FK → user.id, NOT NULL | 사용자 참조 |
| mood | VARCHAR(255) | NOT NULL | 감정 단계 (ENUM 값) |
| memo | LONGTEXT | | 메모 |
| tag_text | LONGTEXT | | 태그 목록 (CSV 형식) |
| record_date | DATE | NOT NULL | 기록 날짜 |
| created_at | DATETIME | NOT NULL | 생성 일시 |

---

### refresh_token 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 토큰 ID |
| token | VARCHAR(255) | UNIQUE, NOT NULL | Refresh Token 값 |
| user_id | BIGINT | FK → user.id, NOT NULL | 사용자 참조 |
| expiry_date | BIGINT UNSIGNED | NOT NULL | 만료 시각 (Epoch millis) |

---

### blacklisted_token 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | ID |
| jti | VARCHAR(255) | UNIQUE, NOT NULL | JWT ID (로그아웃된 토큰 식별자) |
| expires_at | DATETIME | NOT NULL | JWT 원래 만료 시각 (정리 기준) |

> 매 1시간마다 만료된 항목 자동 정리 (`@Scheduled`)

---

## 6. 아키텍처 구조

### 전체 구조

```
[브라우저 (정적 HTML + JS)]
        │  HTTP REST API
        ▼
[Spring Boot 서버 :9090]
        │
        ├── Security Filter Chain
        │       ├── JwtAuthenticationFilter       -- Bearer 토큰 검증 + DB 블랙리스트 체크
        │       └── OAuth2LoginAuthenticationFilter -- 소셜 로그인 처리
        │
        ├── Controller Layer
        │       ├── AuthController                -- 로그인/토큰 갱신/OAuth 코드 교환
        │       ├── UserApiController             -- 회원가입/로그아웃
        │       ├── UserProfileApiController      -- 프로필 조회/수정
        │       ├── MoodApiController             -- 감정 기록 CRUD
        │       └── AnalysisController            -- AI 분석
        │
        ├── Service Layer
        │       ├── UserService                   -- 사용자 비즈니스 로직
        │       ├── UserProfileService            -- 프로필 비즈니스 로직
        │       ├── MoodService                   -- 감정 기록 비즈니스 로직
        │       ├── RefreshTokenService           -- Refresh Token 관리
        │       ├── BlacklistedTokenService       -- 로그아웃 토큰 블랙리스트 (DB)
        │       ├── AnalysisService               -- AI 분석 조합
        │       └── ClaudeApiService              -- Anthropic API 호출 (30초 타임아웃)
        │
        ├── Repository Layer (Spring Data JPA)
        │       ├── UserRepository
        │       ├── UserProfileRepository
        │       ├── MoodRecordRepository
        │       ├── RefreshTokenRepository
        │       └── BlacklistedTokenRepository
        │
        └── Database (MySQL 8.0)

[Anthropic Claude API]  ◄── ClaudeApiService (WebClient)
```

---

### 패키지 구조

```
com.example.moodlog/
├── MoodLogApplication.java
├── DataInitializer.java
├── common/
│   ├── ApiResponse.java
│   ├── GlobalExceptionHandler.java
│   └── exception/
│       ├── NotFoundException.java       (404)
│       ├── ForbiddenException.java      (403)
│       └── ConflictException.java       (409)
├── config/
│   ├── JwtSecurityConfig.java
│   ├── SchedulingConfig.java            (@EnableScheduling)
│   └── WebConfig.java
├── security/
│   ├── jwt/
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationFilter.java
│   └── oauth2/
│       ├── OAuth2SuccessHandler.java
│       ├── OAuthTokenStore.java         (일회용 코드 저장소)
│       └── CustomAuthorizationRequestResolver.java
├── domain/
│   ├── auth/
│   │   ├── entity/ (RefreshToken, BlacklistedToken)
│   │   ├── repository/ (RefreshTokenRepository, BlacklistedTokenRepository)
│   │   ├── service/ (RefreshTokenService, BlacklistedTokenService)
│   │   ├── controller/AuthController.java
│   │   └── dto/ (LoginRequest, LoginResponse, RefreshRequest)
│   ├── user/
│   │   ├── entity/ (User, UserProfile)
│   │   ├── repository/ (UserRepository, UserProfileRepository)
│   │   ├── security/ (CustomerUserDetails, CustomerUserDetailsService)
│   │   ├── service/ (UserService, UserProfileService)
│   │   ├── controller/ (UserApiController, UserProfileApiController)
│   │   └── dto/ (SignupRequest, ProfileUpdateRequest, ProfileResponse, ImageUploadResponse)
│   ├── mood/
│   │   ├── entity/ (MoodRecord, MoodType)
│   │   ├── repository/MoodRecordRepository.java
│   │   ├── service/MoodService.java
│   │   ├── controller/MoodApiController.java
│   │   └── dto/ (MoodRequest, MoodStatResponse, TodayCheckResponse)
│   └── analysis/
│       ├── AnalysisController.java
│       └── service/
│           ├── AnalysisService.java
│           └── ClaudeApiService.java
```

---

### 인증 플로우

**일반 로그인**
```
클라이언트 → POST /api/auth/login
           ← { accessToken, refreshToken }

이후 요청 → Authorization: Bearer {accessToken}

만료 시   → POST /api/auth/refresh { refreshToken }
           ← 새 accessToken
```

**소셜 로그인**
```
클라이언트 → GET /oauth2/authorization/{provider}
           → (제공자 인증 화면 — 항상 계정 선택 강제)
           ← GET /login/oauth2/code/{provider}  (콜백)
           → OAuth2SuccessHandler
                 ├── DB에서 기존 회원 조회 또는 신규 저장
                 ├── accessToken + refreshToken 발급
                 └── OAuthTokenStore에 30초 일회용 코드 저장
           ← Redirect /?code={일회용코드}
           → GET /api/auth/oauth-token?code={코드}
           ← { accessToken, refreshToken }
```

**로그아웃**
```
클라이언트 → POST /api/logout  (Authorization: Bearer {accessToken})
           → 서버: 토큰 JTI를 blacklisted_token 테이블에 저장
           → 서버: refresh_token 삭제
           ← 200 OK
```

---

### 환경별 설정 파일

| 파일 | git 추적 | 용도 |
|---|---|---|
| `application.yaml` | ✅ | 공통 비민감 설정 |
| `application-secret.yaml` | ❌ | API 키, OAuth Secret, DB 비밀번호 |
| `application-secret.yaml.example` | ✅ | 신규 환경 설정 가이드 |
| `application-dev.yaml` | ✅ | 개발용 (show-sql, DEBUG 로그) |
| `application-prod.yaml` | ✅ | 운영용 (ddl-auto: validate, WARN 로그) |

---

### 정적 파일 구조 (프론트엔드)

```
static/
├── index.html       -- 메인 (닉네임, 오늘 기록 여부, 이번 주 감정 흐름)
├── login.html       -- 로그인 (이메일 / 카카오 / 구글 / 네이버)
├── signup.html      -- 회원가입
├── mypage.html      -- 마이페이지 (프로필 조회/수정)
├── analysis.html    -- AI 분석 (태그 통계 + 감정 코칭 카드)
└── mood/
    ├── write.html   -- 감정 기록 작성
    ├── list.html    -- 목록 + 달력 뷰
    ├── view.html    -- 상세 보기
    └── edit.html    -- 수정
```
