# 환경설정 전면 재검토와 최소 재구성

검토일: 2026-10-17 KST. 기준 코드: develop `aa15bf771616ce0c5faf581d2b6b5bad37b95af9`.
작업 브랜치: `pill-config`. 팀 확정사항과 이번 작업안, 문서 충돌, 기능 담당의 후속 작업을 구분합니다.
개인 노션: https://app.notion.com/p/3fcbf16cc3ec81dab02decb3d435618c

## 1. 결론과 작업 범위

기존 Java/Spring/JWT/RestClient 기반은 수업 내용에 근거가 있었습니다. 원본 전체가 기술스택에
맞지 않았다고 판단하지 않습니다. 이번 작업은 팀 설계와 다른 계약, 누락된 저장 구조,
사용하지 않는 공통 코드와 중복을 정리하는 환경설정 PR입니다.

기존 업무 패키지 member/profile/drug/image/chat/medication과 사용자 추가 Qdrant 연결을 보존합니다.
로그인·사진 식별·대화·약 등록 등 전체 업무 API/Entity를 대신 구현하지 않습니다.
Redis, WebFlux, Refresh Token, 메시지 큐, MSA, Flyway, Resilience4j를 추가하지 않습니다.

인증은 사용자의 “인증은 Bearer를 사용한다”는 최종 확인과 수업 기본 구현을 기준으로
Bearer JWT로 적용합니다. API/FS의 쿠키 문구는 이 적용 기준과 다르므로 수정 대상으로 기록합니다.
토큰 인증과 쿠키 전달은 서로 다른 개념이며, 쿠키 문구가 팀에서 확정됐다고 간주하지 않습니다.

## 2. 무엇을 확인했는가

| 자료 | 확인 범위와 우선순위 |
| --- | --- |
| 팀 프로젝트 루트 | 직접 설계 링크 전체 확인 |
| Architecture ver.4 | PRD 42행·FS 50행·UFM 15행과 본문 총107개 확인 |
| API 명세 | 전체 확인. 문서 자체가 충돌 시 FS ver.4 우선이라고 명시 |
| 첨부 ERD 이미지 | MySQL14개+Qdrant1개. 노션 ERD와 별도로 대조 |
| 노션 ERD v4 | 본문·첨부 SVG 전체 텍스트 확인. 이미지와 같은 판이 아님 |
| 요구사항 페이지 | 연결된 것은 과거 PRD28행. 최신 상세 기준으로 오인하지 않음 |
| 화면정의/DDD/용어집/API추출표 | 텍스트와 접근 가능한 자료 확인. 아래 제한 별도 기재 |
| 수업 자료 | 강의 본문30개와 Spring/Security/JPA/가상스레드/LLM/모니터링 등 목록 확인 |
| 원본 프로젝트 두 개 | 메일 ZIP의 두 원본 SQL 모두 같은10개 테이블인 사실 확인 |
| 최신 develop | 작업 시작 때 aa15bf7을 다시 fetch하고 별도 worktree에서 작업 |

FS 상태는 확정8·확인필요24·제안13·보류5입니다. 문서에 적힌 모든 기본값이 팀 확정이라는
의미는 아닙니다. 서로 다른 판의 표를 개수만으로 같다고 보지 않습니다.

### 확인하지 못한 자료

지정한 학원 GitHub9개는 gh와 GitHub 연결 모두에서 접근되지 않았습니다. 저장소가 없다고
단정하지 않으며 전체 실습 소스 검토를 완료했다고 표현하지 않습니다. 대체 근거는 실제
수업 Notion 본문입니다. DDD의 Miro 임베드 내용과 Claude 화면3개의 실제 화면은 확인할 수
없었습니다(Claude 링크는 JS shell만 응답). 용어집은 비어 있고 API 추출표는 e약은요5개 필드만
적힌 미완성 표입니다. 따라서 공식 API의 모든 필드/파라미터 검수가 끝났다고 할 수 없습니다.
Notion SQL 조회 한도 이후 공식 view 방식으로 목록을 회수했습니다.

