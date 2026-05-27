# MoodLog 코드리뷰 Q&A 정리

> 코드리뷰 대비 — 2026-05-18 기준

---

## Q1. Controller에서 Repository를 직접 써도 되나요?

**질문**
> `MoodApiController`에 `UserRepository`가 주입되어 있는데, Controller에서 Repository 직접 써도 돼?

**답변**
안 됩니다. Spring의 레이어드 아키텍처(Controller → Service → Repository)에서는 Controller가 Repository를 직접 사용하면 안 됩니다.

**문제 위치**
- `MoodApiController.java` — `UserRepository` 직접 주입
- `UserApiController.java` — 동일 문제

**수정 방향**
`UserService`에 `findById(Long id)` 같은 메서드를 만들고, Controller는 `UserService`를 통해 사용자를 조회해야 합니다.

---

## Q2. `JwtTokenProvider.createRefreshToken()`은 어디서 호출하나요?

**질문**
> `JwtTokenProvider` 안에 `createRefreshToken()` 메서드가 있는데, 이거 어디서 호출해?

**답변**
어디서도 호출하지 않습니다. 데드코드입니다.

**배경 설명**
이 앱의 Refresh Token은 두 가지 방식이 혼재했던 흔적이 있습니다.

| 방식 | 설명 | 실제 사용 여부 |
|---|---|---|
| JWT 방식 | `JwtTokenProvider.createRefreshToken()` | ❌ 사용 안 함 |
| UUID DB 방식 | `RefreshTokenService.createRefreshToken()` | ✅ 실제 사용 |

**실제 Refresh Token 흐름**
```
로그인 → UUID 생성 → DB(refresh_token 테이블)에 저장
만료 시 → POST /api/auth/refresh { refreshToken: "uuid" }
        → DB에서 uuid 조회 + 만료 체크 → 새 accessToken 발급
```

**Access Token vs Refresh Token 무효화 방식**
- Access Token(JWT) → 로그아웃 시 JTI를 `blacklisted_token` 테이블에 저장
- Refresh Token(UUID) → 로그아웃 시 DB에서 삭제

**수정 방향**
`JwtTokenProvider.createRefreshToken()`은 삭제해야 합니다.

---

## Q3. `UserService`에 주입된 `UserProfileService`는 어디서 쓰나요?

**질문**
> `UserService.java` 보면 `UserProfileService`가 주입되어 있는데, 어디서 써?

**답변**
어디서도 사용하지 않는 유령 코드입니다.

**추가 문제**
`signup()` 메서드 안에서 `UserProfileService` 대신 `UserProfileRepository`를 직접 호출하고 있습니다. 이것도 레이어 위반입니다. (Service가 다른 도메인의 Repository를 직접 사용)

```java
// 현재 (잘못된 방식)
userProfileRepository.save(profile);

// 올바른 방식
userProfileService.create(savedUser, nickname);
```

**수정 방향**
- `UserProfileService` 주입 선언 제거
- `userProfileRepository.save()` → `userProfileService.create()` 로 변경

---

## Q4. `ClaudeApiService` 예외처리가 왜 잘못됐나요?

**질문**
> `WebClientResponseException` 잡으면서 왜 "시간을 초과했습니다"라고 출력해?

**개념 정리**

| 예외 | 발생 상황 |
|---|---|
| `WebClientResponseException` | 서버가 HTTP 에러 응답 반환 (4xx, 5xx) |
| `TimeoutException` | 지정한 시간 내 응답 없음 (`.timeout()`) |

**현재 코드의 문제**
```java
} catch (WebClientResponseException e) {
    // 실제로는 401(인증 오류), 429(한도 초과) 등인데
    throw new RuntimeException("AI 분석 요청이 시간을 초과했습니다."); // 엉뚱한 메시지
}
```

`TimeoutException`은 import만 해두고 실제로 catch하지 않아서, 타임아웃 발생 시 `catch (Exception e)`에 걸려 "AI 서비스 오류" 메시지가 나옵니다.

**올바른 구조**
```java
} catch (TimeoutException e) {
    throw new RuntimeException("AI 분석 요청이 시간을 초과했습니다.");
} catch (WebClientResponseException e) {
    throw new RuntimeException("AI API 오류가 발생했습니다: " + e.getStatusCode());
} catch (Exception e) {
    throw new RuntimeException("AI 서비스 오류가 발생했습니다.");
}
```

---

## Q5. `permitAll()`과 `ignoring()`의 차이가 뭔가요?

**질문**
> `JwtSecurityConfig`에서 같은 경로가 `permitAll()`이랑 `ignoring()` 두 군데 다 있는데, 차이가 뭐야?

**차이점**

| | `permitAll()` | `ignoring()` |
|---|---|---|
| Security 필터 실행 | ✅ 실행됨 | ❌ 건너뜀 |
| JWT 필터 실행 | ✅ 실행됨 | ❌ 건너뜀 |
| 비유 | 경비원이 검사는 하되 통과시켜줌 | 경비원 자체가 없는 문 |

**문제**
`ignoring()`에 등록된 경로는 필터 체인 자체에 들어오지 않으므로, 같은 경로를 `permitAll()`에 또 등록해도 의미가 없는 죽은 설정입니다.

