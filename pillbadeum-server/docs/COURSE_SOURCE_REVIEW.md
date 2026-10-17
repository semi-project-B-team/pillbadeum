# 실제 학원 소스 대조 결과

검토일: 2026-10-17 (KST). 기준 자료: 전달받은 `saltlux-llm-agent-service-1st_sources.zip`.
소스 경로는 해당 압축파일 내부 기준입니다. 학원 소스 전체를 이 저장소에 복제하지 않습니다.

## 확인한 사실

요청한 수업 폴더 9개와 파일 1,743개를 확인했습니다. 각 폴더의 구성·빌드 설정을 조사하고,
현재 환경설정과 관련된 실제 구현·HOWTO·README를 대조했습니다.

| 수업 폴더 | 파일 수 | 대조 대상 |
| --- | --- | --- |
| 04_spring_framework | 113 | 설정 클래스·빈 등록·DI |
| 04_spring_boot | 213 | MVC·예외 처리·검증·파일 저장·REST 응답 |
| 04_spring_data_jpa | 149 | 버전·JPA·MySQL·페이징·DDL 정책 |
| 05_spring_monitoring | 57 | Actuator·요청 ID·MDC·민감정보 로그 |
| 06_js | 120 | 웹 요청·비동기·문법 예제 |
| 06_react | 135 | 상태·폼·인증 화면 예제 |
| 07_spring_security | 367 | JWT·BCrypt·CORS·CSRF·Principal·프런트 인증 |
| 07_sse_websocket | 517 | RestClient·Responses·가상 스레드·작업 실행기 |
| vibe-design-practice | 72 | UI·JPA 연동·수업 단계별 CORS 차이 |

기본 구성인 Java 21·Boot 4.1.1·MVC·JPA·MySQL·Bearer JWT·RestClient는 수업 소스로 확인했습니다.
수업 소스 때문에 환경설정을 전면 재작성하거나 버전을 변경할 문제는 발견되지 않았습니다.

## 유지하는 구성과 소스 근거

| 구성 | 소스 근거 | 적용 결과 |
| --- | --- | --- |
| Boot 4.1.1·Java 21 | `04_spring_data_jpa/chap06-spring-data-jpa/build.gradle:3,10–19`, `07_sse_websocket/phase-4/spring-server/build.gradle:3,14` | 기존 버전·가상 스레드 유지 |
| MVC·JPA·MySQL | JPA 최종 예제와 phase-4 build.gradle | 기존 의존성 유지 |
| Bearer·STATELESS·CSRF | `07_spring_security/phase-2/spring-server/src/main/java/com/ohgiraffers/restaurant/common/security/SecurityConfig.java:62–78` | 헤더 토큰·서버 세션 미사용 유지 |
| JWT 필터·BCrypt | 같은 파일 144–175행과 JwtTokenProvider.java | 기존 보안 클래스 역할 유지 |
| 프런트 Bearer 전송 | `07_spring_security/phase-2/react-client/src/api/client.js:40–48` | Authorization 헤더 계약 유지 |
| CORS | `07_spring_security/phase-2/spring-server/src/main/java/com/ohgiraffers/restaurant/common/config/CorsConfig.java:15–33` | 정확한 출처·헤더, credentials=false 유지 |
| 전역 예외·보안 401/403 | `07_spring_security/phase-2/spring-server/src/main/java/com/ohgiraffers/restaurant/common/exception/GlobalExceptionHandler.java:23–31,89–115`, 보안 EntryPoint | 역할 분리는 보존하고 응답 필드는 현재 API에 맞춤 |
| 요청 ID·MDC | `05_spring_monitoring/chap01-spring-monitoring/src/main/java/com/ohgiraffers/monitoring/section06/logging/TraceIdFilter.java:54–96` | common으로 이동해 유지 |
| RestClient·시간제한 | `07_sse_websocket/phase-3-1/spring-server/src/main/java/com/ohgiraffers/restaurant/ai/client/OpenAiClient.java:64–86` | 동일 HTTP 방식으로 명세 시간제한 적용 |
| Responses·구조화 출력 | 같은 파일 111–164행, phase-4 OpenAiClient 355–378행 | JSON Schema·store 옵션·요청 구조 유지 |
| Boot 작업 실행기 | `07_sse_websocket/phase-3-1/spring-server/src/main/java/com/ohgiraffers/restaurant/ai/service/RecommendationService.java:84–85,143` | 별도 실행기 대신 applicationTaskExecutor 사용 기준 유지 |
| WebFlux 전환 불필요 | `07_sse_websocket/phase-3-1/HOWTO-PHASE-3-1.md:119–151` | MVC·가상 스레드 유지 |
| 파일 저장 | `04_spring_boot/chap06-file-upload-lecture-source/src/main/java/com/ohgiraffers/file/util/FileUploadUtils.java:8–44` | 외부 디렉터리·설정 경로·UUID 파일명 유지 |
| DDL 관리 | `04_spring_data_jpa/chap02-mapping/src/test/resources/META-INF/persistence.xml:32–46` | 실습 create를 복사하지 않고 none·명시 SQL 유지 |
| Mockito 에이전트 | `07_sse_websocket/phase-4/spring-server/build.gradle:28–30,67–75` | 기존 테스트 설정 유지 |