## 3. 수업 범위와 유지·제외 근거

| 구성 | 근거와 판단 | 반영 |
| --- | --- | --- |
| Java21/Boot4.1.1/JPA | 수업 본문에 실제 버전 명시 | 유지 |
| MVC/RestClient | LLM 수업의 동기 호출 방식 | 유지. WebFlux/새 SDK 없음 |
| Bearer JWT/STATELESS | 기본 Security 수업 방식과 사용자 설명 | 유지. API/FS쿠키 문구는 수정 대상 |
| BCrypt/JJWT0.13 | 수업 구현 | 유지 |
| 환경변수/.env import | 수업에서 import와 gitignore 설명 | local만 import, example만 공유 |
| 전역 예외/보안401·403 | 수업에서 역할을 나누는 클래스 구조 | 기존 역할 유지, 응답 계약만 통일 |
| Request ID/MDC | 모니터링 수업의 필터·finally 정리 | 보존하여 common으로 이동 |
| 가상 스레드/백그라운드 | 수업의 Boot applicationTaskExecutor 재사용 | virtual.enabled 유지. 새 executor 없음 |
| SSE/WebSocket | 실제 수업 항목이나 현재 API는 일괄 응답 | 미사용 의존성과 SSE 메서드 제거 |
| Clock | 세영 원본에 있었으나 현재 소비처 없음 | 제거. 한국 시간 설정은 유지 |
| 캐시 삭제 작업 | 실제 캐시 사용처 없음. DB synced_at과 다른 정책 | 선행 작업 제거, 기능 구현 시 정책 적용 |
| 외부 동시 호출 제한 | FS 요구이며 Java 표준 Semaphore 소규모 구현 | 새 라이브러리 없이 제한. 정확한 Semaphore 수업 구현은 미확인 |
| Web Push | API 요구. 확보 수업에서 구체 구현 근거 미확인 | 저장 테이블만 준비. 발송 라이브러리/서비스는 미구현 |
| Qdrant | 사용자가 허용한 예외이며 직접 추가한 구성 | 기존 설정/Compose 그대로 보존 |

@Async가 없다는 사실만으로 비동기가 불가능하다고 판단하면 안 됩니다. 수업처럼 Boot 실행기의
execute()로도 처리할 수 있습니다. 현재 사용하지 않는 @EnableAsync나 별도 큐를 미리 추가하지
않았습니다. 미래에 @Async를 선택하면 활성화·다른 빈 호출·트랜잭션 경계까지 맞춰야 합니다.

## 4. ERD와 SQL: 테이블·컬럼·제약

### 개수 차이의 정확한 의미

| 기준 | MySQL 구성 | Qdrant |
| --- | --- | --- |
| 원본 두 프로젝트/기존 develop | 동일10개 | 별도 사용자 추가 |
| 사용자가 첨부한 ERD 이미지 | 기존10+photo/ocr_candidate/dur_warning/dur_interaction=14 | vector_point |
| 노션 ERD v4 | terms+consent_history 대신 consent, push_subscription 포함=14 | vector_point |
| 이번 최소 변경 작업안 | 기존10보존+누락4+API푸시구독1=15 | 기존 연결 보존 |

따라서 이전의 “14개가 필요”는 첨부 이미지대로 구현할 때의 수치입니다. 전체 API를 유지하면서
기존 약관 테이블도 보존하면15개입니다. 숫자를 고정하려고 관계없는 데이터를 합치지 않습니다.

현재 15개: member, terms, profile, consent_history, notification_setting, drug, my_drug,
chat_session, chat_message, chat_summary, photo, ocr_candidate, dur_warning, dur_interaction,
push_subscription. MySQL에 vector_point나 별도 VECTOR 컬럼을 추가하지 않습니다.

### 실제 기능을 막는 차이와 반영

