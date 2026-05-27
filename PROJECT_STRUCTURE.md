# MoodLog 프로젝트 구조 설명

## 한 줄 요약

> **MoodLog**는 매일 감정을 기록하고, AI(Claude)가 패턴을 분석해서 코칭해주는 감정 일기 웹 서비스입니다.

---

## 전체 구조 개념도

```
[사용자 브라우저]
     │  HTML 파일을 열고, JavaScript로 API 호출
     ▼
[Spring Boot 서버 :9090]  ← 여기가 이 프로젝트의 핵심
     │
     ├── 보안 필터 (JWT 토큰 검증)
     ├── Controller (요청을 받는 창구)
     ├── Service (실제 비즈니스 로직)
     ├── Repository (DB와 대화)
     └── MySQL 데이터베이스

[Anthropic Claude API]  ← AI 분석 요청 시에만 외부 호출
```

---

## 기술 스택

| 분류 | 도구 | 설명 |
|---|---|---|
| 언어 | Java 21 | 서버 코드 작성 언어 |
| 프레임워크 | Spring Boot 3.5 | Java로 웹 서버를 쉽게 만드는 틀 |
| DB | MySQL 8.0 | 데이터를 영구 저장하는 데이터베이스 |
| 보안 | Spring Security + JWT | 로그인·인증 처리 |
| AI | Anthropic Claude API | 감정 패턴 분석 |
| 프론트엔드 | 순수 HTML + JavaScript | 서버가 파일을 제공, 브라우저에서 실행 |

---

## 폴더/파일 구조 설명

### 최상위 루트

```
MoodLog/
├── build.gradle          # 프로젝트 의존성 설정 (Maven의 pom.xml 역할)
├── settings.gradle       # 프로젝트 이름 설정
├── gradlew / gradlew.bat # Gradle 빌드 실행 스크립트
├── jwt-test.http         # JWT API 테스트용 HTTP 요청 모음
└── MoodLog_SPEC.md       # 프로젝트 전체 명세서 (설계 문서)
```

### 설정 파일 (`src/main/resources/`)

```
application-local.yaml    # 개발자 로컬 환경 설정
application-dev.yaml      # 개발 서버 환경 (SQL 로그 켜짐)
application-prod.yaml     # 운영 서버 환경 (로그 최소화)
application-secret.yaml   # DB 비밀번호, API 키 (git에 올리지 않음)
application-secret.yaml.example  # 위 파일의 예시 템플릿
```

> `application-secret.yaml`은 `.gitignore`로 보호되어 GitHub에 올라가지 않습니다.

---

### Java 소스 코드 (`src/main/java/com/example/moodlog/`)

#### 시작점

```
MoodLogApplication.java     # 서버 실행 진입점. main() 메서드가 여기 있음
DataInitializer.java        # 서버 시작 시 테스트용 초기 데이터 자동 생성
```

---

#### `common/` — 공통 유틸리티

```
ApiResponse.java            # 모든 API 응답을 { success, data, message } 형태로 통일
GlobalExceptionHandler.java # 예외 발생 시 일관된 에러 응답 반환
exception/
  NotFoundException.java    # 404 에러 (데이터 없을 때)
  ForbiddenException.java   # 403 에러 (권한 없을 때)
  ConflictException.java    # 409 에러 (중복 데이터일 때)
```

---

#### `config/` — 설정 클래스

```
JwtSecurityConfig.java      # 보안 핵심 설정
                            # - 어떤 URL은 로그인 없이 접근 가능한지 정의
                            # - JWT 필터를 Security 체인에 연결
                            # - 소셜 로그인(OAuth2) 설정
                            # - 비밀번호를 BCrypt로 암호화

SchedulingConfig.java       # @Scheduled 기능 활성화
                            # → 만료된 블랙리스트 토큰을 1시간마다 자동 청소

WebConfig.java              # 파일 업로드 경로 등 웹 관련 설정
```

---

#### `security/` — 보안 처리

