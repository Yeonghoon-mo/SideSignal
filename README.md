# SideSignal

자율출퇴근 환경에서 함께 일하는 두 사람이 조용하게 상태와 퇴근 예정 시간을 공유하는 macOS 메뉴바 앱이다.

Slack 채널에 "오늘 몇 시야?"를 물어보는 상황을 없애는 게 목표다. 상태 변경과 퇴근 시간 입력은 macOS 메뉴바에서 처리하고, Spring Boot 서버가 인증, 페어링, 상태 저장, 실시간 이벤트 전송을 담당한다.

## 핵심 기능

| 기능 | 설명 |
|---|---|
| 상태 공유 | 집중 중, 회의 중, 커피 가능, 점심 가능, 퇴근 준비 6가지 상태를 공유한다. |
| 퇴근 시간 공유 | 예상 퇴근 시간을 입력하고 상대방 화면에 실시간으로 동기화한다. |
| 콕 찌르기 | 상대방에게 간단한 신호를 보낸다. 쿨다운 제한이 있다. |
| 로컬 알림 | 상대방의 퇴근 시간 10분 전과 도달 시 macOS 알림을 표시한다. |
| 초대 코드 페어링 | 한 사용자가 초대 코드를 만들고, 다른 사용자가 입력해 pair를 생성한다. |
| 재연결 복구 | 절전 해제나 네트워크 변경 후 SSE를 다시 연결하고 최신 상태를 조회한다. |

## 기술 스택

| 영역 | 기술 |
|---|---|
| macOS 앱 | SwiftUI, MenuBarExtra, UserNotifications, async/await |
| 백엔드 | Spring Boot 4.0, Java 21, Gradle |
| 데이터베이스 | PostgreSQL |
| 인증 | JWT (HS256, 외부 라이브러리 없이 직접 구현) |
| 실시간 통신 | Server-Sent Events (SseEmitter) |
| 마이그레이션 | Flyway |
| API 문서 | springdoc-openapi (Swagger UI) |
| 테스트 | JUnit 5, AssertJ, Mockito, Testcontainers |
| 배포 | Docker 멀티 스테이지 빌드, Railway |

## 아키텍처

```text
macOS App (MenuBarExtra)
  │
  ├── REST API (명령, 최신 상태 조회)
  │   - POST /auth/register, /auth/login
  │   - POST /pair-invites, /pair-invites/{code}/accept
  │   - PATCH /me/signal
  │   - POST /pairs/current/poke
  │
  └── SSE (서버 → 클라이언트 단방향 이벤트)
      - signal.updated
      - poke.received
      - heartbeat (45초 간격)
      │
      v
Spring Boot Server
  │
  └── PostgreSQL (Flyway 자동 마이그레이션)
```

SideSignal의 실시간 요구사항은 양방향 채팅이 아니라 낮은 빈도의 상태 변경 알림이다. WebSocket보다 SSE가 단순하고 적합하다고 판단해 선택했다. REST API는 명령과 최신 상태 조회를 담당하고, SSE는 서버에서 클라이언트로 전달되는 이벤트만 담당한다.

## 백엔드 설계

### 패키지 구조

```text
com.sidesignal
  auth        # 인증 (JWT 로그인, 회원가입)
  pair        # 페어링 (초대 코드 생성 / 수락)
  signal      # 상태 관리 (상태 변경, 퇴근 시간)
  poke        # 콕 찌르기 (쿨다운 제한)
  realtime    # SSE 이벤트 스트림
  common      # 공통 설정, 보안, 에러 처리
```

각 도메인은 `api / application / domain / infrastructure` 4계층으로 구성된다. Controller는 `@AuthenticationPrincipal`로 인증된 사용자만 받고, 권한 검증(pair 멤버십 확인)은 Service에서 일관되게 처리한다.

### 도메인 모델

| 도메인 | 역할 | 주요 설계 결정 |
|---|---|---|
| User | 사용자 계정과 인증 정보 | UUID PK, BCrypt 해시 저장 |
| Pair | 두 사용자의 연결 관계 | firstUser / secondUser, 생성 후 불변 |
| PairInvite | 초대 코드 발급 / 수락 | codeHash만 저장 (평문 코드는 응답 시 1회만 노출), 24시간 만료 |
| Signal | 현재 상태와 퇴근 시간 | (pair_id, user_id) 고유제약으로 페어당 사용자 Signal 1개 보장 |
| SignalEvent | SSE 재전송 / 복구용 이벤트 로그 | JSONB 페이로드, (pair_id, created_at) 복합 인덱스 |

### 에러 처리

GlobalExceptionHandler가 BusinessException, 인증 / 권한 오류, 요청 검증 실패를 통일된 형식으로 응답한다.

```json
{
  "timestamp": "2026-04-24T10:15:30Z",
  "status": 409,
  "code": "ALREADY_PAIRED",
  "message": "이미 짝이 맺어진 상태",
  "path": "/api/v1/pair-invites"
}
```

주요 ErrorCode: `DUPLICATE_EMAIL`, `ALREADY_PAIRED`, `INVITE_EXPIRED`, `PAIR_NOT_FOUND`, `POKE_COOLDOWN (429)`

## 실시간 통신 (SSE)

### 서버 구현

`SseEmitterRepository`는 `ConcurrentHashMap<UUID, SseEmitter>`로 활성 연결을 관리한다. 클라이언트별 1개 emitter만 유지하고, 상태 변경 시 pair 상대방의 emitter를 찾아 즉시 전송한다.

