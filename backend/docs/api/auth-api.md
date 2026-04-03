# Auth API

> 회원가입과 로그인 기능을 구현하기 전에 참고하는 API 설계 초안입니다.

## 문서 목적

- 즉흥적으로 API를 만들지 않고, 요청과 응답 형식을 먼저 합의하기
- 현재 프로젝트의 JWT 인증 흐름에 맞는 최소한의 인증 API를 정의하기
- 학습용 프로젝트 기준으로 복잡도를 낮추되, 실제 개발 습관에 맞게 문서부터 설계하기

## 범위

이 문서는 아래 API를 다룹니다.

- `POST /api/user/signup`
- `POST /api/user/login`
- `GET /api/user/me`

`/api/user/me`는 로그인 이후 JWT 인증이 실제로 동작하는지 확인하기 위한 보호 API입니다.

## 설계 원칙

- 사용자 정보는 MySQL `users` 테이블에 저장
- 비밀번호는 평문 저장 금지, `BCryptPasswordEncoder`로 암호화
- Access Token은 JWT로 발급
- Access Token은 응답 본문으로 반환
- 인증이 필요한 요청은 `Authorization: Bearer <token>` 헤더 사용
- 초기 학습 단계에서는 Refresh Token과 Redis는 제외

## 사용자 테이블 기준 필드 매핑

현재 스키마 기준으로 우선 사용하는 필드는 아래와 같습니다.

| DB 컬럼 | 의미 | API 사용 여부 |
|--------|------|---------------|
| `user_id` | 로그인 아이디 | 사용 |
| `user_pw` | 암호화된 비밀번호 | 사용 |
| `user_name` | 사용자 이름 | 사용 |
| `email` | 이메일 | 사용 |
| `role` | 권한 | 사용 |
| `login_type` | 로그인 방식 | 기본값 사용 |
| `social_id` | 소셜 로그인 식별자 | 지금은 미사용 |
| `is_active` | 활성 여부 | 선택적으로 사용 |

## 1. 회원가입 API

### Endpoint

`POST /api/user/signup`

### 설명

새로운 로컬 사용자를 등록합니다.

### Request Body

```json
{
  "userId": "minimings",
  "password": "1234",
  "userName": "민밍스",
  "email": "minimings@example.com"
}
```

### Request 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `userId` | string | 예 | 로그인 아이디 |
| `password` | string | 예 | 원본 비밀번호 |
| `userName` | string | 예 | 사용자 이름 |
| `email` | string | 예 | 이메일 |

### 검증 규칙 초안

- `userId`
  - 공백 불가
  - 4자 이상 20자 이하 권장
  - 중복 불가
- `password`
  - 공백 불가
  - 8자 이상 권장
- `userName`
  - 공백 불가
- `email`
  - 이메일 형식 확인
  - 중복 불가

### 처리 흐름

1. 요청값 검증
2. `userId` 중복 확인
3. `email` 중복 확인
4. 비밀번호 암호화
5. `users` 테이블에 저장
6. 성공 응답 반환

### Success Response

`201 Created`

```json
{
  "message": "회원가입이 완료되었습니다.",
  "userId": "minimings"
}
```

### Failure Response 예시

`409 Conflict`

```json
{
  "message": "이미 사용 중인 아이디입니다."
}
```

또는

```json
{
  "message": "이미 사용 중인 이메일입니다."
}
```

## 2. 로그인 API

### Endpoint

`POST /api/user/login`

### 설명

사용자 아이디와 비밀번호를 검증한 뒤 Access Token을 발급합니다.

### Request Body

```json
{
  "userId": "minimings",
  "password": "1234"
}
```

### Request 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `userId` | string | 예 | 로그인 아이디 |
| `password` | string | 예 | 원본 비밀번호 |

### 처리 흐름

1. 요청값 검증
2. `userId`로 사용자 조회
3. 사용자 존재 여부 확인
4. 암호화된 비밀번호와 입력 비밀번호 비교
5. 사용자 권한 확인
6. `JwtTokenProvider.createToken(userId, roles)` 호출
7. Access Token 반환

### Success Response

`200 OK`

```json
{
  "grantType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "minimings",
  "role": "ROLE_USER"
}
```

### Failure Response 예시

`401 Unauthorized`

```json
{
  "message": "아이디 또는 비밀번호가 올바르지 않습니다."
}
```

## 3. 내 정보 조회 API

### Endpoint

`GET /api/user/me`

### 설명

현재 로그인한 사용자 정보를 반환합니다. JWT 인증이 잘 연결되었는지 확인하는 용도로 먼저 구현합니다.

### Request Header

```http
Authorization: Bearer <accessToken>
```

### 처리 흐름

1. `JwtAuthenticationFilter`가 Bearer 토큰 추출
2. 토큰 유효성 검증
3. `UserDetailsService`를 통해 사용자 조회
4. `SecurityContext`에 인증 정보 저장
5. 컨트롤러에서 현재 사용자 정보 반환

### Success Response

`200 OK`

```json
{
  "userId": "minimings",
  "userName": "민밍스",
  "email": "minimings@example.com",
  "role": "ROLE_USER"
}
```

### Failure Response 예시

`401 Unauthorized`

```json
{
  "message": "인증이 필요합니다."
}
```

## 에러 응답 형식 제안

학습용 프로젝트에서는 우선 단순한 형식으로 시작하는 것이 좋습니다.

```json
{
  "message": "에러 메시지"
}
```

이후 필요하면 아래처럼 확장할 수 있습니다.

```json
{
  "code": "AUTH_001",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다.",
  "timestamp": "2026-04-03T10:00:00"
}
```

## 추천 DTO 초안

### SignupRequest

```json
{
  "userId": "string",
  "password": "string",
  "userName": "string",
  "email": "string"
}
```

### LoginRequest

```json
{
  "userId": "string",
  "password": "string"
}
```

### LoginResponse

```json
{
  "grantType": "Bearer",
  "accessToken": "string",
  "userId": "string",
  "role": "ROLE_USER"
}
```

### MeResponse

```json
{
  "userId": "string",
  "userName": "string",
  "email": "string",
  "role": "ROLE_USER"
}
```

## 구현 순서 추천

1. `SignupRequest`, `LoginRequest`, `LoginResponse`, `MeResponse` DTO 작성
2. `User` 모델 작성
3. 사용자 조회 및 저장용 Mapper 작성
4. 회원가입 Service 구현
5. 로그인 Service 구현
6. `UserDetailsService` 구현
7. `/api/user/me` 구현
8. 예외 응답 정리

## 나중에 확장할 항목

- Refresh Token 도입
- Redis 기반 Refresh Token 저장
- 로그아웃 API
- 토큰 재발급 API
- 소셜 로그인 API
- 이메일 인증 및 비밀번호 재설정

## 현재 문서의 성격

이 문서는 첫 구현을 위한 초안입니다. 실제 코드를 작성하면서 필드명이나 응답 구조는 조금 바뀔 수 있지만, 큰 흐름은 이 문서를 기준으로 유지하는 것을 권장합니다.