```
jwt/
  JwtTokenProvider.java         # JWT 토큰 생성·해석 전담
                                # - createToken(): Access Token 발급
                                # - createRefreshToken(): Refresh Token 발급 (7일 유효)
                                # - getUserId(): 토큰에서 사용자 ID 추출

  JwtAuthenticationFilter.java  # 모든 HTTP 요청을 가로채는 필터
                                # - Authorization 헤더에서 토큰 추출
                                # - 토큰 유효성 검사
                                # - 블랙리스트 체크 (로그아웃 여부 확인)

oauth2/
  OAuth2SuccessHandler.java     # 소셜 로그인 성공 후 처리
                                # - 신규 회원이면 DB에 자동 저장
                                # - JWT 토큰 발급
                                # - 일회용 코드 생성 후 클라이언트로 리다이렉트

  OAuthTokenStore.java          # 소셜 로그인용 30초짜리 일회용 코드 저장소
                                # (소셜 로그인 후 URL에 토큰을 직접 노출하지 않기 위한 보안 장치)

  CustomAuthorizationRequestResolver.java
                                # 카카오/구글/네이버 로그인 시
                                # 항상 계정 선택 화면이 뜨도록 파라미터 추가
```

---

#### `domain/` — 핵심 비즈니스 로직

Spring에서는 기능을 도메인(역할)별로 묶어서 관리합니다.  
각 도메인은 `entity → repository → service → controller → dto` 구조를 따릅니다.

---

##### `domain/auth/` — 인증 (로그인/로그아웃)

```
entity/
  RefreshToken.java             # DB에 저장되는 Refresh Token 테이블 매핑
  BlacklistedToken.java         # 로그아웃된 토큰의 JTI를 저장하는 블랙리스트 테이블

repository/
  RefreshTokenRepository.java       # RefreshToken DB 조작 (JPA)
  BlacklistedTokenRepository.java   # 블랙리스트 DB 조작

service/
  RefreshTokenService.java          # Refresh Token 저장·검증·삭제 로직
  BlacklistedTokenService.java      # 로그아웃 토큰 등록·조회·만료 청소 로직

controller/
  AuthController.java               # 인증 API 엔드포인트
                                    # POST /api/auth/login    → 로그인
                                    # POST /api/auth/refresh  → 토큰 갱신
                                    # GET  /api/auth/oauth-token → 소셜 일회용 코드 교환

dto/
  LoginRequest.java                 # 로그인 요청 데이터 { email, password }
  LoginResponse.java                # 로그인 응답 데이터 { accessToken, refreshToken }
  RefreshRequest.java               # 토큰 갱신 요청 { refreshToken }
```

---

##### `domain/user/` — 사용자 관리

```
entity/
  User.java                     # 사용자 테이블 (email, password, nickname, 소셜 정보)
  UserProfile.java              # 프로필 테이블 (bio, 프로필 이미지 경로)
                                # ※ User와 1:1 관계 — 기본 정보와 표시 정보를 분리

repository/
  UserRepository.java           # User DB 조작
  UserProfileRepository.java    # UserProfile DB 조작

security/
  CustomerUserDetails.java          # Spring Security가 사용하는 사용자 정보 래퍼
  CustomerUserDetailsService.java   # DB에서 사용자를 찾아 Spring Security에 제공

service/
  UserService.java              # 회원가입, 로그아웃 로직
  UserProfileService.java       # 프로필 조회·수정, 이미지 업로드 로직

controller/
  UserApiController.java        # POST /api/signup   → 회원가입
                                # POST /api/logout    → 로그아웃
  UserProfileApiController.java # GET/PUT /api/profile       → 프로필 조회/수정
                                # POST    /api/profile/image → 이미지 업로드

dto/
  SignupRequest.java            # 회원가입 입력 데이터
  ProfileResponse.java          # 프로필 응답 데이터
  ProfileUpdateRequest.java     # 프로필 수정 입력 데이터
  ImageUploadResponse.java      # 이미지 업로드 결과
```

---

##### `domain/mood/` — 감정 기록 (핵심 기능)

```
entity/
  MoodRecord.java               # 감정 기록 테이블
                                # (user, mood, memo, tagText, recordDate)
  MoodType.java                 # 감정 단계 ENUM
                                # VERY_GOOD / GOOD / NORMAL / BAD / VERY_BAD

repository/
  MoodRecordRepository.java     # 감정 기록 DB 조작
                                # (날짜별 조회, 월별 조회, 태그 검색 등)

service/
  MoodService.java              # 감정 기록 비즈니스 로직
                                # - 하루 1회만 허용 (중복 방지)
                                # - 당일 기록만 수정 허용
                                # - 태그는 "발표,회의,피곤" 형태로 저장

controller/
  MoodApiController.java        # 감정 기록 API
                                # POST /api/mood/write      → 기록 작성
                                # GET  /api/mood/list       → 목록 조회
                                # GET  /api/mood/today      → 오늘 기록 여부
                                # GET  /api/mood/view/{id}  → 상세 보기
                                # PUT  /api/mood/edit/{id}  → 수정

dto/
  MoodRequest.java              # 기록 작성 입력 데이터
  MoodStatResponse.java         # 이번 달 감정 통계 응답
  TodayCheckResponse.java       # { hasTodayRecord: true/false }
```

