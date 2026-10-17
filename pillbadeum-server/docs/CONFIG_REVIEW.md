# 환경설정 전면 재검토와 최소 재구성

검토일: 2026-10-17 (KST). 코드 기준: 팀 develop aa15bf7에서 분기한 개인 pill-config.
노션 검토 문서: https://app.notion.com/p/3fcbf16cc3ec81dab02decb3d435618c

## 1. 검토 결론 및 반영 범위
- 기존 업무 패키지 6개(member, profile, drug, image, chat, medication)와 추가된 Qdrant 연결 설정을 보존했습니다.
- 인증은 확인된 Bearer JWT 방식으로 적용했습니다. API 명세와 Functional Spec의 쿠키 관련 문구는 현재 적용 기준과 달라 정정이 필요합니다. 토큰 인증의 종류와 토큰을 전달하는 방법은 구분합니다.
## 2. 확인한 사실
- 두 원본 프로젝트와 기존 develop의 생성 SQL에는 동일한 MySQL 테이블 10개가 정의되어 있습니다.
- 첨부 ERD 이미지의 관계형 테이블은 14개이며, 그중 photo, ocr_candidate, dur_warning, dur_interaction 4개가 기존 SQL에 없습니다. vector_point는 Qdrant에 해당합니다.
- 첨부 이미지와 노션 ERD v4는 테이블 수가 같아도 구성이 다릅니다. 노션 ERD는 약관 테이블 구성과 푸시 구독 저장 구조가 다릅니다.
- 기존 SQL의 일부 NULL·UNIQUE 제약은 일정 없는 약 등록, 빈 대화방 생성, 같은 턴의 질문·답변 저장 흐름과 충돌합니다.
- 인증은 Bearer JWT로 확인됐으며, API 명세와 Functional Spec에는 쿠키 전달 방식이 남아 있습니다.
- Java·Spring·JPA·RestClient·JWT의 기본 구성은 실제 수업 소스에서 확인했습니다. 프로젝트 명세에 맞춘 사진 식별·임베딩·동시 호출 제한은 별도 보완 항목으로 구분했습니다.
### 검토 자료 및 확인 범위
| 자료 | 확인 범위와 우선순위 |
| --- | --- |
| 팀 프로젝트 루트 | 직접 설계 링크 전체 확인 |
| Architecture ver.4 | PRD 42행·FS 50행·UFM 15행과 본문 총107개 확인 |
| API 명세 | 전체 확인. 문서 자체가 충돌 시 FS ver.4 우선이라고 명시 |
| 첨부 ERD 이미지 | MySQL14개+Qdrant1개. 노션 ERD와 별도로 대조 |
| 노션 ERD v4 | 본문·첨부 SVG 전체 텍스트 확인. 이미지와 같은 판이 아님 |
| 요구사항 페이지 | 연결된 것은 과거 PRD28행. 최신 상세 기준으로 오인하지 않음 |
| 화면정의/DDD/용어집/API추출표 | 텍스트와 접근 가능한 자료 확인. 아래 제한 별도 기재 |
| 수업 자료 | 노션 강의 본문 30개와 전달받은 학원 소스 9개 폴더의 관련 구현·빌드 설정 대조 |
| 원본 프로젝트 두 개 | 메일 ZIP의 두 원본 SQL 모두 같은10개 테이블인 사실 확인 |
| 최신 develop | 작업 시작 때 aa15bf7을 다시 fetch하고 별도 worktree에서 작업 |
Functional Spec 50개 항목의 상태는 확정 8개, 확인 필요 24개, 제안 13개, 보류 5개입니다. 문서에 기재된 기본값 중에는 팀 확인이 남은 사항이 있습니다.
### 추가 확인이 필요한 설계 자료
수업 범위는 노션 강의 본문 30개와 전달받은 학원 소스 9개 폴더를 기준으로 대조했습니다. 업로드된 소스는 총 1,743개 파일이며, 각 폴더의 빌드 설정과 현재 프로젝트에 해당하는 구현을 확인했습니다.
DDD의 Miro 임베드와 외부 화면 링크 3개의 실제 화면도 확인하지 못했습니다. 외부 화면 링크에서는 화면 내용이 없는 웹앱 기본 페이지만 응답했습니다. 용어집은 비어 있고, API 추출표에는 e약은요 필드 5개만 기재되어 있습니다. 공식 API의 전체 필드·파라미터 검수는 별도로 필요합니다.
## 3. 수업 내용과 설정 근거
### 실제 수업 소스 대조 결과
| 항목 | 실제 소스에서 확인한 내용 | 현재 적용 |
| --- | --- | --- |
| 기본 스택 | JPA 최종 예제와 실시간 통신 phase-4에 Boot 4.1.1·Java 21·MVC·JPA·MySQL 구현 | 기존 버전과 구조 유지 |
| Bearer JWT | 보안 phase-2의 STATELESS·CSRF 비활성·Bearer 필터, 프런트의 Authorization 헤더 전송 | 확인된 Bearer 방식 유지 |
| Refresh Token | 보안 HOWTO 부록 A에 DB 저장·재발급·회전·HttpOnly 쿠키를 선택 확장으로 설명. 기본 완성 앱은 Access Token만 사용 | 현재 명세의 1시간 만료·재로그인 정책에 따라 미도입 |
| 비동기·가상 스레드 | 실시간 통신 예제에서 Boot applicationTaskExecutor.execute()를 실제 사용 | 별도 실행기·외부 메시지 큐 없이 구현하는 기준 유지 |
| 외부 API | JDK HttpClient·RestClient·Responses·구조화 출력·연결/읽기 시간제한 구현 | 동일 방식으로 프로젝트의 사진 식별·임베딩·작업별 시간제한 보완 |
| 요청 추적·전역 예외 | 모니터링 예제에 요청 ID·MDC·finally 정리, 보안 예제에 전역 예외와 보안 401·403 처리 분리 | 역할을 보존하고 응답 형식은 프로젝트 API에 맞춤 |
| 수동 DDL | JPA 자료에서 실습용 create와 운영 none/validate·별도 변경 관리를 구분 | 신규 생성 SQL과 기존 DB 변경 SQL 분리 유지 |
| 프로젝트용 보완 | Semaphore·한정 재시도·MDC 작업 전달·JPA 감사·한국 시간 설정은 수업 예제와 같은 구현을 복사한 항목이 아님 | 명세와 데이터 처리에 필요한 Spring·Java 기능만 유지 |

