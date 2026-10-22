# MySQL 설치와 기존 DB 갱신

- 앱 SQL은 00~11의 **12개**임. 새 설치용 00·01, 선택 샘플 02, 기존 DB 변경 이력 03~11로 구성함.
- 최신 01에 현재 18개 테이블·146개 컬럼과 승인된 제약을 통합함. 성분명은 처음부터 VARCHAR(100) NOT NULL임. 새 DB는 03~11을 거칠 필요가 없음.
- 기존 DB의 데이터와 갱신 경로를 위해 03~11을 보존함. 파일 개수와 신규 설치 때 손으로 실행할 횟수는 구분함.

## 처음 설치

서버 폴더에서 아래 명령 한 번을 실행함. 00으로 개발 DB/계정을 준비하고 01로 현재 구조를 생성함.

```powershell
.\scripts\setup-local-db.ps1
```

빈 개발 DB에 샘플이 필요할 때만 아래 명령을 사용함. 02까지 실행하며 실제 사용자 DB에는 사용하지 않음.

```powershell
.\scripts\setup-local-db.ps1 -IncludeTestData
```

- Workbench에서는 00 → 대상 DB 선택 → 최신 01 전체 실행이면 됨. 필요할 때만 같은 버전의 02를 한 번 실행함.
- 최신 01은 기존 14개 테이블 이후의 복약/푸시·복합 UNIQUE·사진 연결·공식 메타데이터·복용량 확인·DUR 이력·벡터 정리·낱알 임시 결과·성분 100자를 포함함.
- 02는 최신 컬럼명 relationship_type/material을 사용함. 옛 01과 최신 02를 섞지 않음. 실제 로그인 비밀번호·공식 품목 일치 검증용 자료가 아닌 화면/관계 fixture임.

## 이미 사용 중인 DB

00~10을 모두 적용한 기존 DB는 11만 대상 DB에서 실행하고 ingredient_patch_status=READY를 확인함. 최신 01을 다시 실행해 기존 컬럼이 바뀌기를 기대하지 않음.

일부 변경이 빠진 기존 팀 DB는 아래 명령으로 03~11을 순서대로 적용함. 01·02는 실행하지 않음.

```powershell
.\scripts\setup-local-db.ps1 -UpgradeExisting
```

- 03~11은 이미 적용된 구조·확인 상태를 보존하도록 작성한 기존 패치임. 변경 이력을 삭제하거나 기존 DB에 샘플 02를 넣지 않음.
- 갱신 전 백업과 대상 DB를 확인함. DDL은 자동 커밋되므로 오류/BLOCKED를 무시하지 않음. 스크립트는 최종 current_schema_status=READY를 확인하며 실패 시 서버를 갱신하지 않음.
- 03의 필수 표/중복 알림 설정 사전 검사, 04의 기존 중복 검사에 실패하면 데이터를 임의 삭제하지 않음. 문제 행을 확인하고 담당자가 정리한 뒤 재실행함.
- 앱의 SQL 자동 실행·JPA 자동 DDL은 계속 꺼져 있음. 이 파일 정리는 사용자 DB를 자동 변경하지 않음.

## 파일별 역할

| 파일 | 용도 |
| --- | --- |
| `00_PILLBADEUM_DB_CREAT.sql` | 개발 DB·계정 준비임. 기존 계정 비밀번호는 바꾸지 않음. |
| `01_PILLBADEUM_DB_SCRIPT.sql` | 새 설치용 최신 전체 구조임. 18표·146열·PK18·FK21·UNIQUE5·CHECK6임. |
| `02_PILLBADEUM_TEST_DATA.sql` | 빈 개발 DB의 선택 샘플임. |
| `03_MEDICATION_UPDATE.sql` | 기존 DB의 컬럼명 오타·직접 입력 약·일정 NULL·푸시 구조 갱신임. |
| `04_MY_DRUG_COMPOSITE_UNIQUE.sql` | 동일 프로필·품목 중복 방지임. item_seq=NULL인 직접 입력 약은 허용함. |
| `05_PHOTO_CANDIDATE_LINKS.sql` | 사진 앞뒤 연결·공식 후보 품목코드임. |
| `06_DRUG_CACHE_METADATA.sql` | 공식 출처/갱신·nullable 취하 상태임. |
| `07_DOSE_CONFIRMATION.sql` | 복용량 확인 상태임. 처음 추가할 때만 기존 일정 유무로 분류하고 기존 수치/일정·재실행 확인 상태를 보존함. |
| `08_CHAT_DUR_SNAPSHOT.sql` | 답변 당시 DUR JSON임. 과거 답변은 NULL로 유지함. |
| `09_VECTOR_CLEANUP.sql` | Qdrant 정리 대기 표임. 외부 정리 성공 후 대기를 제거함. |
| `10_PILL_IDENTIFICATION_SESSION.sql` | 낱알 사진/단서 결과 30분 임시 보관임. |
| `11_DRUG_INGREDIENT_LENGTH.sql` | 기존 material 30자→100자 확장임. 기존 값을 자르지 않으며 과거 손실 문자열을 복원하지 않음. |

- 01의 CREATE TABLE IF NOT EXISTS는 같은 최신 구조 재실행에서 기존 값을 보존함. 기존 컬럼 변경·다른 10/15개 테이블 설계의 자동 이관 기능은 아님.
- 03~11의 순서와 파일명은 기존 팀원 적용 이력을 위해 유지함. 신규 설치와 기존 DB 갱신을 위해 추가 SQL 파일을 만들지 않음.

## 검증 결과

- 최신01 단독 생성과 옛01+03~11의18개 테이블 구조가 같음을 확인함. 최신02 직접 입력, 기존 샘플 전체 보존,01 재실행 보존,07 확인 상태 재실행 보존, 기존DB의11 단독 확장을 확인함.03~11 파일은 미변경임.
- PowerShell7.6.7에서 신규 설치·선택 샘플·기존DB 갱신·충돌 옵션 차단을 실제 전용 MySQL 어댑터와 함께 검증함. Windows의 mysql.exe 자체 실행 검증은 아님.
- 새01만 사용하는 테스트 준비로 전체730개·84스위트·실패/오류/생략0·test/bootJar 성공임. [검증 집계](../docs/SQL_INSTALL_VALIDATION_2026_10_22.json)에 고정함. 실제 사용자DB와 팀원격은 변경하지 않음.