---

##### `domain/analysis/` — AI 분석

```
AnalysisController.java         # AI 분석 API
                                # GET /api/analysis               → 태그 통계 + AI 분석
                                # GET /api/analysis/coaching      → 감정 코칭 (7일/30일)
                                # GET /api/analysis/stats/monthly → 이번 달 통계

service/
  AnalysisService.java          # 분석 데이터 조합
                                # - 태그별 감정 분포 집계
                                # - Claude AI에게 보낼 프롬프트 생성

  ClaudeApiService.java         # Anthropic Claude API 실제 호출
                                # - model: claude-haiku-4-5
                                # - 30초 타임아웃
                                # - WebClient(비동기 HTTP)로 REST 호출
```

---

### 프론트엔드 (`src/main/resources/static/`)

서버가 이 HTML 파일들을 그대로 브라우저에 제공합니다. 별도 빌드 과정 없이 Vanilla JS로 동작합니다.

```
index.html        # 메인 화면
                  # - 로그인 여부 확인 및 닉네임 표시
                  # - 오늘 감정 기록 여부 배너
                  # - 이번 주 감정 흐름 (이모지 도트 시각화)

login.html        # 로그인 화면
                  # - 이메일/비밀번호 로그인
                  # - 카카오 / 구글 / 네이버 소셜 로그인 버튼

signup.html       # 회원가입 화면

mypage.html       # 마이페이지
                  # - 닉네임, 상태메시지 수정
                  # - 프로필 이미지 업로드

analysis.html     # AI 분석 화면
                  # - 태그별 감정 분포 통계
                  # - Claude AI 코칭 카드 (7일 / 30일 선택)

mood/
  write.html      # 감정 기록 작성 (감정 선택 + 메모 + 태그)
  list.html       # 기록 목록 + 달력 뷰 (월별 네비게이션)
  view.html       # 기록 상세 보기
  edit.html       # 기록 수정
```

---

## 데이터 흐름 예시 — "감정 기록 작성"

```
1. 사용자가 mood/write.html에서 감정 선택 후 [저장] 클릭

2. JavaScript가 POST /api/mood/write 요청 전송
   헤더: Authorization: Bearer {accessToken}
   바디: { mood: "GOOD", memo: "오늘 날씨가 좋았다", tagText: "산책,커피" }

3. JwtAuthenticationFilter가 토큰을 검증하고 사용자 확인

4. MoodApiController가 요청을 받아 MoodService에 전달

5. MoodService가:
   - 오늘 이미 기록이 있는지 확인 → 있으면 409 에러
   - MoodRecord 객체 생성 후 DB 저장

6. 200 OK 응답 반환 → 브라우저가 list.html로 이동
```

---

## 소셜 로그인 보안 흐름

소셜 로그인은 JWT를 URL에 직접 노출하면 안 되기 때문에 **일회용 코드 교환** 방식을 사용합니다.

```
카카오 로그인 버튼 클릭
    → /oauth2/authorization/kakao (카카오 인증 서버로 이동)
    → 카카오에서 인증 완료
    → 서버의 OAuth2SuccessHandler 실행
        - DB에 사용자 저장 (신규) 또는 조회 (기존)
        - JWT 토큰 발급
        - 30초짜리 일회용 코드 생성 → OAuthTokenStore에 저장
    → /?code=abc123 으로 리다이렉트

브라우저의 index.html이 code 파라미터 감지
    → GET /api/auth/oauth-token?code=abc123
    → 서버가 코드로 JWT 토큰 반환 (코드는 즉시 삭제)
    → localStorage에 토큰 저장 완료
```

---

## 각 레이어의 역할 요약

| 레이어 | 역할 | 비유 |
|---|---|---|
| **Controller** | HTTP 요청/응답 처리 | 식당 홀 직원 (주문 받기) |
| **Service** | 비즈니스 로직 실행 | 주방장 (요리하기) |
| **Repository** | DB 읽기/쓰기 | 냉장고·창고 (재료 꺼내기) |
| **Entity** | DB 테이블 구조 정의 | 식재료 명세서 |
| **DTO** | 요청/응답 데이터 형태 | 주문서 양식 |
| **Security Filter** | 모든 요청에서 인증 확인 | 입구 경비원 |
