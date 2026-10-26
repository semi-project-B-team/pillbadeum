# 필받음 (pillbadeum)

> 사진 한 장이나 약 이름 검색으로 **복용 중인 약을 등록**하고, 그 약 목록을 기준으로 **AI 복약 상담**과 **복약 알림**을 받는 웹 서비스

회원 한 명이 본인과 가족 프로필을 여러 개 만들 수 있고, 약·복약 일정·상담 기록은 프로필마다 따로 관리됩니다.
상담 답변에는 식약처 공공데이터(의약품 허가정보·e약은요·DUR)를 근거로 붙여, 출처를 확인할 수 있게 했습니다.

- 팀: 5인 세미 프로젝트 (semi-project-B-team)
- 저장소: [semi-project-B-team/pillbadeum](https://github.com/semi-project-B-team/pillbadeum)
- 구성: Spring Boot API 서버 + React(Vite) 웹 클라이언트, 한 저장소에서 관리

---

## 목차

1. [서비스 흐름](#1-서비스-흐름)
2. [주요 기능](#2-주요-기능)
3. [시스템 구성](#3-시스템-구성)
4. [기술 스택](#4-기술-스택)
5. [저장소 구조](#5-저장소-구조)
6. [데이터베이스](#6-데이터베이스)
7. [API 목록](#7-api-목록)
8. [화면 목록](#8-화면-목록)
9. [시작하기](#9-시작하기)
10. [테스트와 검증](#10-테스트와-검증)
11. [협업 규칙](#11-협업-규칙)
12. [범위에서 제외한 것](#12-범위에서-제외한-것)
13. [관련 문서](#13-관련-문서)

---

## 1. 서비스 흐름

```text
회원가입·로그인 ─► 프로필 만들기(본인·가족)
                      │
                      ▼
            약 등록 방법 고르기
     ┌──────────────┼───────────────┐
 처방전·약봉투 사진    알약 사진        약 이름 검색
  (OCR로 이름 읽기)  (각인·모양·색 판독)  (허가정보 검색)
     └──────────────┼───────────────┘
                      ▼
          후보 확인 ─► 내 약 목록에 저장
                      │
        ┌─────────────┼──────────────┐
        ▼             ▼              ▼
   약 상세·근거 출처   복약 일정 등록      AI 복약 상담
   (e약은요/허가정보)  (화면·음성)         (내 약 기준 + DUR)
                      │
                      ▼
              복약 시각 웹 푸시 알림
```

---

## 2. 주요 기능

### 2-1. 회원 (`member`)

- 약관 동의 → 정보 입력 → 가입, 이메일 중복 확인
- 로그인 시 **Bearer JWT**(유효 1시간, Refresh Token 없음) 발급, 서버는 STATELESS
- 로그아웃은 프론트가 토큰을 지우는 방식이며, 401을 받으면 로그인 만료 화면으로 이동

### 2-2. 프로필 (`profile`)

- 본인·가족 프로필 생성·조회·수정·삭제
- 화면 헤더의 "관리할 사람"으로 프로필을 바꾸면 약·일정·상담 화면이 그 프로필 기준으로 바뀜
- 프로필마다 색을 정해 달력·태그 등에서 같은 사람을 같은 색으로 표시

### 2-3. 내 약 (`drug`)

- **약 이름 검색**: 공공데이터 의약품 허가정보 목록을 검색(전문·일반의약품 모두), 이미 등록한 약은 표시
- **내 약 목록·상세**: 제품명·제조사·전문/일반·모양·색·성분명·등록 방법·복용 정보
- **근거 출처(guide)**: e약은요 → 없으면 허가정보 상세에서 효능·용법·주의사항·보관법 등을 실시간으로 가져와 출처·자료 수정일과 함께 표시
- **저장**: 여러 약을 한 번에 저장, 같은 프로필에 같은 약은 중복 저장 불가(409)
- **삭제**: 내 약 삭제 시 해당 약의 일정·알림도 함께 빠짐
- 공용 약 정보(`drug`)는 처음 저장될 때 낱알식별 + 허가정보로 채워 DB에 보관

### 2-4. 사진으로 약 등록 (`image`)

- **처방전·약봉투**: 사진 1장 → AI 비전 모델로 약 이름을 읽고 → 이름별 공식 후보를 찾아 보여 줌
- **알약**: 앞·뒷면 사진 2장 → 각인·모양·색을 판독 → 낱알식별 카탈로그와 대조해 후보 제시
- 사용자가 후보를 확인해야 내 약으로 저장됨(자동 저장 없음)
- 업로드 사진은 공개 URL로 노출하지 않고, 판독 결과는 **30분**만 임시 보관 후 자동 정리
- WEBP 포함 이미지 디코딩·축소·메타데이터(EXIF) 제거 후 판독

### 2-5. AI 복약 상담 (`chat`)

- 프로필별 대화방 생성·목록·대화 기록 조회, 실패한 답변 재시도
- 질문 분류 → 내 약 목록·프로필 정보·공식 근거를 모아 답변 생성
- **DUR 안내**: 병용금기·연령·임부·노인 주의 등 공공데이터 DUR을 대조해 답변에 함께 표시, 답변 당시의 DUR 결과를 저장
- 긴 대화는 요약을 만들어 다음 답변의 맥락으로 사용
- (선택) **Qdrant** 벡터 검색: 질문·답변·요약을 임베딩해 과거 대화를 찾아 씀. Qdrant가 없거나 장애가 나면 MySQL의 최근 대화·요약으로 답변

### 2-6. 복약 일정·알림 (`medication`)

- **복용 달력**: 월별 복용 일정, 특정 날짜·시각에 먹을 약 조회
- **일정 관리**: 약별 기간·복용 시각(하루 최대 3회)·1회 복용량·복용 시점 등록·수정, 그만 먹기(종료일을 오늘로), 일정만 삭제
- **음성 일정 등록**: 말로 일정 입력 → AI가 해석해 저장, 완료 안내 음성(TTS) 재생
- **실시간 음성 대화**: GPT Live 세션으로 대화하며 일정 등록(세션당 최대 3분)
- **웹 푸시 알림**: VAPID 키 기반 브라우저 알림, 알림 설정 on/off. 서버가 **매분** 복용 시각을 확인해 발송하고, 알림을 누르면 그 시각에 먹을 약 화면이 열림

### 2-7. 공통 (`common`)

- JWT 발급·검증·필터, 401/403 처리, 프로필 소유권 확인
- 오류 응답은 `{ "code": ..., "message": ... }`로 통일, 내부 SQL·외부 응답 원문은 숨김
- 외부 호출 시간 제한·한정 재시도·동시 호출 상한(기본 4개), 요청 ID(MDC) 추적
- 한국 시간(Asia/Seoul) 기준 JVM·Jackson·JDBC·로그 설정

---

## 3. 시스템 구성

```text
┌───────────────────┐     /api (Vite 프록시)     ┌───────────────────────────┐
│  react-client     │ ─────────────────────────► │  pillbadeum-server        │
│  React + Vite     │ ◄───────────────────────── │  Spring Boot (:8080)      │
│  (:5173)          │      JSON / Bearer JWT     │                           │
└─────────┬─────────┘                            │  member · profile · drug  │
          │ Web Push                             │  image · chat · medication│
          ▼                                      └──┬──────┬──────┬───────┬──┘
   브라우저 푸시 서비스 ◄────── VAPID 발송 ─────────────┘      │      │       │
                                                        ▼      ▼       ▼
                                                 MySQL 8.4  Qdrant   외부 API
                                                 (원본)     (선택)   ├ OpenAI
                                                                     └ 공공데이터포털
```

| 외부 연동 | 용도 |
| --- | --- |
| OpenAI Responses API | 상담 답변 생성, 질문 분류, 처방전·알약 사진 판독, 음성 일정 해석 |
| OpenAI Embeddings | 대화 임베딩(기본 `text-embedding-3-small`) |
| OpenAI Speech / Live | 일정 저장 완료 음성(TTS), 실시간 음성 대화 |
| 의약품 허가정보 (`DrugPrdtPrmsnInfoService08`) | 약 이름 검색, 성분명, 허가정보 상세(근거 대체) |
| e약은요 (`DrbEasyDrugInfoService`) | 효능·사용법·주의사항 등 근거 출처 |
| 낱알식별 (`MdcinGrnIdntfcInfoService03`) | 모양·색·각인, 알약 사진 대조 카탈로그 |
| DUR 품목정보 (`DURPrdlstInfoService03`) | 병용금기·연령·임부 등 주의 정보 |

외부 키가 없으면 그 기능만 503으로 응답하고 나머지 기능은 그대로 동작합니다.

### 서버 정기 작업

| 작업 | 주기 |
| --- | --- |
| 복약 알림 발송 (`MedicationReminderScheduler`) | 매분 0초 (Asia/Seoul) |
| 사진·낱알 판독 임시 결과 정리 (`PhotoLifecycleService`) | 10분 |
| Qdrant 정리 대기 재시도 (`VectorCleanupService`) | 1분 |
| 낱알식별 카탈로그 갱신 (`PillCatalog`) | 6시간 |

---

## 4. 기술 스택

### 서버

| 구분 | 사용 기술 |
| --- | --- |
| 언어·런타임 | Java 21 |
| 프레임워크 | Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation, Actuator) |
| 빌드 | Gradle Wrapper 9.8.1 |
| 인증 | Spring Security + JJWT 0.13 (Bearer JWT) |
| DB | MySQL 8.4 (`ddl-auto: none`, SQL 파일로 스키마 관리) |
| 벡터 검색 | Qdrant 1.19 (선택, gRPC 클라이언트) |
| API 문서 | springdoc-openapi 3 (Swagger UI) |
| 기타 | PDFBox(식약처 공식 PDF 텍스트), TwelveMonkeys ImageIO(WEBP) |

### 클라이언트

| 구분 | 사용 기술 |
| --- | --- |
| 프레임워크 | React 19, Vite 8 |
| 라우팅 | React Router |
| 서버 요청 | axios (스트리밍 응답만 fetch) |
| 상태 관리 | Zustand (인증 등 공유 값만), 그 외 useState · URL 쿼리 |
| 스타일 | 순수 CSS + 디자인 토큰(`var(--토큰)`), UI 라이브러리·TypeScript 미사용 |
| 정적 검사 | ESLint |

### 인프라·협업

GitHub Actions(서버 CI), Docker Compose(로컬 MySQL·Qdrant, 선택), IntelliJ HTTP Client 요청 파일

---

## 5. 저장소 구조

```text
pillbadeum/
├─ .github/
│  ├─ pull_request_template.md       PR 템플릿 (제목 · 작업 내용 · 관련 이슈)
│  └─ workflows/pillbadeum-server-ci.yml
│
├─ pillbadeum-server/                Spring Boot API 서버
│  ├─ src/main/java/org/semiproject/pillbadeumserver/
│  │  ├─ common/                     ai · config · entity · exception · external · file · security · util · web
│  │  ├─ member/                     회원 · 로그인 · 약관
│  │  ├─ profile/                    본인 · 가족 프로필
│  │  ├─ drug/                       약 검색 · 약 정보 · 내 약 · 근거 출처
│  │  ├─ image/                      사진 업로드 · OCR · 알약 식별 · 후보 저장
│  │  ├─ chat/                       대화방 · 질문 분류 · 답변 생성 · DUR · 요약 · 벡터
│  │  └─ medication/                 복약 일정 · 달력 · 음성 · 실시간 음성 · 웹 푸시
│  │     (각 업무 패키지는 controller / service / domain / dto)
│  ├─ src/main/resources/            application.yaml, application-local.yaml, 개발용 테스트 페이지
│  ├─ src/test/                      단위 · WebMvc · MySQL · 외부 연동 테스트, 점검용 CLI
│  ├─ sql/                           00~13 DB 생성 · 스키마 · 패치 · 약관 SQL
│  ├─ docs/                          기능별 안내, 검증 기록, OpenAPI JSON
│  ├─ requests/                      기능별 .http 요청 예시
│  ├─ scripts/                       로컬 DB 준비, 실연동 점검 스크립트
│  ├─ compose.yaml                   로컬 MySQL (선택)
│  ├─ compose.qdrant.yaml            로컬 Qdrant (선택)
│  └─ .env.example                   개인 환경변수 양식
│
└─ react-client/                     React + Vite 웹 클라이언트
   ├─ src/
   │  ├─ api/                        서버 요청 (auth · profiles · drugs · photos · chat · schedules · notifications · live)
   │  ├─ pages/                      주소 하나에 대응하는 화면
   │  ├─ components/                 화면 조각 (헤더 · 달력 · 채팅 · 약 선택 등)
   │  ├─ store/                      Zustand 스토어 (인증 · 처방전 임시 결과)
   │  ├─ router.js, paths.js         라우트와 화면 주소
   │  └─ tokens.css                  디자인 토큰
   ├─ design/                        디자인 시스템(Pillbox) 토큰 · 컴포넌트 명세
   ├─ docs/                          화면별 구현 메모 (인증 · 채팅)
   └─ AGENTS.md                      클라이언트 작업 지침
```

---

## 6. 데이터베이스

MySQL 8.4, DB 이름 `pilldb`. 최신 스키마(`sql/01`) 기준 **18개 테이블**입니다.

| 영역 | 테이블 |
| --- | --- |
| 회원·약관 | `member`, `consent`, `consent_history` |
| 프로필 | `profile` |
| 약 | `drug`(공용 약 정보), `my_drug`(프로필별 내 약 + 복약 일정) |
| DUR | `dur_warning`, `dur_interaction` |
| 사진 | `photo`, `ocr_candidate`, `pill_identification_session` |
| 상담 | `chat_session`, `chat_message`, `chat_summary`, `vector_cleanup` |
| 알림 | `notification_setting`, `notification_dispatch`, `push_subscription` |

- 서버는 SQL을 자동 실행하지 않습니다. 스키마 변경은 `sql/`에 번호를 붙인 패치 파일로 추가합니다.
- 신규 설치는 `00 → 01 → 13`(샘플이 필요하면 `02`), 기존 DB는 빠진 `03~12`와 `13`을 적용합니다. 자세한 내용은 [SQL 안내](pillbadeum-server/sql/README.md)를 참고하세요.
- Qdrant는 별도 저장소이며 MySQL이 원본 데이터입니다.

---

## 7. API 목록

모든 API는 `/api`로 시작하며, 회원가입·로그인 등 공개 API를 빼면 `Authorization: Bearer <토큰>`이 필요합니다.
전체 요청·응답 형식은 로컬 Swagger 또는 [openapi.json](pillbadeum-server/docs/openapi.json)을 보세요.

<details>
<summary>회원 · 프로필</summary>

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/auth/consent-policies` | 가입 약관 조회 |
| GET | `/api/auth/email-availability` | 이메일 중복 확인 |
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 (JWT 발급) |
| GET | `/api/auth/me` | 내 정보 |
| GET · POST | `/api/profiles` | 프로필 목록 · 생성 |
| PUT · DELETE | `/api/profiles/{profileId}` | 프로필 수정 · 삭제 |

</details>

<details>
<summary>약 · 내 약</summary>

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/drugs/search` | 약 이름 검색 |
| GET | `/api/drugs/{itemSeq}` | 약 정보 (없으면 공공데이터에서 가져와 저장) |
| GET | `/api/drugs/{itemSeq}/guide` | 근거 출처 (효능 · 용법 · 주의 · DUR) |
| GET · POST | `/api/profiles/{profileId}/drugs` | 내 약 목록 · 저장 |
| GET · DELETE | `/api/profiles/{profileId}/drugs/{myDrugId}` | 내 약 상세 · 삭제 |

</details>

<details>
<summary>사진 등록</summary>

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/profiles/{profileId}/photo-identifications` | 처방전 · 약봉투 · 알약 사진 식별 (multipart) |
| GET | `/api/photos/{photoId}/candidates` | 사진에서 찾은 후보 다시 불러오기 |
| GET · DELETE | `/api/photos/{photoId}/image` · `/api/photos/{photoId}` | 업로드 사진 조회 · 삭제 |
| GET · DELETE | `/api/profiles/{profileId}/pill-identifications/{identificationId}` | 알약 판독 결과 복원 · 삭제 |
| POST | `/api/images/profiles/{profileId}/drugs` | 알약 후보로 내 약 저장 |

</details>

<details>
<summary>AI 상담</summary>

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET · POST | `/api/profiles/{profileId}/chat-sessions` | 대화방 목록 · 생성 |
| GET · POST | `/api/chat-sessions/{sessionId}/messages` | 대화 기록 · 질문 보내기 |
| POST | `/api/chat-sessions/{sessionId}/messages/{messageId}/retry` | 실패한 답변 재시도 |

</details>

<details>
<summary>복약 일정 · 알림</summary>

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/calendar` | 월별 복용 달력 |
| GET | `/api/calendar/doses` | 특정 시각에 먹을 약 |
| GET | `/api/profiles/{profileId}/drugs/schedule-choices` | 일정 등록할 약 고르기 |
| GET · PUT · DELETE | `/api/profiles/{profileId}/drugs/{myDrugId}/schedule` | 일정 조회 · 저장 · 삭제 |
| POST | `/api/profiles/{profileId}/drugs/{myDrugId}/schedule/stop` | 그만 먹기 |
| POST | `/api/schedule-voice-commands` | 음성으로 일정 등록 |
| POST · GET · DELETE | `/api/medication/live/sessions[/{sessionId}]` | 실시간 음성 대화 세션 |
| GET · PUT | `/api/notification-settings` | 알림 설정 |
| GET | `/api/push/vapid-public-key` | 웹 푸시 공개 키 |
| POST | `/api/push/subscriptions` | 브라우저 푸시 구독 등록 |

</details>

local 프로필에서만 열리는 개발용 페이지: `/dev/medication-live`(실시간 음성 테스트), `/dev/push-test`(푸시 발송 테스트)

---

## 8. 화면 목록

화면 ID는 팀 목업 v2 보드 기준입니다. (`react-client/src/paths.js`)

| 구분 | 화면 | 주소 |
| --- | --- | --- |
| 회원 | MEM-01 시작 · MEM-02 약관 동의 · MEM-03 정보 입력 · MEM-04 로그인 | `/welcome` · `/signup` · `/signup/info` · `/login` |
| 공통 | CMN-01 홈 · CMN-03 로그인 만료 | `/` · `/session-expired` |
| 내 약 | MED-01 약 목록 · MED-02 등록 방법 · MED-03 이름 검색 · MED-04 약 상세 | `/drugs` · `/drugs/new` · `/drugs/search` · `/drugs/:myDrugId` |
| 사진 | IMG-01 처방전·약봉투 사진 · IMG-02 알약 사진 | `/drugs/new/prescription` · `/drugs/new/pill` |
| 일정 | SCH-01 복용 달력 · SCH-03 일정 추가 · SCH-06 일정 수정 | `/calendar` · `/schedules/new` · `/schedules/edit` |
| 상담 | CHT-01 대화방 목록 · CHT-02 대화방 | `/chat` · `/chat/:sessionId` |
| 프로필 | PRF-01 관리 · PRF-02 만들기 · PRF-03 수정 | `/profiles` · `/profiles/new` · `/profiles/:profileId/edit` |
| 알림 | NTF-01 알림 설정 · NTF-02 그 시각에 먹을 약 | `/notifications` · `/doses` |

---

## 9. 시작하기

### 9-1. 준비물

| 도구 | 버전 | 비고 |
| --- | --- | --- |
| JDK | 21 | Gradle Wrapper가 포함되어 있어 Gradle 설치는 불필요 |
| MySQL | 8.4 | 직접 설치하거나 Docker Compose 사용 |
| Node.js · npm | Vite 8을 지원하는 버전 | 클라이언트 실행용 |
| Docker | 선택 | MySQL · Qdrant를 컨테이너로 띄울 때 |

### 9-2. 환경변수 (`pillbadeum-server/.env`)

```powershell
cd pillbadeum-server
Copy-Item .env.example .env     # macOS/Linux: cp .env.example .env
```

| 변수 | 필수 | 설명 |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | ✅ | 로컬 MySQL 연결 (예제 주소 그대로 사용 가능) |
| `JWT_SECRET` | ✅ | Base64URL 32바이트 이상 개인 서명키. 없거나 짧으면 서버가 시작되지 않음 |
| `OPENAI_API_KEY`, `OPENAI_CHAT_MODEL`, `OPENAI_VISION_MODEL` 등 | 선택 | AI 상담 · 사진 판독 · 음성 기능 |
| `PUBLIC_DATA_SERVICE_KEY` | 선택 | 공공데이터포털 인증키(Decoding) |
| `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT` | 선택 | 웹 푸시. 셋을 함께 넣거나 모두 비움 |
| `QDRANT_ENABLED` | 선택 | Qdrant를 띄운 경우에만 `true` |

JWT 키 생성 예: `python -c "import secrets; print(secrets.token_urlsafe(32))"`
VAPID 키 생성 예: `npx web-push generate-vapid-keys`
`.env`는 Git에 올리지 않으며, 실제 키는 공유하지 않습니다. 전체 항목은 [서버 README](pillbadeum-server/README.md)에 정리되어 있습니다.

### 9-3. DB 준비

```powershell
# 설치된 MySQL 사용 (00 → 01 → 13 적용)
.\scripts\setup-local-db.ps1
.\scripts\setup-local-db.ps1 -IncludeTestData   # 샘플 데이터까지
.\scripts\setup-local-db.ps1 -UpgradeExisting   # 기존 DB에 빠진 패치 적용

# 또는 Docker Compose 사용 (.env에 MYSQL_ROOT_PASSWORD 추가)
docker compose up -d --wait
docker compose exec mysql mysql -u pill -p pilldb -e "source /sql/01_PILLBADEUM_DB_SCRIPT.sql; source /sql/13_BASE_CONSENT.sql;"
```

### 9-4. 서버 실행

```powershell
.\gradlew.bat bootRun           # macOS/Linux: ./gradlew bootRun
```

| 주소 | 내용 |
| --- | --- |
| http://localhost:8080/actuator/health | 상태 확인 |
| http://localhost:8080/swagger-ui/index.html | Swagger (local 프로필) |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

### 9-5. 클라이언트 실행

```powershell
cd react-client
npm install
npm run dev                     # http://localhost:5173
```

개발 서버가 `/api` 요청을 `http://localhost:8080`으로 넘기므로 CORS 설정 없이 연결됩니다.

| 명령 | 내용 |
| --- | --- |
| `npm run dev` | 개발 서버 |
| `npm run build` · `npm run preview` | 배포용 빌드 · 빌드 결과 미리 보기 |
| `npm run lint` | ESLint |
| `npm run tokens` | 디자인 토큰 → `tokens.css` 생성 |

### 9-6. (선택) Qdrant

```powershell
docker compose -f compose.qdrant.yaml up -d     # 대시보드 http://localhost:6333/dashboard
```

`.env`에 `QDRANT_ENABLED=true`를 넣고 서버를 재시작합니다.

---

## 10. 테스트와 검증

```powershell
cd pillbadeum-server
.\gradlew.bat test bootJar
```

- 기본 테스트는 MySQL 없이 보안 필터 · CORS · 공통 오류 · WebMvc · 서비스 단위를 검증합니다.
- 실제 DB 테스트(`*MySqlTests`)는 별도 `pill_test` DB에서만 실행되며, `MYSQL_TEST_URL` · `MYSQL_TEST_USER` · `MYSQL_TEST_PASSWORD`가 있을 때만 동작합니다.
- **CI**: `pillbadeum-server/**`가 바뀐 push · PR마다 GitHub Actions가 MySQL 8.4 · Qdrant 서비스를 띄워 `./gradlew test bootJar`를 실행하고 테스트 리포트를 올립니다.

실제 외부 API를 부르는 점검은 일반 테스트와 분리된 Gradle 작업으로만 실행합니다.

| 작업 | 내용 | 외부 호출 |
| --- | --- | --- |
| `checkChatConfiguration` | 챗봇 키 · 모델 설정 누락 점검 | 없음 |
| `tryQuestionClassifier` | 질문 직접 입력 → 실제 분류 결과 · 비용 확인 | OpenAI (유료) |
| `tryChatProviders` | OpenAI · 공공데이터 연결 점검 | OpenAI (유료) · 공공데이터 |
| `tryOfficialEvidence` | 품목 하나의 e약은요 · 허가정보 근거 점검 | 공공데이터 |
| `tryDurEvidence` | 품목 하나의 DUR 유형별 결과 점검 | 공공데이터 |
| `syncPillCatalog` | 낱알식별 카탈로그 적재 · 식별 점검 | 공공데이터 |

API를 손으로 확인할 때는 `pillbadeum-server/requests/*.http`(IntelliJ HTTP Client)를 사용합니다.

---

## 11. 협업 규칙

- **브랜치**: `develop`을 기준으로 기능 브랜치(`feature/*`, `fix/*`, `test/*`, `chore/*`)를 만들어 작업하고, PR로 `develop`에 병합합니다.
- **PR**: `.github/pull_request_template.md` 형식(제목 · 작업 내용 · 관련 이슈)으로 작성하고, 서버 변경은 CI 통과 후 병합합니다.
- **설정**: 개인 `.env`는 올리지 않고 `.env.example`만 공유합니다. `build/`, `.gradle/`, `.idea/`, 로그, 업로드 파일도 제외합니다.
- **DB**: `ddl-auto: none`. 스키마 변경은 `sql/`에 다음 번호로 추가하고, 기존 패치 파일은 수정하지 않습니다.
- **서버 코드**: 공통 코드는 `common` 한 곳에만 둡니다. 정상 응답은 업무 DTO를 그대로, 오류는 `{code, message}`, 204는 본문 없이 반환합니다.
- **클라이언트 코드**: 서버 요청은 `src/api/`에만 둡니다. 색은 반드시 `var(--토큰)`으로 쓰고, 허용된 라이브러리(react · react-dom · react-router · axios · zustand) 외에는 추가하지 않습니다. 자세한 규칙은 [AGENTS.md](react-client/AGENTS.md)를 참고하세요.

---

## 12. 범위에서 제외한 것

- 시럽 · 주사 · 연고 등 정제/캡슐이 아닌 약 (백로그)
- 복약 완료 체크 · 실제 복용 기록
- 내 약 복용 정보 별도 수정 API (복약 일정 저장 API로 대신함)
- Refresh Token · 토큰 차단 목록 (1시간 만료 후 재로그인)

---

## 13. 관련 문서

| 문서 | 내용 |
| --- | --- |
| [서버 README](pillbadeum-server/README.md) | 환경변수 전체, MySQL · Qdrant 준비, 인증 정책, 외부 호출 시간 제한 |
| [패키지 안내](pillbadeum-server/docs/PACKAGE_GUIDE.md) | common · 업무 패키지 구조와 역할 |
| [SQL 안내](pillbadeum-server/sql/README.md) | 신규 · 기존 DB별 SQL 적용 순서, 파일별 역할 |
| [사진 등록](pillbadeum-server/docs/PHOTO_REGISTRATION.md) | 처방전 · 약봉투 사진 등록 흐름 |
| [낱알 식별](pillbadeum-server/docs/PILL_IDENTIFICATION.md) | 알약 사진 식별 |
| [임시 사진 정리](pillbadeum-server/docs/TEMP_IMAGE_CLEANUP.md) | 업로드 사진 정리 방식 |
| [복약 일정](pillbadeum-server/docs/MEDICATION_SCHEDULE.md) | 일정 API · 삭제 동작 |
| [복약 알림](pillbadeum-server/docs/MEDICATION_NOTIFICATION.md) | 웹 푸시 알림 |
| [실시간 음성 대화](pillbadeum-server/docs/MEDICATION_LIVE.md) | GPT Live 일정 등록 |
| [직접 입력 약](pillbadeum-server/docs/MEDICATION_CUSTOM_NAME.md) | 품목 없이 이름만 입력한 약 |
| [프론트 연결 계약](pillbadeum-server/docs/MEDICATION_FRONTEND_HANDOFF.md) | 복약지도 화면과 API 연결 기준 |
| [테스트 계정 준비](pillbadeum-server/docs/MEDICATION_TEST_ACCOUNT.md) | 프로필 · 내 약 · 일정이 준비된 테스트 계정 만들기 |
| [백엔드 검증 기록](pillbadeum-server/docs/MEDICATION_VERIFICATION.md) | 실행 방법과 확인 결과 |
| [OpenAPI](pillbadeum-server/docs/openapi.json) | 전체 API 명세 |
| [디자인 시스템](react-client/design/README.md) | Pillbox 토큰 · 색 · 컴포넌트 원칙 |
| [클라이언트 작업 지침](react-client/AGENTS.md) | 폴더 구조 · 데이터 요청 · 상태 관리 · 스타일 규칙 |
| [인증 · 로그아웃 지침](react-client/docs/auth-logout.md) | 프론트 인증 처리 규칙 |