수업 소스와 대조한 결과 기본 인증·DB·외부 호출 구조를 전면 변경할 문제는 발견되지 않았습니다. Refresh Token·메시지 큐 등 추가 구성은 도입하지 않는 현재 방향을 유지합니다. 수업에서 소개된 기술인지와 이번 프로젝트에 필요한 기술인지는 구분합니다.

수업 프런트의 오류 파서는 description/detail/result를 읽으므로 그대로 복사하면 현재 code/message·직접 DTO 응답과 맞지 않습니다. 프런트 연결 시 현재 API 응답 형식으로 파서를 맞춰야 합니다.

### 구성별 유지·정리 근거
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
| 외부 동시 호출 제한 | FS 요구이며 Java 표준 Semaphore 소규모 구현 | 프로젝트 명세를 반영한 Java 표준 코드 보완. 별도 라이브러리 없음 |
| Web Push | API 요구. 확보 수업에서 구체 구현 근거 미확인 | 저장 테이블만 준비. 발송 라이브러리/서비스는 미구현 |
| Qdrant | 사용자가 허용한 예외이며 직접 추가한 구성 | 기존 설정/Compose 그대로 보존 |
백그라운드 처리는 수업에서 사용한 Boot의 applicationTaskExecutor.execute()로 구현할 수 있습니다. 현재 사용처가 없는 @EnableAsync나 별도 작업 큐는 추가하지 않았습니다. 이후 @Async를 사용할 경우 활성화 설정, 다른 빈을 통한 호출, 트랜잭션 경계를 함께 적용해야 합니다.
Qdrant는 허용된 예외로 보존했습니다. Semaphore 동시 호출 제한은 설계 요구에 따라 추가한 Java 표준 코드이며, 실제 수업 예제에는 같은 Semaphore 구현이 없습니다. 프로젝트 명세의 외부 호출 제한을 Java 표준 기능으로 보완한 부분입니다. 웹 푸시는 구독 테이블만 준비했으며 발송 기능과 라이브러리는 추가하지 않았습니다.
### 추가 기술의 미도입 근거
아래 항목은 수업에 없는 기술이라는 이유로 일괄 제외한 목록이 아닙니다. 명세에 정해진 정책과 현재 구조에서의 필요성을 구분해 판단했습니다.
| 항목 | 현재 미도입 근거 | 재검토 조건 |
| --- | --- | --- |
| Redis | 현재 구조에는 서버 간 공유 세션·캐시가 필수인 요구가 없습니다. 저장·재사용할 약·DUR 정보는 MySQL 구조를 준비했습니다. | 다중 서버의 공유 상태나 캐시 성능 문제가 확인될 때 검토합니다. |
| WebFlux | 확인된 수업 구성은 Spring MVC·RestClient·가상 스레드입니다. 현재 API는 일괄 응답이며 리액티브 전환 요구는 확인되지 않았습니다. | 비동기 입출력·배압 등 구체적인 요구가 생기면 검토합니다. |
| Refresh Token | 현재 API·Functional Spec은 액세스 토큰 1시간, 리프레시 토큰 없음, 만료 후 재로그인을 명시합니다. 수업 기본 완성 예제도 같은 정책이며, Refresh Token은 보안 HOWTO의 선택 확장 부록에 설명되어 있습니다. | 로그인 유지 정책을 바꾸면 토큰 보관·재발급·폐기 정책을 함께 설계합니다. Redis가 반드시 필요한 것은 아닙니다. |
| 메시지 큐 | 현재 확인한 요구에는 내구성 있는 큐나 여러 서버의 작업 분산이 명시되어 있지 않습니다. 단일 서버의 요약·DUR 후처리는 수업의 Boot 작업 실행기로 구현할 수 있어 기본 설정에 큐를 추가하지 않았습니다. | 서버 재시작 이후 작업 복구·유실 방지·분산 처리가 필요하면 내구성 있는 작업 처리 구조를 검토합니다. 메모리 실행기는 이런 보장을 제공하지 않습니다. |
| MSA | 현재 구현은 업무 패키지를 나눈 단일 Spring 서버 기준입니다. 서비스별 독립 배포·확장 요구는 확인되지 않았습니다. | 독립 배포·확장·장애 격리의 구체적인 필요가 확인될 때 검토합니다. |
| Flyway | 현재 프로젝트는 수동 SQL 관리 기준이며 신규 생성 SQL과 명시적 변경 SQL로 적용 경로를 준비했습니다. | 스키마 변경 빈도와 환경 수가 늘어나 적용 이력·순서 관리가 어려워지면 검토합니다. |
| Resilience4j | 수업에서 장애 대응 개념은 소개하지만 수업 프로젝트에는 도입하지 않은 것으로 확인했습니다. 현재 필요한 시간제한·제한적 재시도·호출 상한은 기존 클라이언트에서 처리했습니다. | 회로 차단기 등 추가 장애 제어가 실제로 필요해지면 검토합니다. |
메시지 큐·Redis·MSA 등의 미도입은 현재 프로젝트 구조와 요구사항을 기준으로 판단했습니다.
## 4. ERD·SQL 대조 및 반영 결과
### 테이블 구성과 개수
| 기준 | MySQL 구성 | Qdrant |
| --- | --- | --- |
| 원본 두 프로젝트/기존 develop | 동일10개 | 별도 사용자 추가 |
| 사용자가 첨부한 ERD 이미지 | 기존10+photo/ocr_candidate/dur_warning/dur_interaction=14 | vector_point |
| 노션 ERD v4 | terms+consent_history 대신 consent, push_subscription 포함=14 | vector_point |
| 이번 최소 변경 작업안 | 기존10보존+누락4+API푸시구독1=15 | 기존 연결 보존 |
첨부 ERD 이미지의 MySQL 테이블은 14개입니다. 이번 구성은 기존 약관 테이블을 보존하고 API의 웹 푸시 구독 저장소를 추가하여 15개로 정리했습니다.
현재 테이블은 member, terms, profile, consent_history, notification_setting, drug, my_drug, chat_session, chat_message, chat_summary, photo, ocr_candidate, dur_warning, dur_interaction, push_subscription입니다. vector_point는 Qdrant 포인트로 관리하며 MySQL 테이블이나 VECTOR 컬럼으로 추가하지 않았습니다.
### 컬럼·제약 차이와 수정 내용
| 항목 | 이전 | 반영/이유 |
| --- | --- | --- |
| 이메일 | 50자 | API100자 수용 |
| 임신 여부 | BOOLEAN NOTNULL DEFAULT FALSE | nullable BOOLEAN. YES/NO/미입력 의미 보존 |
| 프로필 색 | 없음 | CHAR7 한 컬럼. API #RRGGBB 배정값 저장 |
| 기본 약 속성 | 낱알 속성까지 NOTNULL | 공식 속성 없는 약도 저장하도록 선택필드 nullable |
| 약 상세 재구성 | 선택 공식속성 일부 없음 | 필요한 main_ingredient/appearance/앞뒤색/크기/취하상태 등 추가 |
| DUR 조회 상태 | 미조회·0건 구분 불가 | dur_synced_at으로 성공0건과 미조회/실패 구분 |
| 일정 없는 약 등록 | 일정·수량 필수 | NULL 허용. 일정 삭제 시 날짜·시각만 비우는 흐름도 지원 |
| 같은 약 중복 | DB중복허용 | UNIQUE(profile_id,item_seq) |
| 빈 대화방 | 출생년도/성별 사본 필수 | 첫 질문 전 NULL 허용 |
| 한 턴 질문/답변 | UNIQUE(session_id,turn_no) | UNIQUE(session_id,turn_no,role) |
| 요약 중복 | 종료턴만 달라도 저장 | UNIQUE(session_id,from_turn) |
| 사진 종류 | 그림의 VARCHAR10은 PRESCRIPTION12자 부족 | VARCHAR20 |
| 앞/뒤 사진 | 쌍 연결 없음 | 뒷사진 front_photo_id로 연결 |
| 후보 약 | 공식약 참조 불충분 | nullable item_seq와 순위/조회상태 저장 |
| 푸시 수신정보 | push_enabled만 존재 | endpoint/p256dh/auth/회원 소속 저장용 구독 테이블 |
성별(MALE/FEMALE/UNKNOWN), 임신 여부(BOOLEAN), 앞면 색상(color/colorFront)은 기존 DB 표현을 보존하고 DTO에서 API 값으로 매핑합니다. 키·몸무게 컬럼도 기존 데이터 보존을 위해 유지했습니다.
drug.etc_otc는 여러 약이 공유하는 분류이므로 UNIQUE를 적용하지 않았습니다. 문자열 길이는 첨부 이미지와 다르다는 이유만으로 일괄 축소하지 않았습니다.
### 테이블·샘플 데이터·공공데이터의 구분
외부 API 호출 자체에는 우리 DB 테이블이 필수는 아닙니다. 프로젝트에서 응답을 저장·재사용하거나 사진 소유권, 후보 선택, 재조회를 관리하는 기능에는 해당 저장 구조가 필요합니다.
photo, ocr_candidate, DUR, push_subscription의 초기 샘플 데이터는 필수가 아닙니다. 임시 데이터도 실제 처리 과정에서 저장한다면 테이블 구조를 먼저 준비해야 합니다.
기존 생성 SQL의 대상 10개와 샘플 INSERT 대상 10개의 집합은 같습니다. 이 사실만으로 원본 작성자의 생성 의도까지 판단할 수는 없습니다.
샘플 품목 코드 202600001\~202600009와 약명·업체 조합의 공식 일치 여부는 검증되지 않았습니다. DB의 item_seq, 외부 API의 ITEM_SEQ, JSON의 itemSeq는 매핑할 수 있으나, 실제 조회에 사용하는 코드 값과 요청 파라미터는 공식 API 규칙을 따라야 합니다. 코드가 맞지 않으면 조회 결과가 없거나 다른 약이 반환될 수 있습니다. 공식 품목 코드 검증과 스키마 정리는 별도로 진행할 수 있습니다.
### 신규 생성 및 기존 DB 변경 절차
01 생성 SQL은 신규 스키마용입니다. CREATE TABLE IF NOT EXISTS를 다시 실행해도 기존 컬럼과 제약은 변경되지 않습니다.
03 변경 SQL은 기준 커밋 aa15bf7의 원본 10개 테이블을 대상으로 하는 1회 업그레이드입니다. 백업 후 DB에 쓰는 앱과 작업을 중단한 상태에서 실행하며, 중복 데이터와 기존 채팅 순서를 먼저 검사합니다. 사전검사 실패 시 첫 영구 ALTER 전에 중단하고 데이터를 자동 삭제·병합하지 않습니다.
기존 채팅의 메시지별 번호는 질문·답변 한 쌍의 턴 번호로 변환하며 요약 범위도 함께 맞춥니다. 원본 패턴과 다른 데이터는 자동 변환하지 않습니다. MySQL DDL은 파일 전체를 한 번에 롤백할 수 없으므로 부분 실패 시 마지막으로 성공한 ALTER와 현재 정의를 확인해야 합니다. 자동 DROP과 Flyway는 추가하지 않았습니다.
## 5. 인증 설정 및 문서 정정 사항
인증은 Authorization 헤더로 JWT를 전달하는 Bearer 방식으로 확인됐으며, 현재 코드도 해당 방식으로 적용했습니다. 리프레시 토큰은 미도입으로 확인했습니다. 서버 세션은 사용하지 않습니다.
[AUTH-003 로그인](https://app.notion.com/p/20bbf16cc3ec8252af9f816a5ac5d54e)
[AUTH-004 로그아웃](https://app.notion.com/p/298bf16cc3ec82e89741019acdf31a05)
위 로그인·로그아웃 기능명세와 API 명세에는 ACCESS_TOKEN을 HttpOnly·Secure·SameSite=Lax 쿠키로 전달하는 내용이 남아 있습니다. 해당 Functional Spec의 상태는 ‘확인 필요’입니다. 쿠키 전달도 JWT 인증에 사용할 수 있지만, 현재 적용한 Bearer 헤더 방식과는 다른 계약입니다.
- 토큰 유효기간은 발급 후 1시간으로 고정합니다. Refresh Token, 서버 토큰 저장소, 차단목록은 두지 않습니다.
- 로그인 API는 토큰을 JSON 응답으로 제공하고, 프런트는 이후 요청의 Authorization 헤더에 포함합니다. 응답 필드와 프런트 보관 방식은 로그인 기능 구현 시 맞춰야 합니다.
- 로그아웃은 프런트에서 토큰을 제거하는 방식입니다. 이미 복사된 토큰은 원래 만료 시각까지 유효합니다.
- 공개 경로는 GET consent-policies·email-availability와 POST signup·login·logout 5개를 HTTP 메서드별로 허용합니다.
- 회원가입·로그인·로그아웃 Controller 구현은 기능 담당 범위입니다.
API·Functional Spec의 쿠키 저장, Set-Cookie, 쿠키 삭제 관련 문구는 Bearer 계약에 맞게 정정해야 합니다. 이번 작업에서는 기존 팀 문서를 직접 변경하지 않고 정정 대상을 기록했습니다.
향후 쿠키 전달을 채택하면 인증 필터, CORS credentials, 프런트 요청, CSRF 방어, Swagger, 로컬 HTTPS·Secure 설정, 로그아웃을 함께 변경해야 합니다. JWT·STATELESS 여부만으로 쿠키 인증의 CSRF 방어 필요성이 사라지지는 않습니다.
## 6. 공통 코드 정리 결과
common과 global의 공통 코드를 common으로 통합했습니다. 기존 예외 계층의 역할은 유지하고 응답 형식을 정리했습니다.
정상 응답은 업무 DTO를 직접 반환하며, 오류 응답은 code와 message로 통일했습니다. 보안 필터의 401·403도 같은 형식을 사용합니다. 204 응답에는 본문을 포함하지 않습니다.
외부 호출 실패·시간초과는 503으로 응답하고 code로 구분합니다. 파일 용량 초과는 400 FILE_TOO_LARGE로 처리합니다. 원인을 분류하지 못한 DB 제약 오류는 내부 정보를 숨긴 500으로 처리하며, 예상 가능한 이메일·약 중복은 기능 서비스에서 해당 업무 코드로 처리해야 합니다.
- 제거: 중복 PageResponse, 사용처 없는 Clock·ResponseMessage·캐시 삭제 작업·URI별 중복 요청 인터셉터·WebSocket 의존성·SSE 메서드·TokenBlacklist.
- 유지: common의 PageResponse, 기존 GlobalExceptionHandler 역할, RequestIdFilter·MDC 전달, BaseTimeEntity·JPA 감사, TimeZoneConfig, 보안 필터·CORS 검증.
중복 요청 제어는 사진의 경우 회원 단위, 채팅의 경우 대화방 단위로 구현해야 합니다. 현재 요구 범위와 다른 URI별 공통 락은 제거하고 기능 서비스의 처리 범위에서 적용하도록 정리했습니다.
로그에는 예외 종류, 안전한 발생 위치, 요청 ID, 외부 응답 상태와 소요 시간을 기록합니다. 외부 응답 본문·프롬프트·키·건강정보·SQL 원문은 통째로 기록하지 않습니다.
## 7. 외부 API·모델·파일 설정
| 항목 | 작업안 설정 |
| --- | --- |
| 연결 | 2초 |
| 공공데이터 읽기 | 5초 |
| OpenAI 분류/임베딩 읽기 | 10초 |
| OpenAI 비전/답변/요약 읽기 | 30초 |
| 재시도 | 공공데이터/임베딩의 I/O·408·429·5xx만500ms 뒤1회 |
| 생성 요청/업무 오류/형식 오류 | 자동재시도 안 함 |
| 외부 동시호출 | 클라이언트별 기본4개, permit대기1초. 팀 확정값이 아닌 조정 가능한 초기값 |
| 전체90초 | 여러 호출을 합친 담당 서비스 deadline 구현은 아직 없음 |
기존 OpenAiClient 내부에서 작업별 RestClient 2개를 사용합니다. 별도 제공자 계층이나 재시도 라이브러리는 추가하지 않았습니다. 동시 호출 허가는 finally에서 반납하고 인터럽트 상태를 복구합니다.
생성 모델 ID는 설계상 미정이므로 개인 환경변수에 계정에서 사용할 수 있는 모델을 지정하도록 했습니다. 해당 작업의 모델이나 API 키가 없으면 외부 요청 전에 503으로 응답합니다. 기본 임베딩 모델인 text-embedding-3-small의 1,536차원과 Qdrant 컬렉션 차원은 실제 사용 시 일치시켜야 합니다.
업로드 제한은 파일당 10MB, 요청당 21MB입니다. 현재 FileStorage는 확장자·Content-Type 검사와 파일 입출력을 제공합니다. 실제 이미지 내용 검사, 긴 변 2,048px 조정, JPEG 품질 85 적용, EXIF 제거는 미구현입니다. WebP 디코더 등 새 의존성은 추가하지 않았습니다.
소유권·후보 선택 검증, 행·파일 정리, 30분 만료 처리는 이미지 기능 담당 범위입니다. 업로드 디렉터리를 공개 URL로 노출하는 설정은 추가하지 않았습니다.
## 8. 추가 구현 및 확인 사항
| 항목 | 남은 처리 |
| --- | --- |
| 인증 문서 정정 | Bearer JWT 적용. API/FS의 ACCESS_TOKEN 쿠키·Set-Cookie·로그아웃 쿠키삭제·쿠키 기반 보안 문구를 헤더 계약으로 수정 필요 |
| 성별/임신/약관 DTO | 기존 DB 표현과 API 상태 매핑, 동의 이력은 recorded_at+PK로 최신 판단 |
| DUR 빈 결과 | dur_synced_at 없는 상태를 금기 없음으로 판정하지 않음 |
| 임신 미입력 경고 | API는 문구 없음, FS CHAT008은 일반주의 문구 있음. 기능 결정이며 FS우선 원칙 확인 |
| 그만먹기/일정 삭제 | 오늘 종료 시 오늘 남은 알림 여부, 날짜만 삭제 후 수량보존은 확인필요 기본값 |
| 프로필 삭제 | 하위 행/FK·파일·Qdrant 정리를 서비스 트랜잭션/후처리로 구현 |
| 사진/후보 | 회원/프로필·앞뒤 소유자일치, 식별결과선택, 실패/취소/등록/만료 삭제 |
| 웹 푸시 | VAPID·구독API·브라우저ServiceWorker·발송·404/410정리·정시알림 미구현 |
| 로그아웃 후 푸시 | 기존 기본값은 구독유지. 계정전환/알림정책을 기능 담당과 확인 |
| 채팅 | 분류·공공근거·전체 deadline·커밋후요약·MySQL fallback·Qdrant3초 실제연결 |
| OpenAI/공공데이터 | 실제 모델 접근권한·품목코드·공식 endpoint/필드 검증 별도 |
| 보류 기능 | 복약완료/미복용/날짜별기록/100일삭제를 복구하지 않음 |
| 설계 연결 | 요구사항페이지의옛PRD링크, 미완성추출표/용어집, 접근불가DDD/화면 별도보완 |
로그인 여부는 보안 필터에서, 데이터 소유권은 서비스 조회에서 확인해야 합니다. 기본키와 외래키만으로 해당 회원이 프로필의 소유자인지까지 검증되지는 않습니다. 실제 파일과 Qdrant 데이터의 삭제도 SQL과 별도로 처리해야 합니다.
## 9. 검증 결과 및 제한 사항
Java 21과 실제 MySQL 8.4.11에서 테스트 및 실행 JAR 빌드를 완료했습니다. **테스트 31개 모두 통과했으며 실패·오류·생략은 없습니다.**
- 보안·MVC 12개: 토큰 서명·만료·권한, 세션 미생성, CORS, 요청 ID, 입력 검증, 내부 오류 정보 비노출.
- 인증 계약 4개: 공개 경로 5개의 메서드 제한, 유효하지 않은 토큰의 로그아웃, 외부 오류 503 및 원문 비노출, 파일 용량 오류 400.
- 외부 호출 12개: 로컬 HTTP 서버로 서비스 키·한글 인코딩, 재시도 횟수, 비재시도 대상, 임베딩, 작업별 시간제한, 동시 호출 상한, 인터럽트, 모델 미설정 처리.
- 실제 앱 2개: health·Swagger·토큰 보안, 테이블 15개·샘플·SQL 재실행·외래키.
- DB 계약 1개 내 여러 시나리오: 일정 없는 약 등록, 약 중복 방지, 빈 대화방, 같은 턴의 질문·답변, 요약, 사진 쌍, 빈 후보, DUR 상대 코드, 푸시 구독 주소.
별도 임시 MySQL에서 원본 10개 테이블과 샘플에 03 변경 SQL을 적용했습니다. 회원 5행·메시지 180행·요약 45행 보존, 질문·답변 90쌍의 턴 번호 및 요약 범위 변환, 중복·예상과 다른 채팅 순서의 사전 차단, 01 재실행 시 데이터 보존, 03 재실행 거절을 확인했습니다. 실제 개발 DB는 변경하지 않았습니다.
실제 외부 API 키, OpenAI 모델 사용권한, 공식 품목 코드, Qdrant 저장·검색·삭제, 웹 푸시 발송은 검증하지 않았습니다. 이번 테스트 성공은 환경설정과 공통 계약에 대한 결과이며 미구현 업무 기능이나 쿠키 인증의 동작을 보장하지 않습니다.
이전 테스트 15개는 기존 테이블 10개, Bearer 인증, 기존 오류 코드를 기준으로 했습니다. 당시 성공 결과와 이번 ERD·API 정합성 검증은 구분합니다.
## 10. 참고 자료
- [팀 프로젝트](https://app.notion.com/p/3fcbf16cc3ec80b2b9c1ef0665cc9c57)
- [Architecture](https://app.notion.com/p/638bf16cc3ec82e68c2001084709c895)
- [PRD ver.4](https://app.notion.com/p/b55bf16cc3ec824ca7db01055fbcb3bd)
- [Functional Spec ver.4](https://app.notion.com/p/526bf16cc3ec82d5b7b8815b16de3754)
- [User Flow Map ver.4](https://app.notion.com/p/1cebf16cc3ec83b79c740196a73f3d41)
- [API 명세](https://app.notion.com/p/b80bf16cc3ec82a1924c01a553200c88)
- [ERD](https://app.notion.com/p/7cbbf16cc3ec8288ba6c811496e94e07)
- [수업 자료](https://app.notion.com/p/3fcbf16cc3ec803f84b3fe80ef520618)
- [수업 JWT](https://app.notion.com/p/4e9bf16cc3ec824e959701da86a48f82)
- [수업 JWT 구현](https://app.notion.com/p/f59bf16cc3ec8388ab27016828100e72)
- [수업 쿠키 심화](https://app.notion.com/p/860bf16cc3ec8390912101b9e6c1b4ea)
- [수업 JPA·Boot 버전](https://app.notion.com/p/9e0bf16cc3ec82f6b1ee01299febeb5d)
- [수업 RestClient·환경변수](https://app.notion.com/p/cb3bf16cc3ec826db7ec8184da08be63)
- [수업 Boot 실행기](https://app.notion.com/p/a7cbf16cc3ec829fa5850144818fcf06)
- [수업 외부 장애 대응](https://app.notion.com/p/b78bf16cc3ec82ca845d0175777ee7f0)
- [수업 요청 추적](https://app.notion.com/p/03cbf16cc3ec830db6800187ebff23a2)
- [실제 수업 소스 대조 결과](https://github.com/kodonghui/pillboot-config/blob/pill-config/pillbadeum-server/docs/COURSE_SOURCE_REVIEW.md)
## 11. 업로드 결과 및 PR 진행 상태
- 업로드 완료: [개인 저장소의 환경설정 브랜치](https://github.com/kodonghui/pillboot-config/tree/pill-config).
- 기존 설정 커밋: [환경설정·공통 코드·DB 스키마 정리](https://github.com/kodonghui/pillboot-config/commit/de62aab7cd003a8a9e87305b97f2c8e80248f030). 팀 develop 기준 커밋에서 분기한 작업입니다.
- 검토 링크: [전체 변경 비교](https://github.com/kodonghui/pillboot-config/compare/aa15bf771616ce0c5faf581d2b6b5bad37b95af9...pill-config), [실행·설정 안내](https://github.com/kodonghui/pillboot-config/blob/pill-config/pillbadeum-server/README.md), [기존 DB 변경 절차](https://github.com/kodonghui/pillboot-config/blob/pill-config/pillbadeum-server/sql/README.md).
- 검증 완료: [GitHub CI](https://github.com/kodonghui/pillboot-config/actions/runs/39007518263)에서도 테스트와 빌드가 통과했습니다.
- 미완료: 팀 develop 대상 PR 생성. 실제 push·브랜치 생성·fork 요청이 연결 앱의 접근 권한 오류로 거절됐습니다.
추가 권한 확인 결과, 계정 자체의 Write 권한과 개인 계정에 설치된 앱의 코드·PR 쓰기 권한은 정상입니다. 현재 연결의 설치 목록에서는 팀 조직이 확인되지 않아 조직의 앱 설치·승인·저장소 선택을 확인해야 합니다. 조직에 앱이 없거나, 설치된 앱이 현재 연결에서 접근되지 않는 경우를 이 조회만으로 구분할 수는 없습니다.
코드, 검토 문서, PR 본문, 패치, 소스 ZIP, Git bundle은 준비되어 있습니다. 팀 저장소 접근이 해결되면 동일 커밋을 업로드하고 develop 대상 PR을 생성할 수 있습니다. 팀 저장소와 기존 개발 DB는 변경하지 않았습니다.
권한이 있는 팀 저장소의 로컬 체크아웃에서 아래 명령으로 적용할 수 있습니다. 같은 이름의 로컬 브랜치가 없는 경우의 예시입니다.
```bash
git fetch https://github.com/kodonghui/pillboot-config.git pill-config
git switch -c pill-config FETCH_HEAD
git push origin pill-config
gh pr create --repo semi-project-B-team/pillbadeum --base develop --head pill-config --web
```
PR 제목: 설정: 설계와 수업 기준으로 공통 환경과 DB 스키마 정리.