| 항목 | 이전 | 반영/이유 |
| --- | --- | --- |
| 이메일 |50자 |API100자 수용 |
| 임신 여부 |BOOLEAN NOTNULL DEFAULT FALSE |nullable BOOLEAN. YES/NO/미입력 의미 보존 |
| 프로필 색 |없음 |CHAR7 한 컬럼. API #RRGGBB 배정값 저장 |
| 기본 약 속성 |낱알 속성까지 NOTNULL |공식 속성 없는 약도 저장하도록 선택필드 nullable |
| 약 상세 재구성 |선택 공식속성 일부 없음 |필요한 main_ingredient/appearance/앞뒤색/크기/취하상태 등 추가 |
| DUR 조회 상태 |미조회·0건 구분 불가 |dur_synced_at으로 성공0건과 미조회/실패 구분 |
| 일정 없는 약 등록 |일정·수량 필수 |NULL 허용. 일정 삭제 시 날짜·시각만 비우는 흐름도 지원 |
| 같은 약 중복 |DB중복허용 |UNIQUE(profile_id,item_seq) |
| 빈 대화방 |출생년도/성별 사본 필수 |첫 질문 전 NULL 허용 |
| 한 턴 질문/답변 |UNIQUE(session_id,turn_no) |UNIQUE(session_id,turn_no,role) |
| 요약 중복 |종료턴만 달라도 저장 |UNIQUE(session_id,from_turn) |
| 사진 종류 |그림의 VARCHAR10은 PRESCRIPTION12자 부족 |VARCHAR20 |
| 앞/뒤 사진 |쌍 연결 없음 |뒷사진 front_photo_id로 연결 |
| 후보 약 |공식약 참조 불충분 |nullable item_seq와 순위/조회상태 저장 |
| 푸시 수신정보 |push_enabled만 존재 |endpoint/p256dh/auth/회원 소속 저장용 구독 테이블 |

성별 MALE/FEMALE/UNKNOWN, 임신 BOOLEAN, color/colorFront 등은 DTO 매핑으로 통일합니다.
모든 DB 이름을 API JSON 이름으로 개명하지 않습니다. 기존 키·몸무게 컬럼은 사용하지 않더라도
데이터 손실 DROP을 하지 않습니다. etc_otc는 여러 약이 공유하므로 ERD의 UNIQUE 표기를
그대로 적용하면 안 됩니다. 이미지보다 긴 문자열을 근거 없이 줄이지 않았습니다.

### 빈 테이블과 더미·공식 API

공공데이터 호출 자체에 우리 DB 테이블이 필수인 것은 아닙니다. 응답을 저장·재사용하고
소유권·후보 선택·재조회를 구현하기로 한 서비스에는 해당 저장 구조가 필요합니다.
photo/ocr_candidate/DUR/push_subscription의 초기 행은 필수가 아닙니다.
임시 데이터라도 실제 처리 중 저장한다면 테이블은 먼저 있어야 합니다.

기존 생성 대상10개와 seed INSERT 대상10개의 집합은 같습니다. 이것만으로 원저자의 의도를
“더미가 필요한 것만 생성”으로 단정하지 않습니다. 현재 샘플 품목 코드202600001~202600009와
약명/업체 조합의 공식 일치 여부는 검증되지 않았습니다. DB item_seq, 외부 ITEM_SEQ, JSON itemSeq의
철자가 같을 필요는 없으나 호출에 사용하는 값과 외부 파라미터 이름은 공식 계약을 따라야 합니다.
맞지 않는 코드는0건이나 다른 약을 돌려줄 수도 있어 항상 HTTP 오류라고 표현하지 않습니다.
공식 품목 코드 검증은 스키마 변경과 별도로 진행할 수 있습니다.

### 신규 DB와 기존 DB

