# SideSignal

SideSignal은 자율출퇴근 환경에서 함께 일하는 두 사람이 조용하게 상태와 퇴근 예정 시간을 공유하는 macOS 메뉴바 앱이다.

업무 흐름을 방해하지 않는 작은 신호를 목표로 한다. 상태 변경과 퇴근 시간 입력은 macOS 앱에서 처리하고, Spring Boot 서버가 인증, 페어링, 상태 저장, 실시간 이벤트 전송을 담당한다.

## 핵심 기능

| 기능 | 설명 |
|---|---|
| 상태 공유 | 집중 중, 회의 중, 커피 가능, 점심 가능, 퇴근 준비 상태를 공유한다. |
| 퇴근 시간 공유 | 각자의 예상 퇴근 시간을 입력하고 상대방 화면에 동기화한다. |
| 로컬 알림 | 상대방의 퇴근 시간이 가까워지면 macOS 알림을 표시한다. |
| 초대 코드 페어링 | 한 사용자가 초대 코드를 만들고, 다른 사용자가 입력해서 pair를 생성한다. |
| 실시간 동기화 | 상태 변경 이벤트를 SSE로 전달한다. |
| 재연결 복구 | 절전 해제나 네트워크 변경 후 SSE를 다시 연결하고 최신 상태를 조회한다. |

## 기술 스택

| 영역 | 기술 |
|---|---|
| macOS 앱 | SwiftUI, MenuBarExtra |
| 백엔드 | Spring Boot |
| 데이터베이스 | PostgreSQL |
| 인증 | JWT |
| 실시간 통신 | Server-Sent Events |
| 로컬 개발 | Docker Compose |

## 아키텍처

```text
macOS App
  | REST API
  | - 로그인
  | - 초대 코드 생성/수락
  | - 상태 및 퇴근 시간 변경
  |
  | SSE
  | - 상대 상태 변경 수신
  | - 상대 퇴근 시간 변경 수신
  v
Spring Boot Server
  |
  v
PostgreSQL
```

REST API는 명령과 최신 상태 조회를 담당하고, SSE는 서버에서 클라이언트로 전달되는 실시간 이벤트만 담당한다. SideSignal의 실시간 요구사항은 양방향 채팅이 아니라 낮은 빈도의 상태 변경 알림이므로 WebSocket보다 SSE가 단순하고 적합하다.

## 저장소 구조

```text
SideSignal/
  README.md
  server/
  macos/
```

| 디렉터리 | 역할 |
|---|---|
| `server` | Spring Boot 백엔드 애플리케이션 |
| `macos` | SwiftUI 기반 macOS 메뉴바 앱 |

## 주요 도메인

| 도메인 | 역할 |
|---|---|
| User | 사용자 계정과 인증 정보를 관리한다. |
| Pair | 두 사용자의 연결 관계를 관리한다. |
| PairInvite | 초대 코드 발급, 만료, 수락 상태를 관리한다. |
| Signal | 현재 상태, 퇴근 예정 시간, 짧은 메시지를 관리한다. |
| SignalEvent | SSE 재전송과 최신 상태 복구에 필요한 이벤트를 기록한다. |

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
| `GET` | `/api/v1/pairs/current/signals` | pair 멤버들의 최신 상태 조회 |
| `GET` | `/api/v1/pairs/current/events` | SSE 이벤트 구독 |

## SSE 이벤트 흐름

```text
1. MacBook A가 상태나 퇴근 시간을 변경한다.
2. Spring Boot 서버가 요청을 검증하고 PostgreSQL에 저장한다.
3. 서버가 pair 상대방의 SSE 연결로 signal.updated 이벤트를 보낸다.
4. MacBook B가 메뉴바 UI를 갱신한다.
5. 퇴근 시간이 가까우면 MacBook B가 로컬 알림을 예약한다.
```

## MVP 로드맵

- Spring Boot 서버 프로젝트 구성
- PostgreSQL Docker 개발 환경 구성
- JWT 로그인 구현
- 초대 코드 기반 pair 생성 구현
- 상태 및 퇴근 시간 API 구현
- SSE 이벤트 스트림 구현
- SwiftUI 메뉴바 앱 구성
- macOS 로컬 알림 구현
- 절전 해제와 네트워크 변경 후 재연결 처리
- 실행 방법과 화면 예시 정리

## 포트폴리오 포인트

- Spring Boot 기반 API 설계와 계층형 구조
- PostgreSQL 도메인 모델링
- JWT 인증과 pair 단위 권한 검증
- SSE 기반 실시간 동기화
- macOS 클라이언트와 백엔드 연동
- 네트워크 복구와 최신 상태 재조회 흐름