```text
1. 클라이언트가 /pairs/current/events에 SSE 연결
2. 서버가 연결 즉시 connect 이벤트 전송 (503 방지)
3. 45초마다 heartbeat 전송 (Nginx / 프록시 타임아웃 방지)
4. 상태 변경 요청 수신 → DB 저장 → 상대방 emitter에 signal.updated 전송
5. IOException 발생 시 emitter 자동 제거
```

emitter 1시간 타임아웃으로 설정되어 있으며, onCompletion / onTimeout / onError 콜백으로 연결 종료를 처리한다. 현재 모노리식 단일 서버 배포 기준이며, 수평 확장 시 Redis Pub/Sub으로 대체해야 한다.

### 클라이언트 재연결

macOS 앱은 `NSWorkspace.didWakeNotification`과 `NWPathMonitor`로 절전 해제와 네트워크 복구를 감지해 SSE를 다시 연결한다. 재연결 실패 시 지수 백오프(초기 2초, 최대 30초)를 적용한다.

재연결 후에는 REST API로 최신 상태를 직접 조회해 UI를 갱신하고, SSE 이벤트 스트림을 다시 구독한다.

## 보안

JWT를 외부 라이브러리 없이 직접 구현했다. Base64URL 인코딩 / HMAC-SHA256 서명 / issuer · exp 클레임 검증을 수동으로 처리한다. secret 길이는 @PostConstruct에서 32자 이상 강제한다.

초대 코드는 평문을 DB에 저장하지 않고 해시만 저장한다. 코드 원문은 생성 API 응답에서 1회만 노출된다.

## 테스트 전략

Testcontainers로 PostgreSQL 17 컨테이너를 스핀업해 실제 DB 환경에서 통합 테스트를 실행한다. H2 인메모리나 Mock Repository를 쓰지 않아 Flyway 마이그레이션, 고유제약, 인덱스까지 검증할 수 있다.

| 테스트 | 검증 내용 |
|---|---|
| AuthControllerIntegrationTests | 회원가입 / 로그인, 중복 이메일, 토큰 발급 |
| SignalControllerIntegrationTests | 상태 변경, 페어링 없는 사용자 차단, 초기 OFFLINE 상태 확인 |
| PairControllerIntegrationTests | 초대 코드 발급 / 수락, 만료 코드 처리 |
| RealtimeControllerIntegrationTests | SSE 연결 응답, 이벤트 전송 |
| PokeControllerIntegrationTests | 콕 찌르기, 쿨다운 제한 (429) |

MockMvc로 REST API를 호출하고 AssertJ로 응답 상태와 바디를 검증한다.

## 저장소 구조

```text
SideSignal/
  README.md
  SideSignal-Server/   # Spring Boot 백엔드
  SideSignal-Swift/    # SwiftUI macOS 앱
  docs/                # 설계 문서
```

## 실행 방법

### 사전 요구 사항

- Java 21
- PostgreSQL (localhost:5432, database: `sidesignal`, user: `sidesignal`)
- macOS 14 이상 (클라이언트)

### 서버 실행

```bash
cd SideSignal-Server
./gradlew bootRun --args='--spring.profiles.active=local'
```

서버는 포트 8080에서 시작된다. Flyway가 자동으로 스키마를 생성한다.

### macOS 앱 실행

```bash
cd SideSignal-Swift
swift build
SIDESIGNAL_API_BASE_URL=http://localhost:8080/api/v1 .build/debug/SideSignal
```

또는 Xcode에서 `SideSignal-Swift/Package.swift`를 열어 실행한다.

## 배포 (Railway)

멀티 스테이지 Docker 빌드를 사용한다. 빌드 스테이지에서 `gradle:8.13-jdk21-alpine`으로 bootJar를 생성하고, 런타임 스테이지에서 `eclipse-temurin:21-jre`에 복사한다. 컨테이너는 비루트 사용자(`svc`)로 실행된다.

SideSignal은 모노레포 구조이므로 Railway 서비스의 Root Directory를 `SideSignal-Server`로 지정한다.

### 환경변수

| 변수 | 값 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PGHOST` | `${{Postgres.PGHOST}}` |
| `PGPORT` | `${{Postgres.PGPORT}}` |
| `PGDATABASE` | `${{Postgres.PGDATABASE}}` |
| `PGUSER` | `${{Postgres.PGUSER}}` |
| `PGPASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `SIDESIGNAL_JWT_SECRET` | 32자 이상 임의 문자열 |

## API 개요

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/api/v1/auth/register` | 회원가입 |
| `POST` | `/api/v1/auth/login` | 로그인 및 JWT 발급 |
| `POST` | `/api/v1/pair-invites` | 초대 코드 생성 |
| `POST` | `/api/v1/pair-invites/{code}/accept` | 초대 코드 수락 |
| `GET` | `/api/v1/pairs/current` | 현재 pair 조회 |
| `GET` | `/api/v1/me/signal` | 내 상태 조회 |
| `PATCH` | `/api/v1/me/signal` | 내 상태 및 퇴근 시간 변경 |
| `DELETE` | `/api/v1/me/signal/departure-time` | 퇴근 시간 초기화 |
| `GET` | `/api/v1/pairs/current/signals` | pair 멤버들의 최신 상태 조회 |
| `POST` | `/api/v1/pairs/current/poke` | 상대방 콕 찌르기 |
| `GET` | `/api/v1/pairs/current/events` | SSE 이벤트 구독 |