01은 신규 스키마 생성이며 IF NOT EXISTS는 기존 정의를 바꾸지 않습니다.
03은 aa15bf7 원본10개 전용1회 업그레이드입니다. 백업과 작업 중단 후 명시적으로 실행하며
중복/채팅 패턴을 사전검사합니다. 실패하면 첫 영구 ALTER 전에 멈추며 자동 중복삭제는 없습니다.
이전에 메시지마다 증가하던 턴을 질문/답변 한 쌍 번호로 바꾸고 요약 범위도 함께 변환합니다.
패턴이 다른 실제 채팅은 추측하지 않습니다. MySQL DDL은 파일 전체 트랜잭션 롤백이 안 되므로
부분 실패 시 마지막 성공한 ALTER부터 확인해야 합니다. 자동DROP·Flyway는 추가하지 않았습니다.

## 5. 인증 계약: 적용 기준과 문서 정정

[AUTH-003](https://app.notion.com/p/20bbf16cc3ec8252af9f816a5ac5d54e),
[AUTH-004](https://app.notion.com/p/298bf16cc3ec82e89741019acdf31a05), API 명세는
ACCESS_TOKEN HttpOnly/Secure/SameSite=Lax 쿠키를 요구합니다. 해당 FS 상태는 확인 필요입니다.
수업 기본 구현과 기존 코드는 Authorization Bearer입니다. 둘 다 JWT/세션 미사용 인증입니다.

사용자가 토큰/서버세션 미사용을 설명한 뒤 “인증은 Bearer를 사용한다”고 최종 확인했습니다.
이번 브랜치는 Authorization 헤더의 JWT를 사용합니다.
기능명세와 API의 쿠키 문구는 수정 대상이며, 기존 팀 문서 자체는 이 작업에서 덮어쓰지 않습니다.
팀이 이후 실제로 쿠키 전달을 채택한다면 아래 관련 설정 전체를 함께 바꿔야 합니다.

- 공통: Access Token1시간 고정, Refresh Token/서버토큰저장/차단목록 없음.
- 적용한 Bearer 방식: 로그인 JSON으로 토큰 제공, 프런트가 헤더에 넣고 로그아웃 시 제거. 로그인 응답·저장위치 계약은 기능 담당과 맞춤.
- 쿠키 채택 시: 필터·CORS credentials·프런트 전송·CSRF·Swagger·로컬 HTTPS/secure 설정·로그아웃을 함께 변경.
- JWT/STATELESS라는 이유만으로 쿠키 인증의 CSRF 방어를 끄면 안 됨.
- 로그아웃은 복사된 토큰 자체를 만료시키지 않음. 차단목록을 없애고 즉시 폐기라고 설명하지 않음.
- 공개 경로는 GET consent-policies/email-availability, POST signup/login/logout 다섯개만 메서드별 허용.
- 실제 회원가입·로그인·로그아웃 Controller는 환경 PR에서 추가하지 않음.

## 6. 공통 응답·패키지·로그

common/global을 common으로 통합했습니다. 예외 계층은 원본의 역할을 유지하며,
정상은 업무 DTO 직접 반환, 오류는 code/message로 맞춥니다. 204 응답에는 본문을 붙이지 않습니다.
보안401/403도 같은 형식입니다. 외부 실패·시간초과는503이며 code로 구분합니다.
파일 용량 초과는400 FILE_TOO_LARGE입니다. 미분류 DB 제약 오류는500이며 원문을 숨깁니다.
예상한 이메일/약 중복은 담당 서비스가 문서의 업무 코드를 명시적으로 처리합니다.

삭제: 중복 PageResponse, 무사용 Clock, ResponseMessage, 캐시 삭제 작업,
URI별 공통 중복 인터셉터, 미사용 WebSocket 의존성과 SSE 메서드, 미사용 TokenBlacklist.
보존: common의 PageResponse, 원본 GlobalExceptionHandler 역할, RequestIdFilter/MDC,
BaseTimeEntity/JPA감사, TimeZoneConfig, 필수 보안 필터·CORS 검증.

사진은 회원 단위, 채팅은 방 단위로 중복을 막아야 하므로 URL별 락을 그대로 제공하지 않습니다.
해당 서비스에서 실제 처리 범위를 묶도록 문서화했습니다. 미사용 만능 락/새 프레임워크를
미리 구현하지 않았습니다. 예외의 타입·안전한 발생 위치·requestId와 외부 status/소요시간을
기록하되 외부 응답·프롬프트·키·건강정보·SQL 원문은 로그에 통째로 남기지 않습니다.

## 7. 외부 호출·모델·파일

| 항목 | 작업안 설정 |
| --- | --- |
| 연결 |2초 |
| 공공데이터 읽기 |5초 |
| OpenAI 분류/임베딩 읽기 |10초 |
| OpenAI 비전/답변/요약 읽기 |30초 |
| 재시도 |공공데이터/임베딩의 I/O·408·429·5xx만500ms 뒤1회 |
| 생성 요청/업무 오류/형식 오류 |자동재시도 안 함 |
| 외부 동시호출 |클라이언트별 기본4개, permit대기1초. 팀 확정값이 아닌 조정 가능한 초기값 |
| 전체90초 |여러 호출을 합친 담당 서비스 deadline 구현은 아직 없음 |

기존 OpenAiClient 안에 작업별 RestClient 두 개를 둡니다. 별도 provider 계층이나 재시도 라이브러리는
추가하지 않습니다. permit은 finally 반납하고 interrupt 상태를 복구합니다.
OpenAI 모델ID는 설계 미정이므로 임의의 고정 생성모델을 보장하지 않습니다. 개인 환경변수에
계정에서 쓸 모델을 명시하고, 해당 모델이나 키가 없으면 호출 전에503입니다.
text-embedding-3-small 기본1536차원과 Qdrant 차원은 실제 사용 시 함께 맞춰야 합니다.

업로드는 파일10MB/요청21MB입니다. 현재 FileStorage는 확장자·Content-Type 검사와 IO 도구이며
실제 내용검사/긴변2048px/JPEG85/EXIF제거를 완료한 것은 아닙니다. WebP 디코더 등 새 의존성을
설정 정리만을 위해 추가하지 않았습니다. 소유권·후보선택검증·행/파일 동시정리·30분만료는
이미지 담당 기능입니다. 공개 uploads URL 매핑은 추가하지 않습니다.

## 8. 기능 담당에 남기는 사항과 문서 충돌

| 항목 | 남은 처리 |
| --- | --- |
| 인증 문서 정정 |Bearer JWT 적용. API/FS의 ACCESS_TOKEN 쿠키·Set-Cookie·로그아웃 쿠키삭제·쿠키 기반 보안 문구를 헤더 계약으로 수정 필요 |
| 성별/임신/약관 DTO |기존 DB 표현과 API 상태 매핑, 동의 이력은 recorded_at+PK로 최신 판단 |
| DUR 빈 결과 |dur_synced_at 없는 상태를 금기 없음으로 판정하지 않음 |
| 임신 미입력 경고 |API는 문구 없음, FS CHAT008은 일반주의 문구 있음. 기능 결정이며 FS우선 원칙 확인 |
| 그만먹기/일정 삭제 |오늘 종료 시 오늘 남은 알림 여부, 날짜만 삭제 후 수량보존은 확인필요 기본값 |
| 프로필 삭제 |하위 행/FK·파일·Qdrant 정리를 서비스 트랜잭션/후처리로 구현 |
| 사진/후보 |회원/프로필·앞뒤 소유자일치, 식별결과선택, 실패/취소/등록/만료 삭제 |
| 웹 푸시 |VAPID·구독API·브라우저ServiceWorker·발송·404/410정리·정시알림 미구현 |
| 로그아웃 후 푸시 |기존 기본값은 구독유지. 계정전환/알림정책을 기능 담당과 확인 |
| 채팅 |분류·공공근거·전체 deadline·커밋후요약·MySQL fallback·Qdrant3초 실제연결 |
| OpenAI/공공데이터 |실제 모델 접근권한·품목코드·공식 endpoint/필드 검증 별도 |
| 보류 기능 |복약완료/미복용/날짜별기록/100일삭제를 복구하지 않음 |
| 설계 연결 |요구사항페이지의옛PRD링크, 미완성추출표/용어집, 접근불가DDD/화면 별도보완 |

기본키/FK만으로 회원이 그 프로필의 주인인지 자동 확인되지는 않습니다.
로그인 여부는 필터, 소유권은 서비스 조회로 검증합니다. SQL이 실제 파일을 지우지는 않습니다.

## 9. 실행한 검증과 한계

Java21 + 실제 MySQL8.4.11에서 `test bootJar` 통과: **31개, 실패0, 오류0, 생략0**.

- 보안/MVC12개: 토큰서명·만료·권한·세션미생성·CORS·요청ID·입력검증·내부오류은닉.
- 인증 계약4개: 공개5개의메서드제한, invalid토큰로그아웃, 외부503·원문차단, 파일용량400.
- 외부 호출12개: 로컬HTTP로 서비스키/한글인코딩·재시도횟수·비재시도·임베딩·작업별timeout·동시상한·interrupt·미설정모델 검증.
- 실제앱2개: health/Swagger/토큰보안,15테이블/seed/재실행/FK 검증.
- 실제DB계약1개 안 여러시나리오: 일정없는약·중복방지·빈방·같은턴QA·요약·사진쌍·빈후보·DUR상대코드·푸시endpoint 검증.

별도 MySQL에서 원본10+seed를03으로전환하여 회원5·메시지180·요약45행 보존,
메시지90쌍/요약범위변환, 중복/잘못된legacy순서사전거절,01재실행보존,03재실행거절을 확인했습니다.
임시 DB에서만 실행했고 실제 개발 DB를 변경하지 않았습니다.

실제 외부 API키/OpenAI모델/공식품목코드/Qdrant CRUD/웹푸시전송은 검증하지 않았습니다.
31개 성공은 환경·공통계약의 검증이며 미구현 업무기능이나 쿠키인증의 성공을 뜻하지 않습니다.
이전15개는 기존10테이블/Bearer/옛오류코드만 검증했으므로 새설계정합성의근거로사용하지않았습니다.

## 10. 근거 링크

- [팀 프로젝트](https://app.notion.com/p/3fcbf16cc3ec80b2b9c1ef0665cc9c57)
- [Architecture](https://app.notion.com/p/638bf16cc3ec82e68c2001084709c895)
- [PRD ver4](https://app.notion.com/p/b55bf16cc3ec824ca7db01055fbcb3bd)
- [Functional Spec ver4](https://app.notion.com/p/526bf16cc3ec82d5b7b8815b16de3754)
- [User Flow Map ver4](https://app.notion.com/p/1cebf16cc3ec83b79c740196a73f3d41)
- [API](https://app.notion.com/p/b80bf16cc3ec82a1924c01a553200c88)
- [ERD](https://app.notion.com/p/7cbbf16cc3ec8288ba6c811496e94e07)
- [수업 자료](https://app.notion.com/p/3fcbf16cc3ec803f84b3fe80ef520618)
- [수업 JWT](https://app.notion.com/p/4e9bf16cc3ec824e959701da86a48f82)
- [수업 JWT 구현](https://app.notion.com/p/f59bf16cc3ec8388ab27016828100e72)
- [수업 쿠키 심화](https://app.notion.com/p/860bf16cc3ec8390912101b9e6c1b4ea)
- [수업 JPA/Boot 버전](https://app.notion.com/p/9e0bf16cc3ec82f6b1ee01299febeb5d)
- [수업 LLM/RestClient/env](https://app.notion.com/p/cb3bf16cc3ec826db7ec8184da08be63)
- [수업 Boot TaskExecutor](https://app.notion.com/p/a7cbf16cc3ec829fa5850144818fcf06)
- [수업 외부 장애 대응](https://app.notion.com/p/b78bf16cc3ec82ca845d0175777ee7f0)
- [수업 요청 추적/MDC](https://app.notion.com/p/03cbf16cc3ec830db6800187ebff23a2)