**올바른 사용 방향**
- 정적 파일 (`*.html`, `/css/**`, `/js/**`) → `ignoring()`
- API 엔드포인트 (`/api/auth/login` 등) → `permitAll()`

---

## Q6. `OAuth2SuccessHandler`에서 NPE가 발생할 수 있나요?

**NPE란?**
`NullPointerException` — `null`인 객체의 메서드나 필드에 접근할 때 발생하는 예외입니다.
```java
String name = null;
name.length(); // NPE 발생
```

**문제 위치**

| 제공자 | 위험 코드 | NPE 조건 |
|---|---|---|
| 카카오 | `kakaoAccount.get("profile")` | `kakao_account`가 null일 때 |
| 구글 | `oAuth2User.getAttribute("name")` | `name`이 null일 때 |
| 네이버 | `naverResponse.get("id")` | `response`가 null일 때 |

세 제공자 모두 null 체크 없이 바로 `.get()`을 호출합니다. 사용자가 정보 제공 동의를 거부하거나 API 응답 구조가 바뀌면 NPE가 발생할 수 있습니다.

**수정 방향**
각 제공자의 응답 Map을 사용하기 전에 null 체크를 추가해야 합니다.

---

## Q7. Refresh Token 만료 시 어떤 HTTP 상태코드가 반환되나요?

**질문**
> `AuthController`에서 Refresh Token 만료됐을 때 어떤 HTTP 상태코드가 클라이언트한테 가?

**현재 코드**
```java
if (!refreshTokenService.isValid(token)) {
    throw new RuntimeException("Refresh token expired");
}
```

**문제**
`RuntimeException`은 `GlobalExceptionHandler`에서 처리되지 않아 Spring 기본값인 **500 Internal Server Error**가 반환됩니다.

**올바른 상태코드**
Refresh Token 만료는 서버 오류가 아닌 클라이언트 측 문제이므로 **401 Unauthorized**가 맞습니다.

**수정 방향**
```java
throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
```

---

## Q8. `updateMood()`에 `@Transactional`이 없으면 어떤 문제가 생기나요?

**질문**
> `MoodService`에서 `updateMood()` 메서드에 `@Transactional` 없는데 문제 없어?

**`@Transactional`이 없으면**
중간에 오류가 나도 롤백이 되지 않습니다.

**현재 코드의 경우**
`save()` 한 번만 호출하므로 당장은 큰 문제가 없습니다.

**문제가 되는 경우 (예시)**
```java
moodRecordRepository.save(record);    // 1번 저장 성공
logRepository.save(new Log(...));     // 2번 저장 중 에러 발생!
// @Transactional 없으면 1번은 저장된 채로 남아 데이터 불일치 발생
```

`@Transactional`이 있으면 하나라도 실패하면 전체 롤백됩니다.

**수정 방향**
Service의 데이터 변경 메서드에는 습관적으로 `@Transactional`을 붙여야 합니다.

---

## Q9. `orElse()`와 `orElseGet()`의 차이가 뭔가요?

**질문**
> `RefreshTokenService`에서 `orElse()` 쓰는데, `orElseGet()`이랑 차이가 뭐야?

**차이점**

| | `orElse(value)` | `orElseGet(() -> value)` |
|---|---|---|
| 실행 시점 | 값 유무와 관계없이 **항상 실행** | 값이 **없을 때만** 실행 |

**현재 코드의 문제**
```java
refreshTokenRepository.findByUser(user)
    .orElse(RefreshToken.builder()  // DB에 토큰이 있어도 builder가 실행됨
            .user(user)
            .token(UUID.randomUUID().toString())
            .build());              // 객체를 만들었다가 바로 버림
```

**올바른 코드**
```java
refreshTokenRepository.findByUser(user)
    .orElseGet(() -> RefreshToken.builder()  // 토큰이 없을 때만 실행됨
            .user(user)
            .token(UUID.randomUUID().toString())
            .build());
```

**언제 중요하냐면**
`orElse()` 안에 DB 조회나 외부 API 호출 같은 무거운 작업이 있을 경우 심각한 낭비가 됩니다. 습관적으로 `orElseGet()`을 쓰는 것이 안전합니다.

---

## 핵심 요약

| # | 이슈 | 분류 |
|---|---|---|
| 1 | Controller → Repository 직접 사용 (레이어 위반) | 아키텍처 |
| 2 | `createRefreshToken()` 데드코드 | 코드 품질 |
| 3 | `UserProfileService` 미사용 주입 + Repository 직접 사용 | 아키텍처 |
| 4 | `WebClientResponseException`에 잘못된 에러 메시지 | 버그 |
| 5 | `permitAll()`과 `ignoring()` 중복 등록 | 설정 오류 |
| 6 | OAuth2 소셜 로그인 NPE 위험 (카카오/구글/네이버) | 잠재적 버그 |
| 7 | Refresh Token 만료 시 500 반환 (401이어야 함) | 버그 |
| 8 | `updateMood()`에 `@Transactional` 누락 | 코드 품질 |
| 9 | `orElse()` 대신 `orElseGet()` 써야 함 | 코드 품질 |