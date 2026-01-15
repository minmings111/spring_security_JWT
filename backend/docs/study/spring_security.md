# Spring Security 완벽 가이드

> 이 문서는 Spring Security가 무엇이며, 왜 필요하고, 어떻게 동작하는지를 처음부터 끝까지 설명합니다.

## 📚 목차
1. [Spring Security란 무엇인가?](#1-spring-security란-무엇인가)
2. [왜 Spring Security가 필요한가?](#2-왜-spring-security가-필요한가)
3. [Spring Security의 핵심 개념](#3-spring-security의-핵심-개념)
4. [Spring Security 동작 원리](#4-spring-security-동작-원리)
5. [우리 프로젝트의 구현 분석](#5-우리-프로젝트의-구현-분석)
6. [각 설정의 의미와 이유](#6-각-설정의-의미와-이유)

---

## 1. Spring Security란 무엇인가?

### 1.1 정의
**Spring Security**는 Spring 기반 애플리케이션의 **인증(Authentication)**과 **인가(Authorization)**를 담당하는 강력한 보안 프레임워크입니다.

### 1.2 인증(Authentication) vs 인가(Authorization)

#### 인증 (Authentication)
> "당신이 누구인지 확인합니다"

- **목적**: 사용자가 본인이 맞는지 확인
- **예시**: 로그인 (아이디/비밀번호 확인)
- **질문**: "당신은 정말 홍길동입니까?"

#### 인가 (Authorization)
> "당신이 무엇을 할 수 있는지 확인합니다"

- **목적**: 인증된 사용자가 특정 리소스에 접근할 권한이 있는지 확인
- **예시**: 관리자만 회원 목록 조회 가능
- **질문**: "당신은 이 작업을 수행할 권한이 있습니까?"

### 1.3 간단한 비유

```
🏢 회사 건물 출입 시스템

1. 인증 (Authentication)
   - 사원증을 카드 리더기에 찍음
   - 시스템: "이 사람이 우리 회사 직원이 맞네!"
   
2. 인가 (Authorization)
   - 엘리베이터에서 5층 버튼을 누름
   - 시스템: "이 직원은 5층 접근 권한이 있네!" ✅
   - 10층 버튼을 누름
   - 시스템: "이 직원은 10층(임원실) 접근 권한이 없네!" ❌
```

---

## 2. 왜 Spring Security가 필요한가?

### 2.1 보안을 직접 구현하는 경우의 문제점

만약 Spring Security 없이 직접 구현한다면:

```java
// ❌ 나쁜 예: 보안을 직접 구현
@GetMapping("/admin/users")
public List<User> getUsers(HttpServletRequest request) {
    String password = request.getParameter("password");
    
    // 문제 1: 비밀번호 평문 비교 (암호화 없음)
    if (!"admin123".equals(password)) {
        throw new RuntimeException("권한 없음");
    }
    
    // 문제 2: 모든 API마다 이런 코드를 반복해야 함
    // 문제 3: 세션 관리, CSRF 방어 등 직접 구현해야 함
    // 문제 4: 보안 취약점 발생 가능성 높음
    
    return userService.getAllUsers();
}
```

**문제점:**
- 🔴 비밀번호 암호화 안 됨
- 🔴 보안 검증 코드가 비즈니스 로직과 섞임
- 🔴 모든 API에 중복 코드
- 🔴 CSRF, XSS 등 공격에 취약
- 🔴 세션 관리 복잡

### 2.2 Spring Security를 사용하면

```java
// ✅ 좋은 예: Spring Security 사용
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")  // 이 한 줄이면 끝!
public List<User> getUsers() {
    return userService.getAllUsers();
}
```

**장점:**
- ✅ 보안 로직이 비즈니스 로직과 분리됨
- ✅ 코드가 간결하고 읽기 쉬움
- ✅ 검증된 보안 기능 사용
- ✅ 다양한 공격에 대한 방어 기능 내장
- ✅ 표준화된 방식

---

## 3. Spring Security의 핵심 개념

### 3.1 SecurityContext와 Authentication

```
SecurityContextHolder (스레드 저장소)
    └── SecurityContext (보안 컨텍스트)
            └── Authentication (인증 정보)
                    ├── Principal (사용자 정보)
                    ├── Credentials (비밀번호)
                    └── Authorities (권한 목록)
```

#### SecurityContextHolder
- 현재 요청의 **보안 정보를 저장**하는 전역 저장소
- 스레드마다 독립적으로 관리 (ThreadLocal 사용)

```java
// 아무 곳에서나 현재 사용자 정보 조회 가능
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

#### Authentication
- **인증된 사용자의 정보**를 담는 객체
- 로그인 성공 후 SecurityContext에 저장됨

```java
public interface Authentication {
    String getName();                    // 사용자 이름
    Object getPrincipal();               // 사용자 상세 정보
    Object getCredentials();             // 비밀번호 (보통 null로 지워짐)
    Collection<? extends GrantedAuthority> getAuthorities(); // 권한 목록
    boolean isAuthenticated();           // 인증 여부
}
```

### 3.2 UserDetails와 UserDetailsService

#### UserDetails
- Spring Security가 이해하는 **사용자 정보 형식**

```java
public interface UserDetails {
    String getUsername();                // 사용자 아이디
    String getPassword();                // 비밀번호
    Collection<? extends GrantedAuthority> getAuthorities(); // 권한
    boolean isAccountNonExpired();       // 계정 만료 여부
    boolean isAccountNonLocked();        // 계정 잠금 여부
    boolean isCredentialsNonExpired();   // 비밀번호 만료 여부
    boolean isEnabled();                 // 계정 활성화 여부
}
```

#### UserDetailsService
- **사용자 정보를 조회**하는 인터페이스

```java
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

**동작 예시:**
```
1. 사용자가 로그인 시도: username="hong", password="1234"
2. Spring Security가 UserDetailsService.loadUserByUsername("hong") 호출
3. 데이터베이스에서 hong 사용자 조회
4. UserDetails 객체로 변환해서 반환
5. 입력된 비밀번호와 DB의 비밀번호 비교
6. 일치하면 인증 성공!
```

### 3.3 PasswordEncoder

비밀번호를 **안전하게 암호화**하는 인터페이스

```java
public interface PasswordEncoder {
    String encode(CharSequence rawPassword);      // 비밀번호 암호화
    boolean matches(CharSequence raw, String encoded); // 비밀번호 검증
}
```

**BCryptPasswordEncoder 예시:**
```java
PasswordEncoder encoder = new BCryptPasswordEncoder();

// 암호화
String rawPassword = "1234";
String encoded = encoder.encode(rawPassword);
// 결과: $2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDMVr5yUP1KE

// 검증
encoder.matches("1234", encoded);  // true
encoder.matches("5678", encoded);  // false
```

**특징:**
- 같은 비밀번호여도 매번 다른 암호화 결과 (Salt 사용)
- 암호화는 가능하지만 **복호화는 불가능** (단방향)
- 비밀번호 검증만 가능 (matches 메소드)

---

## 4. Spring Security 동작 원리

### 4.1 Filter Chain (필터 체인)

Spring Security의 핵심은 **필터 체인**입니다.

```
HTTP 요청
    ↓
[Filter 1: SecurityContextPersistenceFilter]  ← SecurityContext 생성/저장
    ↓
[Filter 2: LogoutFilter]                       ← 로그아웃 처리
    ↓
[Filter 3: UsernamePasswordAuthenticationFilter] ← 로그인 처리
    ↓
[Filter 4: JwtAuthenticationFilter]            ← JWT 인증 (우리가 추가)
    ↓
[Filter 5: ExceptionTranslationFilter]         ← 예외 처리
    ↓
[Filter 6: FilterSecurityInterceptor]          ← 권한 검증
    ↓
Controller (우리의 API)
```

#### 필터 체인의 역할

각 필터는 요청을 **검사하고 처리**한 후 다음 필터로 전달합니다:

1. **SecurityContextPersistenceFilter**
   - SecurityContext를 생성하고 요청 처리 후 저장
   
2. **LogoutFilter**
   - 로그아웃 URL인지 확인하고 처리
   
3. **UsernamePasswordAuthenticationFilter**
   - 폼 로그인 처리 (우리는 JWT 사용으로 비활성화)
   
4. **JwtAuthenticationFilter** (우리가 추가)
   - JWT 토큰 검증 및 인증 정보 설정
   
5. **ExceptionTranslationFilter**
   - 인증/인가 실패 시 예외 처리
   
6. **FilterSecurityInterceptor**
   - 최종 권한 검증

### 4.2 인증 처리 흐름

```
1. HTTP 요청 도착
   ↓
2. JwtAuthenticationFilter에서 토큰 추출
   ↓
3. JwtTokenProvider.validateToken(token) 호출
   ↓
4-1. 토큰 유효 ✅
     ↓
     JwtTokenProvider.getAuthentication(token) 호출
     ↓
     UserDetailsService.loadUserByUsername(username) 호출
     ↓
     UserDetails 반환
     ↓
     Authentication 객체 생성
     ↓
     SecurityContextHolder에 저장
     ↓
     다음 필터로 진행
     ↓
     Controller 실행
     
4-2. 토큰 무효 ❌
     ↓
     예외 발생
     ↓
     401 Unauthorized 응답
```

---

## 5. 우리 프로젝트의 구현 분석

### 5.1 전체 구조

```
SecurityConfig.java
    ├── PasswordEncoder (BCrypt 암호화)
    ├── SecurityFilterChain (보안 설정)
    │   ├── CSRF 비활성화
    │   ├── CORS 설정
    │   ├── 세션 정책 (STATELESS)
    │   ├── HTTP Basic 비활성화
    │   ├── Form Login 비활성화
    │   ├── URL 권한 설정
    │   └── JWT 필터 추가
    └── CORS 설정

JwtAuthenticationFilter.java (필터)
    ├── shouldNotFilter() - 필터 제외 경로
    ├── doFilterInternal() - 실제 필터 로직
    └── parseJwt() - 토큰 추출

JwtTokenProvider.java (JWT 유틸리티)
    ├── init() - 시크릿 키 초기화
    ├── createToken() - 토큰 생성
    ├── getAuthentication() - 인증 정보 추출
    ├── getUsername() - 사용자명 추출
    ├── resolveToken() - 헤더에서 토큰 추출
    └── validateToken() - 토큰 검증
```

### 5.2 SecurityConfig.java 상세 분석

#### 파일 위치
`src/main/java/me/minimings/backend/global/config/SecurityConfig.java`

#### 전체 코드
```java
@Configuration              // Spring 설정 클래스
@EnableWebSecurity          // Spring Security 활성화
@RequiredArgsConstructor    // final 필드 생성자 자동 생성
public class SecurityConfig {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user/signup", "/api/user/login").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider), 
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // CORS 설정...
    }
}
```

---

## 6. 각 설정의 의미와 이유

### 6.1 어노테이션 설명

#### @Configuration
```java
@Configuration
public class SecurityConfig { }
```
- **의미**: 이 클래스가 Spring 설정 파일임을 표시
- **역할**: 내부의 @Bean 메소드들을 Spring 컨테이너에 등록
- **없으면**: SecurityConfig가 일반 클래스로 취급되어 보안 설정 적용 안 됨

#### @EnableWebSecurity
```java
@EnableWebSecurity
public class SecurityConfig { }
```
- **의미**: Spring Security를 활성화
- **역할**: 
  - Spring Security 필터 체인 자동 생성
  - 모든 요청에 보안 검사 적용
  - SecurityFilterChain 빈 사용 가능
- **없으면**: Spring Security가 동작하지 않음

#### @RequiredArgsConstructor
```java
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    
    // Lombok이 자동으로 생성:
    // public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
    //     this.jwtTokenProvider = jwtTokenProvider;
    // }
}
```
- **의미**: final 필드의 생성자를 자동 생성 (Lombok)
- **역할**: 의존성 주입(DI)을 간편하게 처리
- **장점**: 코드가 간결해지고 불변성 보장

### 6.2 PasswordEncoder 빈

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**왜 BCrypt를 사용하는가?**

1. **느린 속도 = 보안 강화**
   - 의도적으로 느리게 설계됨
   - 무차별 대입 공격(Brute Force)에 강함
   - 1초에 수십 번 정도만 검증 가능

2. **Salt 자동 생성**
   ```
   같은 비밀번호 "1234"를 암호화해도:
   1차: $2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDMVr5yUP1KE
   2차: $2a$10$N9qo8uLOickgx2ZMRZoMye05IjMx.TUjKvB/J5YOa.SYq9hP7yZXm
   
   → 매번 다른 결과! (Salt가 달라서)
   ```

3. **단방향 암호화**
   - 암호화: 1234 → `$2a$10$vI8a...`
   - 복호화: **불가능!**
   - 검증만 가능: `matches("1234", "$2a$10$vI8a...")` → true

### 6.3 CSRF 비활성화

```java
.csrf(csrf -> csrf.disable())
```

#### CSRF(Cross-Site Request Forgery)란?

```
악의적인 공격 시나리오:

1. 사용자가 은행 사이트(bank.com)에 로그인
   → 브라우저에 세션 쿠키 저장

2. 사용자가 악성 사이트(evil.com) 방문

3. evil.com의 숨겨진 코드:
   <form action="https://bank.com/transfer" method="POST">
       <input name="to" value="hacker-account">
       <input name="amount" value="1000000">
   </form>
   <script>document.forms[0].submit();</script>

4. 브라우저가 자동으로 bank.com에 요청 전송
   → 세션 쿠키도 함께 전송됨!
   
5. bank.com은 정상 로그인으로 인식하여 송금 처리 😱
```

#### 왜 비활성화하는가?

```java
// JWT 사용 시 CSRF 불필요
.csrf(csrf -> csrf.disable())
```

**이유:**
1. **세션 쿠키를 사용하지 않음**
   - CSRF는 브라우저가 자동으로 쿠키를 전송하는 것을 악용
   - JWT는 Authorization 헤더에 직접 넣어야 함
   - 악성 사이트에서는 헤더 조작 불가능 (CORS 정책)

2. **Stateless 방식**
   - 서버에 세션 정보 없음
   - 토큰만으로 인증

**예시:**
```javascript
// 브라우저에서 API 호출 시
fetch('/api/user/profile', {
    headers: {
        'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
        // ↑ 이걸 악성 사이트에서 알 수 없음!
    }
})
```

### 6.4 CORS 설정

```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

#### CORS(Cross-Origin Resource Sharing)란?

```
Origin이란?
- 프로토콜 + 도메인 + 포트
- 예: http://localhost:3000

Same Origin (같은 출처):
✅ http://localhost:3000/page1 → http://localhost:3000/api
   └─ 프로토콜, 도메인, 포트 모두 동일

Cross Origin (다른 출처):
❌ http://localhost:3000 → http://localhost:8080
   └─ 포트가 다름!
```

#### 왜 필요한가?

```
시나리오:
1. 프론트엔드: http://localhost:3000 (React)
2. 백엔드: http://localhost:8080 (Spring Boot)

3. React에서 API 호출:
   fetch('http://localhost:8080/api/user/login', { ... })

4. 브라우저: "다른 출처야! 요청 차단!" ⛔
   → CORS 에러 발생

해결:
백엔드에서 "localhost:3000에서 오는 요청은 허용해줘" 설정 필요
```

#### 우리 프로젝트의 CORS 설정

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // 1. React 서버(3000 포트)에서 오는 요청 허용
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
    
    // 2. 허용할 HTTP 메소드
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    
    // 3. 허용할 헤더 (JWT를 위해 Authorization 필수)
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    
    // 4. 쿠키 전송 허용
    configuration.setAllowCredentials(true);
    
    // 5. 응답 헤더 노출 (프론트에서 읽을 수 있게)
    configuration.setExposedHeaders(Arrays.asList("Authorization"));
    
    // 6. 모든 경로에 적용
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

**각 설정의 의미:**

1. **setAllowedOrigins**
   ```java
   configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
   // → "3000 포트에서 오는 요청만 허용"
   ```

2. **setAllowedMethods**
   ```java
   configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
   // → "이 HTTP 메소드들은 사용 가능"
   // OPTIONS는 브라우저가 CORS 확인용으로 먼저 보내는 요청
   ```

3. **setAllowedHeaders**
   ```java
   configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
   // → "이 헤더들은 요청에 포함 가능"
   // Authorization: JWT 토큰 전송용
   // Content-Type: JSON 데이터 전송용
   ```

4. **setAllowCredentials(true)**
   ```java
   configuration.setAllowCredentials(true);
   // → "쿠키, Authorization 헤더 등 인증 정보 전송 허용"
   ```

5. **setExposedHeaders**
   ```java
   configuration.setExposedHeaders(Arrays.asList("Authorization"));
   // → "응답의 Authorization 헤더를 JavaScript에서 읽을 수 있게 허용"
   // 로그인 후 토큰을 응답 헤더로 받을 때 필요
   ```

### 6.5 세션 정책

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

#### SessionCreationPolicy.STATELESS란?

**전통적인 세션 방식:**
```
1. 사용자 로그인
2. 서버가 세션 생성 → 서버 메모리에 저장
   SessionID: abc123
   User: {id: 1, name: "홍길동", role: "USER"}
3. 클라이언트에게 SessionID만 쿠키로 전달
4. 이후 요청마다 SessionID 전송
5. 서버가 SessionID로 세션 조회
```

**STATELESS 방식 (JWT):**
```
1. 사용자 로그인
2. 서버가 JWT 생성 (모든 정보를 토큰에 포함)
   Token: eyJhbGc...
3. 클라이언트에게 토큰 전달
4. 이후 요청마다 토큰 전송
5. 서버는 토큰만 검증 (세션 조회 안 함!)
```

**왜 STATELESS를 사용하는가?**

1. **서버 확장성(Scalability)**
   ```
   세션 방식:
   Server 1: [세션 A, B, C]
   Server 2: [세션 D, E, F]
   → 사용자 A가 Server 2로 요청하면?
   → Server 2는 세션 A를 몰라서 로그아웃됨! 😱
   → 해결: 세션 공유 서버 필요 (Redis 등)
   
   STATELESS 방식:
   Server 1: [세션 없음]
   Server 2: [세션 없음]
   → 어느 서버로 요청해도 토큰만 검증하면 OK! ✅
   ```

2. **메모리 효율**
   ```
   세션: 사용자 1만 명 → 서버 메모리에 1만 개 세션 저장
   STATELESS: 사용자 1만 명 → 서버 메모리 사용 0
   ```

3. **Microservices 아키텍처 적합**
   ```
   Auth 서버 → JWT 발급
   User 서버 → JWT 검증 (세션 공유 불필요)
   Order 서버 → JWT 검증 (세션 공유 불필요)
   ```

### 6.6 HTTP Basic / Form Login 비활성화

```java
.httpBasic(httpBasic -> httpBasic.disable())
.formLogin(formLogin -> formLogin.disable())
```

#### HTTP Basic Authentication이란?
```
요청 헤더:
Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=
                    └─ "username:password"를 Base64 인코딩

문제점:
❌ 암호화 아님! (Base64는 인코딩일 뿐)
❌ 매 요청마다 비밀번호 전송
❌ 로그아웃 불가능
```

#### Form Login이란?
```html
<!-- Spring Security 기본 로그인 페이지 -->
<form action="/login" method="POST">
    <input name="username">
    <input name="password">
    <button>로그인</button>
</form>
```

**왜 비활성화하는가?**
- JWT 기반 인증 사용
- REST API라서 별도 프론트엔드 존재
- Spring Security 기본 로그인 페이지 불필요

### 6.7 URL 권한 설정

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/user/signup", "/api/user/login").permitAll()
    .anyRequest().authenticated()
)
```

#### 동작 방식

```
요청 URL                    권한 필요 여부
─────────────────────────────────────────
/api/user/signup           ⭕ 인증 불필요 (permitAll)
/api/user/login            ⭕ 인증 불필요 (permitAll)
/api/user/profile          ❌ 인증 필요 (authenticated)
/api/admin/dashboard       ❌ 인증 필요 (authenticated)
```

#### 코드 분석

```java
.requestMatchers("/api/user/signup", "/api/user/login")
// → 이 URL들은

.permitAll()
// → 누구나 접근 가능

.anyRequest()
// → 그 외 모든 요청은

.authenticated()
// → 인증된 사용자만 접근 가능
```

#### 더 복잡한 권한 설정 예시

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/public/**").permitAll()           // 공개 API
    .requestMatchers("/api/user/**").hasRole("USER")         // USER 권한 필요
    .requestMatchers("/api/admin/**").hasRole("ADMIN")       // ADMIN 권한 필요
    .requestMatchers("/api/premium/**").hasAnyRole("PREMIUM", "ADMIN") // 둘 중 하나
    .anyRequest().authenticated()                            // 나머지는 인증만 필요
)
```

### 6.8 JWT 필터 추가

```java
.addFilterBefore(
    new JwtAuthenticationFilter(jwtTokenProvider), 
    UsernamePasswordAuthenticationFilter.class
)
```

#### 의미
> "UsernamePasswordAuthenticationFilter 실행 **전에** JwtAuthenticationFilter를 먼저 실행해라"

#### 필터 순서

```
HTTP 요청
  ↓
[SecurityContextPersistenceFilter]
  ↓
[LogoutFilter]
  ↓
[JwtAuthenticationFilter]  ← 우리가 추가한 필터 (먼저 실행)
  ↓
[UsernamePasswordAuthenticationFilter]  ← Form 로그인 처리 (비활성화됨)
  ↓
[FilterSecurityInterceptor]
  ↓
Controller
```

#### 왜 앞에 배치하는가?

```
시나리오:
1. 요청: GET /api/user/profile
   Header: Authorization: Bearer eyJhbGc...

2. JwtAuthenticationFilter 실행:
   - 토큰 추출 ✅
   - 토큰 검증 ✅
   - Authentication 객체 생성 ✅
   - SecurityContext에 저장 ✅

3. FilterSecurityInterceptor 실행:
   - SecurityContext 확인
   - Authentication 있네? → 인증된 사용자!
   - 통과! ✅

만약 JwtAuthenticationFilter가 뒤에 있다면:
→ FilterSecurityInterceptor가 먼저 실행
→ SecurityContext에 Authentication 없음
→ 인증 실패! ❌
```

---

## 7. 전체 흐름 정리

### 7.1 애플리케이션 시작 시

```
1. Spring Boot 시작
   ↓
2. SecurityConfig 로드
   ↓
3. SecurityFilterChain 빈 생성
   ├─ PasswordEncoder 등록
   ├─ CSRF 비활성화
   ├─ CORS 설정
   ├─ 세션 STATELESS 설정
   ├─ URL 권한 설정
   └─ JwtAuthenticationFilter 추가
   ↓
4. 모든 HTTP 요청에 필터 체인 적용 준비 완료
```

### 7.2 로그인 요청 시

```
1. POST /api/user/login
   Body: {username: "hong", password: "1234"}
   ↓
2. JwtAuthenticationFilter.shouldNotFilter() 확인
   → "/api/user/login"은 제외 경로
   → 필터 건너뛰기
   ↓
3. Controller로 바로 이동
   ↓
4. UserService에서:
   - DB에서 사용자 조회
   - passwordEncoder.matches("1234", dbPassword) 검증
   - 일치하면 JwtTokenProvider.createToken() 호출
   ↓
5. JWT 토큰 생성 후 클라이언트에 반환
   Response: {token: "eyJhbGc..."}
```

### 7.3 인증이 필요한 API 요청 시

```
1. GET /api/user/profile
   Header: Authorization: Bearer eyJhbGc...
   ↓
2. SecurityFilterChain 진입
   ↓
3. JwtAuthenticationFilter.doFilterInternal() 실행:
   ├─ parseJwt() → 토큰 추출
   ├─ jwtTokenProvider.validateToken() → 토큰 검증
   ├─ 유효하면 jwtTokenProvider.getAuthentication() → Authentication 생성
   └─ SecurityContextHolder.getContext().setAuthentication(auth)
   ↓
4. FilterSecurityInterceptor 실행:
   - SecurityContext에서 Authentication 확인
   - 인증됨! ✅
   ↓
5. Controller 실행
   ↓
6. 응답 반환
```

### 7.4 인증 실패 시

```
1. GET /api/user/profile
   Header: Authorization: Bearer invalid_token_12345
   ↓
2. JwtAuthenticationFilter:
   - parseJwt() → 토큰 추출
   - validateToken() → 검증 실패! ❌
   - RuntimeException 발생
   ↓
3. ExceptionTranslationFilter가 예외 처리
   ↓
4. 401 Unauthorized 응답
```

---

## 8. 핵심 요약

### Spring Security를 사용하는 이유
1. ✅ 보안 로직과 비즈니스 로직 분리
2. ✅ 검증된 보안 기능
3. ✅ 표준화된 방식
4. ✅ CSRF, XSS 등 공격 방어

### 핵심 구성 요소
1. **SecurityFilterChain**: 모든 보안 설정의 중심
2. **PasswordEncoder**: 비밀번호 암호화
3. **Authentication**: 인증 정보 저장
4. **SecurityContext**: 현재 사용자 정보 보관

### JWT 방식의 장점
1. ✅ Stateless (서버에 세션 없음)
2. ✅ 확장성 좋음 (여러 서버 가능)
3. ✅ Microservices 적합
4. ✅ 모바일 앱에서도 사용 가능

### 우리 프로젝트의 특징
1. JWT 기반 인증
2. STATELESS 세션 정책
3. CORS 설정으로 React 연동 준비
4. BCrypt로 비밀번호 암호화
5. URL별 권한 관리

---

## 9. 자주 묻는 질문 (FAQ)

### Q1. CSRF를 비활성화해도 안전한가요?
**A:** JWT를 사용하면 안전합니다. CSRF는 쿠키 기반 인증의 취약점이고, JWT는 Authorization 헤더를 사용하므로 CSRF 공격이 불가능합니다.

### Q2. 세션을 아예 사용하지 않나요?
**A:** Spring Security는 내부적으로 SecurityContext를 저장하는 용도로 일시적 세션을 사용할 수 있지만, STATELESS 정책으로 인증 정보는 세션에 저장하지 않습니다.

### Q3. @EnableWebSecurity를 빼면 어떻게 되나요?
**A:** Spring Security가 활성화되지 않아 모든 요청이 인증 없이 통과됩니다.

### Q4. permitAll()과 authenticated()의 차이는?
**A:**
- `permitAll()`: 누구나 접근 가능 (인증 불필요)
- `authenticated()`: 인증된 사용자만 접근 가능

### Q5. 왜 UsernamePasswordAuthenticationFilter보다 앞에 JWT 필터를 두나요?
**A:** JWT 토큰을 먼저 검증해서 SecurityContext에 인증 정보를 저장해야, 이후 필터들이 인증된 사용자로 인식하기 때문입니다.

---

**다음 문서**: [JWT 완벽 가이드](./jwt.md)
