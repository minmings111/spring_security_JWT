# Auth Flow

> 현재 프로젝트 기준으로 JWT 인증이 어떤 구조로 동작하는지 정리한 문서입니다.

## 문서 목적

- `docs/study/`의 개념 문서와 분리해서, 이 프로젝트에 실제로 적용된 인증 구조만 빠르게 파악하기
- 지금 구현된 부분과 아직 구현되지 않은 부분을 함께 확인하기

## 현재 구현 범위

### 구현됨
- `SecurityConfig`를 통한 Spring Security 기본 설정
- `JwtAuthenticationFilter`를 통한 요청별 JWT 검사
- `JwtTokenProvider`를 통한 토큰 생성, 파싱, 검증 로직
- `users` 테이블 스키마 설계
- 인증 제외 경로 설정
  - `POST /api/user/signup`
  - `POST /api/user/login`

### 아직 구현되지 않음
- 사용자 도메인 패키지
- 회원가입 API
- 로그인 API
- `UserDetailsService` 구현
- 사용자 조회용 MyBatis Mapper
- 인증/인가 예외 응답 처리
- 보호 API 예제

## 현재 코드 기준 구성 요소

### 1. SecurityConfig

파일: `src/main/java/me/minimings/backend/global/config/SecurityConfig.java`

역할:
- 세션을 사용하지 않는 `STATELESS` 정책 적용
- `/api/user/signup`, `/api/user/login` 경로는 인증 없이 허용
- 그 외 요청은 인증 필요
- `JwtAuthenticationFilter`를 Spring Security 필터 체인에 등록

핵심 의미:
- 이 프로젝트는 세션 로그인 대신 JWT 기반 인증 흐름을 전제로 함
- 로그인 기능 자체는 아직 없지만, 로그인 이후 요청을 처리할 틀은 준비된 상태

### 2. JwtTokenProvider

파일: `src/main/java/me/minimings/backend/global/util/JwtTokenProvider.java`

역할:
- JWT 생성
- 토큰에서 사용자 식별자 추출
- 토큰 유효성 검증
- 토큰으로부터 `Authentication` 객체 생성

주의할 점:
- `getAuthentication()`은 `UserDetailsService`에 의존함
- 따라서 실제 인증 흐름을 끝까지 동작시키려면 사용자 조회 로직이 먼저 필요함

### 3. JwtAuthenticationFilter

파일: `src/main/java/me/minimings/backend/global/filter/JwtAuthenticationFilter.java`

역할:
- 요청의 `Authorization` 헤더에서 Bearer 토큰 추출
- 토큰 검증 성공 시 `SecurityContextHolder`에 인증 정보 저장
- 인증 제외 경로는 필터를 건너뜀

현재 상태 해석:
- 필터 구조는 준비되어 있음
- 하지만 로그인 API와 사용자 조회 계층이 없어서 전체 인증 시나리오는 아직 완성되지 않음

## 목표 인증 흐름

아래 흐름이 이 프로젝트에서 다음으로 완성되어야 하는 핵심 시나리오입니다.

### 1. 회원가입

`POST /api/user/signup`

예상 흐름:
1. 요청 수신
2. 아이디와 이메일 중복 확인
3. 비밀번호를 `BCryptPasswordEncoder`로 암호화
4. `users` 테이블에 저장
5. 성공 응답 반환

이 단계에서 필요한 것:
- User DTO
- User Model
- User Mapper
- User Service
- User Controller

### 2. 로그인

`POST /api/user/login`

예상 흐름:
1. 요청 수신
2. DB에서 사용자 조회
3. 비밀번호 비교
4. `JwtTokenProvider.createToken(...)` 호출
5. Access Token 반환

이 단계에서 필요한 것:
- 로그인 요청/응답 DTO
- 사용자 조회 Mapper
- 비밀번호 검증 로직
- 역할 정보 기반 토큰 생성

### 3. 인증이 필요한 API 호출

예상 흐름:
1. 클라이언트가 `Authorization: Bearer <token>` 헤더와 함께 요청
2. `JwtAuthenticationFilter`가 토큰 추출
3. `JwtTokenProvider.validateToken()`으로 서명과 만료 시간 검증
4. `JwtTokenProvider.getAuthentication()`으로 인증 객체 생성
5. `SecurityContextHolder`에 인증 저장
6. 컨트롤러 진입

이 단계에서 필요한 것:
- `UserDetailsService` 구현
- 인증 성공 후 테스트할 보호 API

## 구현 순서 추천

1. 사용자 도메인 패키지 생성
2. 회원가입 기능 구현
3. 로그인 기능 구현
4. `UserDetailsService` 연결
5. 보호 API 추가
6. 인증 실패 응답 정리
7. 테스트 코드 추가

## 문서 구분 원칙

- `docs/study/`
  - JWT, Spring Security 같은 개념 학습 문서
- `docs/architecture/`
  - 현재 프로젝트에 실제 적용된 구조와 흐름 문서

이 문서는 앞으로 구현이 진행될수록 실제 코드 기준으로 계속 갱신합니다.
