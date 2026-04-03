# Mini Mings Backend

> Spring Boot + JWT 기반 사용자 인증 시스템

## 📌 프로젝트 소개
JWT를 활용한 안전한 사용자 인증 시스템입니다. 추후 소셜 로그인과 프론트엔드 연동을 목표로 개발 중입니다.

## ✨ 주요 기능

### ✅ 구현 완료
- [x] **JWT 인증 인프라 구축**
  - JWT 토큰 생성 및 검증 유틸리티
  - Spring Security 통합
  - JWT 인증 필터 구현
- [x] **데이터베이스 스키마 설계**
  - 사용자 테이블 (소셜 로그인 대비)
  - 역할 기반 접근 제어 (RBAC)
- [x] **MyBatis 설정**
  - 데이터베이스 연동
  - SQL 매퍼 구성

### 🚧 개발 예정
- [ ] 회원가입/로그인 API 구현
- [ ] 웹 프론트엔드 연동 (브라우저 환경)
- [ ] OAuth 2.0 소셜 로그인 (Google, Kakao)
- [ ] 사용자 프로필 관리
- [ ] 비밀번호 재설정 기능

## 🛠 기술 스택

**Backend**
- Java 17
- Spring Boot 3.x
- Spring Security + JWT
- MyBatis
- Gradle

**Database**
- MySQL 8.0

**Security**
- JWT (HS512 알고리즘)
- BCrypt Password Encoder

## 🚀 시작하기

### 사전 요구사항
- JDK 17 이상
- Gradle 7.x 이상
- MySQL 8.0 이상

### 애플리케이션 설정
`src/main/resources/application.yml` 파일을 생성하고 다음 내용을 설정하세요:

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
│   ├── domain/
│   │   └── user/              # 사용자 도메인
│   │       ├── dto/           # 데이터 전송 객체
│   │       ├── mapper/        # MyBatis 매퍼 인터페이스
│   │       ├── model/         # 도메인 모델 (User)
│   │       ├── controller/    # REST API (개발 예정)
│   │       └── service/       # 비즈니스 로직 (개발 예정)
│   └── global/
│       ├── config/            # Spring Security 설정
│       ├── filter/            # JWT 인증 필터
│       ├── util/              # JWT 유틸리티
│       ├── error/             # 예외 처리
│       └── common/            # 공통 코드
└── resources/
    ├── db/migration/          # SQL 스크립트
    ├── mapper/                # MyBatis XML 매퍼
    └── application.yml        # 설정 파일 (git ignored)
```

## 🗄 데이터베이스 스키마
`src/main/resources/db/migration/` 폴더의 SQL 스크립트 참고.

## 📚 API 문서
*(구현 후 추가 예정)*

## 📝 라이센스
MIT License