## 리프레시 토큰 미도입

`07_spring_security/phase-2/HOWTO-PHASE-2.md:3524–3583`의 부록 A는 Refresh Token을
선택 확장으로 설명합니다. 기본 완성 앱은 Access Token 하나를 사용하며, 1시간 만료 후 재로그인합니다.
따라서 리프레시 토큰을 수업에서 다루지 않았다고 설명하지 않습니다.

현재 프로젝트도 **리프레시 토큰 미도입으로 확인**됐으므로 기존 정책을 유지합니다.
Bearer는 토큰 전달 방식이고 Refresh Token은 재발급 수단이므로 서로 배타적인 선택은 아닙니다.
리프레시 토큰을 도입하더라도 Redis가 필수는 아니며, 해당 수업 부록은 관계형 DB 저장을 설명합니다.
재발급 토큰 삭제만으로 기존 Access Token까지 즉시 무효화되지는 않습니다.

## 메시지 큐 등 추가 구성의 판단

| 항목 | 현재 유지하는 판단 |
| --- | --- |
| 메시지 큐 | 재생성 가능한 단일 서버의 요약·DUR 후처리는 수업의 작업 실행기로 구현할 수 있어 기본 설정에 추가하지 않음 |
| Redis | 분산 세션·공유 잠금 요구가 없고 약·DUR 재사용 구조는 MySQL에 준비되어 있어 추가하지 않음 |
| WebFlux | 수업과 현재 프로젝트가 MVC·RestClient·블로킹 JPA·가상 스레드 기준이므로 전환하지 않음 |
| MSA | 수업 모니터링 예제도 단일 서버를 전제하며 현재 독립 서비스 배포 요구가 없어 추가하지 않음 |
| Flyway | 수업은 별도 스키마 변경 관리를 개념적으로 설명하지만 Flyway 구현을 제공하지 않음. 현재 명시 SQL 적용 경로 유지 |
| Resilience4j | 현재 필요한 시간제한·한정 재시도·동시 호출 제한은 기존 클라이언트의 작은 코드로 처리 |
| SSE·WebSocket | 실제 수업 범위지만 현재 API가 비스트리밍 응답이므로 미사용 의존성·메서드는 유지하지 않음 |

작업 실행기는 외부 메시지 브로커와 다릅니다. 서버 재시작 이후 미완료 작업 보존·재전달이
필요해지면 DB 상태 저장과 스케줄러 재처리 같은 작은 구성부터 검토해야 합니다.
가상 스레드만으로 작업 유실 방지가 보장되지는 않습니다.

## 프로젝트 명세에 맞춘 보완

사진 식별의 Responses input_image, 임베딩 호출, Semaphore 동시 호출 제한, 한정 재시도,
MDC 작업 전달, JPA 감사, 한국 시간대 설정은 프로젝트 요구에 맞춘 보완입니다.
실제 수업 예제를 그대로 복사한 구현과 구분하며, 추가 SDK나 서버 인프라를 도입하지 않았습니다.
Qdrant는 허용된 별도 구성으로 보존합니다.

수업의 이미지 API는 이미지 생성 예제이며 처방전 사진 식별과는 용도가 다릅니다.
같은 RestClient·JSON 처리 방식을 활용하더라도 호출 내용은 기능 명세에 맞춰야 합니다.

## 그대로 복사하지 않는 부분

- Security phase-1은 세션 인증이 아니라 인증 도입 전 비교 단계입니다. 클라이언트 회원번호 신뢰·평문 비밀번호 예제는 적용하지 않습니다.
- 수업 프런트의 description/detail/result 파서는 현재 프로젝트의 code/message·직접 DTO 응답에 맞춰야 합니다.
- 수업의 localStorage 저장은 Bearer의 필수 조건이 아닙니다. 이 검토를 이유로 저장소 정책을 새로 확정하지 않습니다.
- 학원 키 생성기는 표준 Base64, 현재 README 생성법과 디코더는 Base64URL입니다. 프로젝트 생성법을 사용하면 정합합니다.
- 수업의 공개 메뉴 이미지 경로를 개인 처방전 파일 공개 경로로 복사하지 않습니다.
- 초기 예제의 요청·응답 원문 로그는 이후 phase-3-2/4에서 제거됐습니다. 현재도 프롬프트·외부 본문을 기록하지 않습니다.

## 이번 반영

인증 방식·의존성·DB 스키마·업무 패키지·Qdrant는 유지했습니다.
회원 안내 주석을 실제 email/user_role 매핑에 맞추고, OpenAI 저장 옵션과 실패 사유 설명을 정리했습니다.
검토 문서를 최신 보고서 문체와 실제 수업 소스 근거로 맞췄습니다.
리프레시 토큰·메시지 큐 등 새 구성은 추가하지 않았습니다.
