# Mini Mings Backend

> Spring Boot + JWT 기반 사용자 인증 시스템

## 📌 프로젝트 소개
JWT를 활용한 안전한 사용자 인증 시스템입니다. 추후 소셜 로그인과 프론트엔드 연동을 목표로 개발 중입니다.

## ✨ 주요 기능

### ✅ 현재 구현 상태
- [x] **JWT 인증 인프라 구축**
  - JWT 토큰 생성 및 검증 유틸리티
  - Spring Security 통합
  - JWT 인증 필터 구현
- [x] **데이터베이스 스키마 설계**
  - 사용자 테이블 (소셜 로그인 대비)
  - 사용자 역할 컬럼 설계
- [x] **MyBatis 기본 설정**
  - 데이터베이스 연동
  - 매퍼 스캔 설정

### 🚧 개발 예정
- [ ] 회원가입/로그인 API 구현
- [ ] 웹 프론트엔드 연동 (브라우저 환경)
- [ ] OAuth 2.0 소셜 로그인 (Google, Kakao)
- [ ] 사용자 프로필 관리
- [ ] 비밀번호 재설정 기능

## 🛠 기술 스택

**Backend**
- Java 21
- Spring Boot 4.0.1
- Spring Security + JWT
- MyBatis
- Gradle

**Database**
- MySQL 8.0

**Security**
- JWT (HS256 알고리즘)
- BCrypt Password Encoder

## 🚀 시작하기

### 사전 요구사항
- JDK 21 이상
- Gradle Wrapper 사용 권장
- MySQL 8.0 이상

### 애플리케이션 설정
현재 저장소에는 `src/main/resources/application.yml`이 포함되어 있습니다. 로컬 환경에 맞게 값을 수정하세요:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mini_mings?serverTimezone=Asia/Seoul&characterEncoding=utf8
    username: your_db_username
    password: your_db_password

jwt:
  header: Authorization
  secret: your_jwt_secret_key_at_least_64_bytes_long
  token-validity-in-seconds: 86400
```

### 실행
```bash
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## 🏗 프로젝트 구조
```
src/main/
├── java/me/minimings/backend/
│   ├── global/
│       ├── config/            # Spring Security 설정
│       ├── filter/            # JWT 인증 필터
│       ├── util/              # JWT 유틸리티
│       └── ...                # 공통/예외 패키지 확장 예정
│   └── domain/                # 사용자 도메인 패키지 추가 예정
│       └── user/
│           ├── dto/
│           ├── mapper/
│           ├── model/
│           ├── controller/
│           └── service/
└── resources/
    ├── db/migration/          # SQL 스크립트
    ├── mapper/                # MyBatis XML 매퍼 추가 예정
    └── application.yml        # 설정 파일
```

## 🗄 데이터베이스 스키마
`src/main/resources/db/migration/` 폴더의 SQL 스크립트 참고.

## 📚 문서
- [docs/architecture/auth-flow.md](./docs/architecture/auth-flow.md) - 현재 프로젝트 기준 인증 흐름
- [docs/api/auth-api.md](./docs/api/auth-api.md) - 회원가입, 로그인, 내 정보 조회 API 설계 초안
- [docs/study/jwt.md](./docs/study/jwt.md) - JWT 개념 학습 문서
- [docs/study/spring_security.md](./docs/study/spring_security.md) - Spring Security 개념 학습 문서

## 📚 API 문서
- [docs/api/auth-api.md](./docs/api/auth-api.md)

## 📝 라이센스
MIT License
