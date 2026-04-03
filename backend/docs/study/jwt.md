# JWT (JSON Web Token) 완벽 가이드

> 이 문서는 JWT가 무엇이며, 어떻게 구성되고, 왜 현재 프로젝트에서 HS256 알고리즘을 사용하고 있는지, 그리고 지금 코드 기준으로 어떻게 구현되었는지를 설명합니다.

## 📚 목차
1. [JWT란 무엇인가?](#1-jwt란-무엇인가)
2. [JWT의 구조](#2-jwt의-구조)
3. [JWT의 동작 원리](#3-jwt의-동작-원리)
4. [JWT 알고리즘 (HS256 vs HS512)](#4-jwt-알고리즘-hs256-vs-hs512)
5. [우리 프로젝트의 JWT 구현](#5-우리-프로젝트의-jwt-구현)
6. [JwtTokenProvider 상세 분석](#6-jwttokenprovider-상세-분석)
7. [JwtAuthenticationFilter 상세 분석](#7-jwtauthenticationfilter-상세-분석)
8. [전체 인증 흐름](#8-전체-인증-흐름)

---

## 1. JWT란 무엇인가?

### 1.1 정의

**JWT (JSON Web Token)**는 **사용자 인증 정보를 안전하게 전송하기 위한 토큰** 기반의 인증 방식입니다.

### 1.2 전통적인 세션 방식 vs JWT 방식

#### 세션 기반 인증 (전통 방식)

```
1. 사용자 로그인
   ↓
2. 서버가 세션 생성 및 저장 (메모리/DB)
   Session ID: abc123
   User Data: {
       id: 1,
       name: "홍길동",
       role: "USER"
   }
   ↓
3. 클라이언트에게 Session ID만 쿠키로 전달
   Set-Cookie: JSESSIONID=abc123
   ↓
4. 이후 요청마다 쿠키로 Session ID 자동 전송
   Cookie: JSESSIONID=abc123
   ↓
5. 서버가 Session ID로 세션 데이터 조회
```

**문제점:**
- 🔴 서버가 모든 세션을 메모리에 저장해야 함 (메모리 부담)
- 🔴 서버가 여러 대일 때 세션 공유 문제
- 🔴 Stateful (서버가 상태를 저장)
- 🔴 확장성(Scalability) 낮음

#### JWT 기반 인증 (현대 방식)

```
1. 사용자 로그인
   ↓
2. 서버가 JWT 생성 (모든 정보를 토큰에 포함)
   Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiJob25nIiwicm9sZXMiOlsiVVNFUiJdfQ.xyz
   ↓
3. 클라이언트에게 토큰 전달
   Response: {token: "eyJhbGc..."}
   ↓
4. 클라이언트가 토큰을 저장 (LocalStorage/SessionStorage)
   ↓
5. 이후 요청마다 Authorization 헤더에 토큰 포함
   Authorization: Bearer eyJhbGc...
   ↓
6. 서버는 토큰 검증만 수행 (DB 조회 불필요!)
```

**장점:**
- ✅ 서버에 세션 저장 안 함 (Stateless)
- ✅ 서버 확장이 자유로움
- ✅ 토큰 자체에 정보 포함
- ✅ Microservices 환경에 적합
- ✅ 모바일 앱에서도 사용 가능

### 1.3 JWT가 해결하는 문제

#### 문제 1: 서버 확장성

```
세션 방식:
[Server 1]        [Server 2]
Session A,B      Session C,D

사용자 A → Server 2 요청
→ Server 2는 Session A를 몰라서 로그인 풀림! 😱

해결책: Redis 같은 세션 공유 저장소 필요 (복잡)

━━━━━━━━━━━━━━━━━━━━━━

JWT 방식:
[Server 1]        [Server 2]
(세션 없음)      (세션 없음)

사용자 A → Server 2 요청
→ 토큰만 검증하면 OK! ✅
```

#### 문제 2: 마이크로서비스 환경

```
세션 방식:
Auth Server → 세션 생성
User Server → 세션 조회 필요 (어떻게? 😱)
Order Server → 세션 조회 필요 (어떻게? 😱)

━━━━━━━━━━━━━━━━━━━━━━

JWT 방식:
Auth Server → JWT 발급
User Server → JWT 검증 (독립적으로 가능!)
Order Server → JWT 검증 (독립적으로 가능!)
```

---

## 2. JWT의 구조

### 2.1 3가지 부분으로 구성

JWT는 `.`(점)으로 구분된 **3개의 부분**으로 구성됩니다:

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiJob25nIiwicm9sZXMiOlsiVVNFUiJdLCJleHAiOjE3MDAwMDAwMDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

└───────── Header ─────────┘ └──────────── Payload ──────────┘ └────────── Signature ──────────┘
```

### 2.2 Header (헤더)

**역할**: 토큰의 **타입과 암호화 알고리즘** 정보

```json
{
  "alg": "HS256",    // 알고리즘 (HMAC SHA-256)
  "typ": "JWT"       // 토큰 타입
}
```

**Base64 인코딩:**
```
{"alg":"HS256","typ":"JWT"}
↓ Base64 인코딩
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
```

> **주의**: Base64는 암호화가 아니라 **인코딩**입니다! 누구나 디코딩 가능합니다.

### 2.3 Payload (페이로드)

**역할**: 실제 **전달할 데이터** (사용자 정보, 권한 등)

```json
{
  "sub": "hong",              // Subject: 사용자 ID
  "roles": ["ROLE_USER"],     // 권한
  "iat": 1700000000,          // Issued At: 발급 시간
  "exp": 1700086400           // Expiration: 만료 시간
}
```

**Base64 인코딩:**
```
{"sub":"hong","roles":["ROLE_USER"],"iat":1700000000,"exp":1700086400}
↓ Base64 인코딩
eyJ1c2VySWQiOiJob25nIiwicm9sZXMiOlsiVVNFUiJdLCJleHAiOjE3MDAwMDAwMDB9
```

#### 표준 클레임 (Standard Claims)

JWT 표준에서 정의한 필드들:

| 클레임 | 의미 | 예시 |
|--------|------|------|
| `sub` | Subject (주체) | 사용자 ID |
| `iss` | Issuer (발급자) | "my-app" |
| `aud` | Audience (수신자) | "mobile-app" |
| `exp` | Expiration Time (만료 시간) | 1700086400 (Unix timestamp) |
| `iat` | Issued At (발급 시간) | 1700000000 |
| `nbf` | Not Before (활성화 시간) | 1700000000 |

#### 커스텀 클레임 (Custom Claims)

우리가 필요한 대로 추가 가능:

```json
{
  "userId": "hong",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "email": "hong@example.com",
  "premium": true
}
```

> **중요**: Payload는 누구나 디코딩 가능하므로 **민감한 정보(비밀번호 등)는 절대 넣으면 안 됩니다!**

### 2.4 Signature (서명)

**역할**: 토큰이 **변조되지 않았음을 검증**

#### 생성 방법

```
Signature = HMAC-SHA256(
    base64(Header) + "." + base64(Payload),
    secret_key
)
```

**예시:**
```javascript
// 1. Header와 Payload를 합침
const data = "eyJhbGc...헤더" + "." + "eyJ1c2...페이로드";

// 2. Secret Key로 HMAC-SHA256 해싱
const signature = HMAC_SHA256(data, "my-secret-key-1234");

// 3. Base64 인코딩
const encodedSignature = base64(signature);
// 결과: SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

#### 검증 과정

```
1. 클라이언트가 토큰 전송:
   eyJhbGc...헤더.eyJ1c2...페이로드.SflKx...서명

2. 서버가 검증:
   - Header와 Payload를 추출
   - Secret Key로 서명 재생성
   - 원본 서명과 비교
   
3. 서명 일치 ✅ → 토큰 유효 (변조 안 됨)
   서명 불일치 ❌ → 토큰 무효 (변조됨)
```

**변조 시도 예시:**
```
공격자가 Payload 수정:
{"sub":"hong","roles":["ROLE_ADMIN"]}  ← USER에서 ADMIN으로 변경

Base64 인코딩:
eyJ...새로운Payload

JWT 재조합:
eyJhbGc...헤더.eyJ...새로운Payload.SflKx...기존서명

검증:
서버가 새Payload로 서명 재생성 → 다른 서명!
→ 기존 서명과 불일치 ❌
→ 토큰 거부!
```

---

## 3. JWT의 동작 원리

### 3.1 전체 흐름

```
┌─────────────┐                              ┌─────────────┐
│   Client    │                              │   Server    │
│  (React)    │                              │(Spring Boot)│
└─────────────┘                              └─────────────┘
       │                                              │
       │  1. POST /api/user/login                    │
       │     {username:"hong", password:"1234"}      │
       ├────────────────────────────────────────────>│
       │                                              │
       │                                     2. 비밀번호 검증
       │                                        BCrypt.matches()
       │                                              │
       │                                     3. JWT 생성
       │                                        createToken()
       │                                              │
       │  4. Response                                 │
       │     {token: "eyJhbGc..."}                   │
       │<────────────────────────────────────────────┤
       │                                              │
  5. 토큰 저장                                        │
  localStorage.setItem()                             │
       │                                              │
       │  6. GET /api/user/profile                   │
       │     Header: Authorization: Bearer eyJhbGc...│
       ├────────────────────────────────────────────>│
       │                                              │
       │                                     7. 토큰 추출
       │                                        parseJwt()
       │                                              │
       │                                     8. 토큰 검증
       │                                        validateToken()
       │                                              │
       │                                     9. 인증 정보 생성
       │                                        getAuthentication()
       │                                              │
       │  10. Response                                │
       │      {name:"홍길동", email:"hong@..."}      │
       │<────────────────────────────────────────────┤
       │                                              │
```

### 3.2 로그인 과정 (토큰 발급)

```
1. 사용자가 username, password 입력
   ↓
2. POST /api/user/login 요청
   ↓
3. 서버가 DB에서 사용자 조회
   ↓
4. BCrypt로 비밀번호 검증
   passwordEncoder.matches(inputPassword, dbPassword)
   ↓
5. 검증 성공 시 JWT 생성
   JwtTokenProvider.createToken(userId, roles)
   ├─ Header 생성: {"alg":"HS256","typ":"JWT"}
   ├─ Payload 생성: {"sub":"hong","roles":["USER"],"exp":...}
   └─ Signature 생성: HMAC_SHA256(header.payload, secretKey)
   ↓
6. 3개 부분을 합쳐서 JWT 완성
   eyJhbGc...헤더.eyJ1c2...페이로드.SflKx...서명
   ↓
7. 클라이언트에게 토큰 반환
   {token: "eyJhbGc..."}
```

### 3.3 인증 과정 (토큰 검증)

```
1. 클라이언트가 API 요청
   GET /api/user/profile
   Header: Authorization: Bearer eyJhbGc...
   ↓
2. JwtAuthenticationFilter에서 토큰 추출
   String token = parseJwt(request);
   ↓
3. 토큰 검증
   jwtTokenProvider.validateToken(token)
   ├─ 토큰 파싱
   ├─ 서명 검증 (Secret Key로 재생성 후 비교)
   └─ 만료 시간 확인 (exp < now?)
   ↓
4-1. 검증 성공 ✅
     ↓
     사용자 정보 추출
     jwtTokenProvider.getAuthentication(token)
     ├─ Payload에서 username 추출
     ├─ UserDetailsService.loadUserByUsername(username)
     └─ Authentication 객체 생성
     ↓
     SecurityContext에 저장
     SecurityContextHolder.getContext().setAuthentication(auth)
     ↓
     다음 필터로 진행 (Controller 실행)
     
4-2. 검증 실패 ❌
     ↓
     401 Unauthorized 응답
```

---

## 4. JWT 알고리즘 (HS256 vs HS512)

### 4.1 대칭 키 알고리즘 (HMAC)

**HMAC (Hash-based Message Authentication Code)**
- **대칭 키 방식**: 서명 생성과 검증에 **같은 Secret Key** 사용
- **서버만 Secret Key 보관**

```
서명 생성:
Signature = HMAC_SHA256(data, secretKey)

서명 검증:
NewSignature = HMAC_SHA256(data, secretKey)
NewSignature == Signature? ✅ 유효 : ❌ 무효
```

#### HS256 (HMAC SHA-256)

```
알고리즘: HMAC + SHA-256
해시 출력: 256 bits (32 bytes)
권장 키 길이: 최소 256 bits (32 bytes)
```

**장점:**
- ✅ 빠른 성능
- ✅ 키 길이가 짧아도 됨
- ✅ 대부분의 경우 충분히 안전

**단점:**
- ⚠️ HS512보다 상대적으로 보안 강도 낮음

#### HS512 (HMAC SHA-512)

```
알고리즘: HMAC + SHA-512
해시 출력: 512 bits (64 bytes)
권장 키 길이: 최소 512 bits (64 bytes)
```

**장점:**
- ✅ **더 강력한 보안** (해시 충돌 확률 극히 낮음)
- ✅ 미래 대비 (양자 컴퓨터 등)
- ✅ 금융 등 고보안 환경 적합

**단점:**
- ⚠️ HS256보다 약간 느림 (실무에서는 거의 무시 가능)
- ⚠️ 더 긴 키 필요

### 4.2 왜 우리 프로젝트는 HS256을 사용하나?

코드를 보면:
```java
String token = Jwts.builder()
    .setClaims(claims)
    .setIssuedAt(now)
    .setExpiration(new Date(now.getTime() + tokenValidMillisecond))
    .signWith(key, SignatureAlgorithm.HS256)  // ← HS256 사용!
    .compact();
```

**HS256을 선택한 이유:**

1. **충분한 보안 강도**
   ```
   HS256의 보안 강도:
   - 2^256 = 약 10^77 가지 경우의 수
   - 현재 컴퓨터로 무차별 대입 공격 사실상 불가능
   - 일반 웹 애플리케이션에는 충분
   ```

2. **성능**
   ```
   HS256: ~10,000 tokens/sec
   HS512: ~9,000 tokens/sec
   → 차이는 미미하지만 HS256이 약간 더 빠름
   ```

3. **키 관리 편의성**
   ```
   HS256: 32 bytes 이상
   HS512: 64 bytes 이상
   → HS256이 더 짧은 키로도 가능
   ```

4. **호환성**
   ```
   HS256: 거의 모든 JWT 라이브러리 지원
   HS512: 대부분 지원하지만 일부 구형 라이브러리에서 문제
   ```

### 4.3 언제 HS512를 사용해야 하나?

다음 경우에는 HS512 권장:

1. **금융/의료 등 고보안 환경**
   ```
   은행, 병원, 정부 기관 등
   → 더 강력한 보안 필요
   ```

2. **장기간 유효한 토큰**
   ```
   Refresh Token (30일 이상)
   → 길게 보관되므로 더 안전하게
   ```

3. **규정 준수**
   ```
   일부 보안 표준(FIPS 등)에서 SHA-512 요구
   ```

### 4.4 비대칭 키 알고리즘 (RS256)

참고: **RS256 (RSA SHA-256)**도 많이 사용됩니다.

```
대칭 키 (HS256):
- 서명 생성: Private Key
- 서명 검증: Private Key (같은 키)
- 문제: 모든 서버가 Secret Key를 알아야 함

비대칭 키 (RS256):
- 서명 생성: Private Key (비밀)
- 서명 검증: Public Key (공개)
- 장점: Public Key만 배포하면 됨
```

**언제 RS256?**
```
HS256 적합:
- 단일 백엔드 서버
- 마이크로서비스 (모두 같은 팀 관리)

RS256 적합:
- 여러 외부 서비스가 검증해야 할 때
- Public API (누구나 검증 가능하게)
- OAuth 2.0, OpenID Connect
```

**우리 프로젝트는 HS256으로 충분:**
- 백엔드 서버만 Secret Key 보관
- 외부 서비스 연동 없음

---

## 5. 우리 프로젝트의 JWT 구현

> 현재 프로젝트에 실제로 적용되는 인증 흐름은 `../architecture/auth-flow.md` 문서에서 별도로 정리합니다. 이 문서는 JWT 개념과 현재 코드에 들어간 구성 요소를 이해하는 데 초점을 둡니다.

### 5.1 전체 구조

```
JwtTokenProvider (유틸리티)
    ├── @Value("${jwt.secret}") secretKey
    ├── Key key (HMAC 키 객체)
    ├── tokenValidMillisecond (유효 기간)
    │
    ├── init() - 시크릿 키 초기화
    ├── createToken() - JWT 생성
    ├── getAuthentication() - 인증 정보 추출
    ├── getUsername() - 사용자명 추출
    ├── resolveToken() - 헤더에서 토큰 추출
    └── validateToken() - 토큰 검증

JwtAuthenticationFilter (필터)
    ├── shouldNotFilter() - 필터 제외할 경로 정의
    ├── doFilterInternal() - 실제 필터 로직
    └── parseJwt() - Authorization 헤더에서 토큰 추출

application.yml (설정)
    ├── jwt.header: Authorization
    ├── jwt.secret: Base64 인코딩된 시크릿 키
    └── jwt.token-validity-in-seconds: 86400 (24시간)
```

### 5.2 application.yml 설정

```yaml
jwt:
  header: Authorization
  # 현재 코드는 HS256을 사용하며, 시크릿은 Base64 인코딩된 문자열입니다.
  secret: c2lsdmVybmluZS10ZWNoLXNwcmluZy1ib290LWp3dC10dXRvcmlhbC1zZWNyZXQtc2lsdmVybmluZS10ZWNoLXNwcmluZy1ib290LWp3dC10dXRvcmlhbC1zZWNyZXQK
  # 토큰 유효 기간 (초)
  token-validity-in-seconds: 86400  # 24시간
```

#### Secret Key 생성 방법

```bash
# Linux/Mac
echo -n "my-super-secret-key-that-is-very-long" | base64

# 결과:
bXktc3VwZXItc2VjcmV0LWtleS10aGF0LWlzLXZlcnktbG9uZw==
```

**주의사항:**
- 🔴 Secret Key는 **절대 GitHub에 커밋하면 안 됨!**
- ✅ `application.yml`을 `.gitignore`에 추가
- ✅ 환경 변수나 별도 설정 파일로 관리

---

## 6. JwtTokenProvider 상세 분석

### 6.1 전체 코드

**파일 위치:** `src/main/java/me/minimings/backend/global/util/JwtTokenProvider.java`

```java
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    private final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);
    private final UserDetailsService userDetailsService;
    
    @Value("${jwt.secret}")
    private String secretKey = "secretKey";
    
    private Key key;
    private final long tokenValidMillisecond = 1000L * 60 * 60; // 1시간
    
    @PostConstruct
    protected void init() {
        LOGGER.info("[init] JwtTokenProvider: Start init secretKey");
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        LOGGER.info("[init] JwtTokenProvider: Finish init secretKey");
    }
    
    public String createToken(String userUid, List<String> roles) {
        LOGGER.info("[createToken] Start token generation");
        Claims claims = Jwts.claims().setSubject(userUid);
        claims.put("roles", roles);
        Date now = new Date();
        
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + tokenValidMillisecond))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        
        LOGGER.info("[createToken] Finish token generation");
        return token;
    }
    
    public Authentication getAuthentication(String token) {
        LOGGER.info("[getAuthentication] Start Token Authentication");
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUsername(token));
        LOGGER.info("[getAuthentication] Finish, UserDetails UserName: {}",
                userDetails.getUsername());
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
    
    public String getUsername(String token) {
        LOGGER.info("[getUsername] Start extraction");
        String info = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        LOGGER.info("[getUsername] Finish, info: {}", info);
        return info;
    }
    
    public String resolveToken(HttpServletRequest request) {
        LOGGER.info("[resolveToken] Start Token Extraction from HTTP header");
        return request.getHeader("X-AUTH-TOKEN");
    }
    
    public boolean validateToken(String token) {
        LOGGER.info("[validateToken] Start Token Validation");
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            LOGGER.info("[validateToken] Token Validation Exception");
            return false;
        }
    }
}
```

### 6.2 각 메소드 상세 분석

#### 6.2.1 init() - 시크릿 키 초기화

```java
@PostConstruct
protected void init() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    this.key = Keys.hmacShaKeyFor(keyBytes);
}
```

**@PostConstruct란?**
- Spring이 빈을 생성한 **직후** 자동으로 실행되는 메소드
- 초기화 작업에 사용

**동작:**
```
1. application.yml에서 Secret Key 읽어옴 (Base64 인코딩된 상태)
   secretKey = "c2lsdmVybmluZS10ZWNo..."
   ↓
2. Base64 디코딩
   byte[] keyBytes = Decoders.BASE64.decode(secretKey)
   → 원본 바이트 배열로 변환
   ↓
3. HMAC Key 객체 생성
   this.key = Keys.hmacShaKeyFor(keyBytes)
   → JWT 서명/검증에 사용할 키 객체
```

**왜 이렇게 하나?**
- Secret Key는 바이트 배열이어야 하지만, 설정 파일은 문자열만 가능
- Base64로 인코딩해서 저장 → 사용 시 디코딩

#### 6.2.2 createToken() - JWT 생성

```java
public String createToken(String userUid, List<String> roles) {
    // 1. Claims 생성 (Payload)
    Claims claims = Jwts.claims().setSubject(userUid);
    claims.put("roles", roles);
    
    // 2. 현재 시간
    Date now = new Date();
    
    // 3. JWT 생성
    String token = Jwts.builder()
            .setClaims(claims)                    // Payload 설정
            .setIssuedAt(now)                     // 발급 시간
            .setExpiration(new Date(now.getTime() + tokenValidMillisecond)) // 만료 시간
            .signWith(key, SignatureAlgorithm.HS256)  // 서명
            .compact();                           // 문자열로 변환
    
    return token;
}
```

**단계별 설명:**

1. **Claims 생성** (Payload 데이터)
   ```java
   Claims claims = Jwts.claims().setSubject(userUid);
   claims.put("roles", roles);
   
   // 결과 JSON:
   {
       "sub": "hong",              // Subject: 사용자 ID
       "roles": ["ROLE_USER"]      // 커스텀 클레임
   }
   ```

2. **발급 시간과 만료 시간 설정**
   ```java
   Date now = new Date();  // 2024-01-15 10:00:00
   
   setIssuedAt(now);
   // iat: 1705280400 (Unix timestamp)
   
   setExpiration(new Date(now.getTime() + 3600000));
   // exp: 1705284000 (1시간 후)
   ```

3. **서명 생성**
   ```java
   .signWith(key, SignatureAlgorithm.HS256)
   
   // 과정:
   // Header: {"alg":"HS256","typ":"JWT"}
   // Payload: {"sub":"hong","roles":["ROLE_USER"],"iat":...,"exp":...}
   // Signature: HMAC_SHA256(header.payload, secretKey)
   ```

4. **최종 토큰 조립**
   ```java
   .compact();
   
   // 결과:
   // eyJhbGc...헤더.eyJzdWI...페이로드.1J_8G2O...서명
   ```

**실제 생성되는 JWT 예시:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJob25nIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTcwNTI4MDQwMCwiZXhwIjoxNzA1Mjg0MDAwfQ.8G2OH1J_yMKq4zP5k6fwpMeJf36POk6yJV_adQssw5c
```

디코딩하면:
```json
// Header
{"alg":"HS256","typ":"JWT"}

// Payload
{
  "sub": "hong",
  "roles": ["ROLE_USER"],
  "iat": 1705280400,
  "exp": 1705284000
}

// Signature (검증용)
8G2OH1J_yMKq4zP5k6fwpMeJf36POk6yJV_adQssw5c
```

#### 6.2.3 validateToken() - 토큰 검증

```java
public boolean validateToken(String token) {
    try {
        Jws<Claims> claims = Jwts.parserBuilder()
                .setSigningKey(key)          // Secret Key로 검증
                .build()
                .parseClaimsJws(token);      // 토큰 파싱 + 서명 검증
        
        // 만료 시간 확인
        return !claims.getBody().getExpiration().before(new Date());
    } catch (Exception e) {
        return false;
    }
}
```

**검증 과정:**

1. **서명 검증**
   ```java
   Jwts.parserBuilder()
       .setSigningKey(key)
       .build()
       .parseClaimsJws(token);
   
   // 내부 동작:
   // 1. 토큰을 Header, Payload, Signature로 분리
   // 2. Header + Payload로 서명 재생성
   //    NewSignature = HMAC_SHA256(header.payload, secretKey)
   // 3. 원본 Signature와 비교
   //    NewSignature == Signature? 
   //    → ✅ 유효 (변조 안 됨)
   //    → ❌ 무효 (변조됨) → Exception 발생
   ```

2. **만료 시간 확인**
   ```java
   claims.getBody().getExpiration().before(new Date())
   
   // 예시:
   // exp: 1705284000 (2024-01-15 11:00:00)
   // now: 1705283000 (2024-01-15 10:50:00)
   // → exp > now → 아직 유효 ✅
   
   // exp: 1705284000 (2024-01-15 11:00:00)
   // now: 1705285000 (2024-01-15 11:10:00)
   // → exp < now → 만료됨 ❌
   ```

**예외 처리:**
```java
catch (Exception e) {
    // 다음 경우 false 반환:
    // 1. SignatureException: 서명 불일치 (변조됨)
    // 2. MalformedJwtException: 형식 오류
    // 3. ExpiredJwtException: 만료됨
    // 4. UnsupportedJwtException: 지원 안 하는 형식
    return false;
}
```

#### 6.2.4 getUsername() - 사용자명 추출

```java
public String getUsername(String token) {
    String info = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()         // Payload 추출
            .getSubject();     // "sub" 클레임 값
    return info;
}
```

**동작:**
```
JWT: eyJ...헤더.eyJzdWI6aG9uZywiLi4ufQ...서명

파싱:
{
  "sub": "hong",        ← getSubject()가 이 값 반환
  "roles": ["ROLE_USER"],
  "iat": 1705280400,
  "exp": 1705284000
}

결과: "hong"
```

#### 6.2.5 getAuthentication() - 인증 정보 생성

```java
public Authentication getAuthentication(String token) {
    // 1. 토큰에서 username 추출
    UserDetails userDetails = userDetailsService.loadUserByUsername(
        this.getUsername(token)
    );
    
    // 2. Authentication 객체 생성
    return new UsernamePasswordAuthenticationToken(
        userDetails,              // Principal: 사용자 정보
        "",                       // Credentials: 비밀번호 (빈 문자열)
        userDetails.getAuthorities()  // Authorities: 권한 목록
    );
}
```

**단계별 흐름:**
```
1. Token에서 username 추출
   getUsername("eyJ...") → "hong"
   ↓
2. DB에서 사용자 정보 조회
   userDetailsService.loadUserByUsername("hong")
   → UserDetails {
       username: "hong",
       password: "$2a$10...",
       authorities: [ROLE_USER]
   }
   ↓
3. Authentication 객체 생성
   new UsernamePasswordAuthenticationToken(...)
   → Authentication {
       principal: UserDetails,
       credentials: "",
       authorities: [ROLE_USER],
       authenticated: true
   }
   ↓
4. SecurityContext에 저장 (Filter에서 수행)
   SecurityContextHolder.getContext().setAuthentication(auth)
```

**왜 DB 조회를 하나?**
```
토큰에는 최소 정보만 저장:
{
  "sub": "hong",
  "roles": ["ROLE_USER"]
}

하지만 애플리케이션에서는 더 많은 정보 필요:
- 이메일
- 이름
- 프로필 이미지
- 계정 상태 (활성화/비활성화)

→ DB에서 최신 정보를 가져옴
→ 계정이 비활성화되었다면 여기서 차단 가능!
```

---

## 7. JwtAuthenticationFilter 상세 분석

### 7.1 전체 코드

**파일 위치:** `src/main/java/me/minimings/backend/global/filter/JwtAuthenticationFilter.java`

```java
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    // 필터를 제외할 경로 설정
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String[] excludePath = {"/api/user/signup", "/api/user/login"};
        String path = request.getRequestURI();
        return Arrays.asList(excludePath).contains(path);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtAuthenticationFilter: jwt authentication processing");
        
        // 1. 토큰 추출
        String jwtToken = parseJwt(request);
        log.info("JWT token present: {}", jwtToken != null);
        
        // 2. 토큰이 있으면 검증
        if (jwtToken != null) {
            if (jwtTokenProvider.validateToken(jwtToken)) {
                // 3. 유효하면 Authentication 생성
                Authentication auth = jwtTokenProvider.getAuthentication(jwtToken);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("JWT token validation success");
            } else {
                log.error("Invalid or expired JWT token");
                throw new RuntimeException("Invalid or expired JWT token");
            }
        } else {
            log.error("JWT token is missing");
            throw new RuntimeException("JWT token is missing");
        }
        
        // 4. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
    
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);  // "Bearer " 제거
        }
        return null;
    }
}
```

### 7.2 각 메소드 상세 분석

#### 7.2.1 OncePerRequestFilter

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
```

**OncePerRequestFilter란?**
- Spring이 제공하는 필터 추상 클래스
- **요청당 한 번만** 실행 보장
- 중복 실행 방지

**왜 필요한가?**
```
일반 Filter:
요청 → Filter 실행
→ Forward/Include 발생
→ Filter 또  실행! (중복)

OncePerRequestFilter:
요청 → Filter 실행 (1회만)
→ Forward/Include 발생
→ Filter 건너뛰기 (중복 방지)
```

#### 7.2.2 shouldNotFilter() - 필터 제외 경로

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    String[] excludePath = {"/api/user/signup", "/api/user/login"};
    String path = request.getRequestURI();
    return Arrays.asList(excludePath).contains(path);
}
```

**동작:**
```
요청 경로가:

/api/user/signup  → shouldNotFilter() = true  → 필터 건너뛰기 ⭕
/api/user/login   → shouldNotFilter() = true  → 필터 건너뛰기 ⭕
/api/user/profile → shouldNotFilter() = false → 필터 실행 ✅
```

**왜 필요한가?**
```
회원가입/로그인:
- 아직 JWT가 없는 상태
- 인증 불필요
→ 필터를 건너뛰어야 함

일반 API:
- JWT 필요
- 인증 검증 필요
→ 필터 실행
```

#### 7.2.3 parseJwt() - 토큰 추출

```java
private String parseJwt(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");
    if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
        return headerAuth.substring(7);  // "Bearer " 제거
    }
    return null;
}
```

**동작:**
```
요청 헤더:
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI...

1. Authorization 헤더 추출:
   headerAuth = "Bearer eyJhbGc..."

2. "Bearer " 접두사 확인 및 제거:
   headerAuth.startsWith("Bearer ") → true
   headerAuth.substring(7) → "eyJhbGc..." (토큰만)

결과: "eyJhbGc..."
```

**Bearer 토큰이란?**
```
OAuth 2.0 표준:
- Authorization: Bearer <token>
- "Bearer"는 "bearer"(소지자)를 의미
- "이 토큰을 가진 사람에게 권한 부여"

다른 방식:
- Authorization: Basic <base64>  (HTTP Basic)
- Authorization: Digest <digest>  (HTTP Digest)
```

#### 7.2.4 doFilterInternal() - 핵심 필터 로직

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    // 1. 토큰 추출
    String jwtToken = parseJwt(request);
    
    // 2. 토큰 검증
    if (jwtToken != null) {
        if (jwtTokenProvider.validateToken(jwtToken)) {
            // 3. Authentication 생성 및 저장
            Authentication auth = jwtTokenProvider.getAuthentication(jwtToken);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            throw new RuntimeException("Invalid or expired JWT token");
        }
    } else {
        throw new RuntimeException("JWT token is missing");
    }
    
    // 4. 다음 필터로
    filterChain.doFilter(request, response);
}
```

**전체 흐름:**
```
1. 토큰 추출
   parseJwt(request) → "eyJhbGc..."
   ↓
2. 토큰 존재 확인
   jwtToken != null? 
   ├─ null → Exception (401)
   └─ 존재 ✅
      ↓
3. 토큰 검증
   validateToken(jwtToken)
   ├─ 실패 → Exception (401)
   └─ 성공 ✅
      ↓
4. 인증 정보 생성
   getAuthentication(jwtToken)
   → Authentication 객체
   ↓
5. SecurityContext에 저장
   SecurityContextHolder.getContext().setAuthentication(auth)
   ↓
6. 다음 필터로 진행
   filterChain.doFilter(request, response)
   ↓
7. Controller 실행
```

---

## 8. 전체 인증 흐름

### 8.1 회원가입 → 로그인 → API 호출 시나리오

#### Step 1: 회원가입

```
POST /api/user/signup
Body: {
    "userId": "hong",
    "password": "1234",
    "name": "홍길동",
    "email": "hong@example.com"
}

1. JwtAuthenticationFilter.shouldNotFilter()
   → "/api/user/signup"은 제외 경로
   → 필터 건너뛰기 ⭕

2. Controller 실행
   ↓
3. UserService:
   - passwordEncoder.encode("1234") → "$2a$10..."
   - DB에 저장
   ↓
Response: {
    "message": "회원가입 성공"
}
```

#### Step 2: 로그인

```
POST /api/user/login
Body: {
    "userId": "hong",
    "password": "1234"
}

1. JwtAuthenticationFilter.shouldNotFilter()
   → "/api/user/login"은 제외 경로
   → 필터 건너뛰기 ⭕

2. Controller 실행
   ↓
3. UserService:
   - DB에서 hong 조회
   - passwordEncoder.matches("1234", "$2a$10...") → true ✅
   ↓
4. JwtTokenProvider.createToken("hong", ["ROLE_USER"])
   ↓
   Header: {"alg":"HS256","typ":"JWT"}
   Payload: {
       "sub": "hong",
       "roles": ["ROLE_USER"],
       "iat": 1705280400,
       "exp": 1705284000
   }
   Signature: HMAC_SHA256(header.payload, secretKey)
   ↓
   Token: eyJhbGc...헤더.eyJzdWI...페이로드.8G2OH...서명
   ↓
Response: {
    "token": "eyJhbGc..."
}

5. 클라이언트가 토큰 저장
   localStorage.setItem("token", "eyJhbGc...")
```

#### Step 3: 인증이 필요한 API 호출

```
GET /api/user/profile
Header: Authorization: Bearer eyJhbGc...

1. SecurityFilterChain 진입
   ↓
2. JwtAuthenticationFilter.doFilterInternal()
   ├─ parseJwt(request)
   │  → "eyJhbGc..." 추출
   ├─ validateToken("eyJhbGc...")
   │  ├─ 서명 검증 ✅
   │  ├─ 만료 시간 확인 ✅
   │  └─ 유효!
   ├─ getAuthentication("eyJhbGc...")
   │  ├─ getUsername("eyJhbGc...") → "hong"
   │  ├─ loadUserByUsername("hong") → UserDetails
   │  └─ new UsernamePasswordAuthenticationToken(...)
   └─ SecurityContextHolder.setAuthentication(auth)
   ↓
3. FilterSecurityInterceptor
   - SecurityContext에서 Authentication 확인
   - 인증됨! ✅
   ↓
4. Controller 실행
   @GetMapping("/api/user/profile")
   public UserProfile getProfile() {
       // SecurityContext에서 현재 사용자 정보 가져오기
       Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       String username = auth.getName();  // "hong"
       
       return userService.getProfile(username);
   }
   ↓
Response: {
    "userId": "hong",
    "name": "홍길동",
    "email": "hong@example.com"
}
```

### 8.2 토큰 만료 시

```
GET /api/user/profile
Header: Authorization: Bearer eyJhbGc...(만료된 토큰)

1. JwtAuthenticationFilter
   ├─ parseJwt() → "eyJhbGc..."
   ├─ validateToken("eyJhbGc...")
   │  ├─ 서명 검증 ✅
   │  ├─ 만료 시간 확인
   │  │  exp: 1705284000 (2024-01-15 11:00)
   │  │  now: 1705290000 (2024-01-15 12:40)
   │  │  → exp < now ❌ 만료됨!
   │  └─ false 반환
   └─ RuntimeException("Invalid or expired JWT token")
   ↓
2. ExceptionTranslationFilter가 예외 처리
   ↓
Response: 401 Unauthorized
{
    "error": "Unauthorized",
    "message": "Invalid or expired JWT token"
}

3. 클라이언트:
   - 401 응답 받음
   - 로그인 페이지로 리다이렉트
   - 재로그인 요청
```

### 8.3 변조된 토큰 시도

```
GET /api/user/profile
Header: Authorization: Bearer eyJhbGc...변조된Payload.원본서명

공격자가 시도:
- Payload를 "ROLE_ADMIN"으로 변경
- Base64로 다시 인코딩
- 하지만 서명은 그대로 사용

1. JwtAuthenticationFilter
   ├─ parseJwt() → "eyJhbGc...변조된토큰"
   ├─ validateToken("eyJhbGc...")
   │  ├─ 토큰 파싱
   │  │  Header: {"alg":"HS256","typ":"JWT"}
   │  │  Payload (변조됨): {"sub":"hong","roles":["ROLE_ADMIN"]}
   │  ├─ 서명 재생성
   │  │  NewSig = HMAC_SHA256(header.변조된Payload, secretKey)
   │  ├─ 원본 서명과 비교
   │  │  NewSig != OriginalSig ❌
   │  └─ SignatureException 발생!
   └─ RuntimeException("Invalid or expired JWT token")
   ↓
Response: 401 Unauthorized

→ 변조 시도 차단! 🛡️
```

---

## 9. 핵심 요약

### JWT의 3가지 구성 요소
1. **Header**: 알고리즘 정보 (HS256)
2. **Payload**: 사용자 데이터 (sub, roles, exp 등)
3. **Signature**: 변조 방지 (HMAC-SHA256)

### HS256을 사용하는 이유
1. ✅ 충분한 보안 강도 (2^256)
2. ✅ 빠른 성능
3. ✅ 간단한 키 관리
4. ✅ 널리 지원됨

### JWT의 장점
1. ✅ Stateless (서버 메모리 절약)
2. ✅ 확장성 (여러 서버 가능)
3. ✅ 자체 포함 (모든 정보 담김)
4. ✅ 크로스 도메인 가능

### 보안 주의사항
1. 🔒 Secret Key는 절대 노출 금지
2. 🔒 Payload에 민감 정보 넣지 말 것
3. 🔒 HTTPS 사용 (토큰 탈취 방지)
4. 🔒 짧은 만료 시간 설정
5. 🔒 Refresh Token 고려

---

## 10. 자주 묻는 질문 (FAQ)

### Q1. JWT는 암호화되나요?
**A:** 아니요! JWT는 **인코딩**(Base64)만 됩니다. 누구나 디코딩 가능합니다. 하지만 **서명(Signature)**으로 변조를 방지합니다.

### Q2. Payload에 비밀번호를 넣어도 되나요?
**A:** 절대 안 됩니다! Payload는 누구나 볼 수 있으므로, 민감한 정보는 넣으면 안 됩니다.

### Q3. 토큰이 탈취되면 어떻게 하나요?
**A:** 
- 짧은 만료 시간 설정 (1시간 등)
- Refresh Token 사용
- HTTPS로 통신
- XSS 방지 (LocalStorage보다 HttpOnly Cookie 고려)

### Q4. HS256과 RS256 중 뭘 써야 하나요?
**A:**
- **HS256**: 단일 백엔드, 빠른 성능 필요 시
- **RS256**: 여러 외부 서비스가 검증해야 할 때

### Q5. 토큰 만료 시간은 얼마로 해야 하나요?
**A:**
- **Access Token**: 1시간~1일 (짧을수록 안전)
- **Refresh Token**: 1주일~1개월
- 금융 앱: 더 짧게 (15분 등)

### Q6. 로그아웃은 어떻게 구현하나요?
**A:**
- **클라이언트**: 토큰 삭제 (LocalStorage.removeItem)
- **서버**: Blacklist (Redis에 만료된 토큰 저장)

---

**이전 문서**: [Spring Security 완벽 가이드](./spring_security.md)
