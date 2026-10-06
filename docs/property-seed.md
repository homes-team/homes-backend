# 팀 개발용 전국 매물 시드 가이드

이 문서는 이슈 #114에서 추가한 전국 단위 매물 시드를 팀 개발 환경에 동일하게 구성하는 방법을 설명합니다.

시더는 기본적으로 비활성화되어 있으며 `local` 프로필에서만 실행됩니다. 운영 환경에서는 사용하지 않습니다.

## 제공 데이터

- 서울 25개 구: 구별 6개, 총 150개
  - 원룸 1개
  - 투룸 1개
  - 오피스텔 1개
  - 아파트 매매 3개
- 경기도 수원시·성남시: 도시별 3개, 총 6개
- 강원·충북·충남·전북·전남·경북·경남·제주: 도별 1개, 총 8개
- 전체: 164개

유형별 테스트 사진 6장을 여러 매물에서 재사용합니다. 이미지가 없는 화면도 확인할 수 있도록 13개 매물에는 사진을 넣지 않습니다.

모든 팀원이 같은 JSON과 생성 규칙을 사용하므로 매물의 지역, 주소, 가격, 유형, 좌표 및 이미지 조합은 동일합니다. 다만 각 DB의 기존 데이터와 자동 증가 값에 따라 매물 ID는 서로 다를 수 있습니다.

## 사전 준비

1. 이슈 #114가 반영된 최신 브랜치를 받습니다.
2. PostgreSQL, Redis 등 로컬 실행에 필요한 의존 서비스를 실행합니다.
3. 애플리케이션에 가입된 일반 사용자 계정을 준비합니다.
4. 해당 사용자의 본인인증을 완료합니다.
5. 사용자 이메일이 로컬 DB의 `users.email` 값과 일치하는지 확인합니다.

시드 소유자는 중개사나 관리자 계정이 아니라 본인인증이 완료된 일반 테스트 계정을 권장합니다.

## 개인 로컬 DB에서 실행

각 팀원이 서로 다른 로컬 DB를 사용하는 경우 각자의 본인인증된 테스트 계정으로 한 번씩 실행합니다.

`application-local.yml`에 다음 설정을 임시로 추가합니다.

```yaml
app:
  seed:
    properties:
      enabled: true
      owner-email: 본인인증이_완료된_테스트_계정_이메일
      image-base-url: http://localhost:8080/seed-images
```

개인 이메일을 저장소에 커밋하지 않습니다.

환경 변수로 설정하려면 다음 값을 사용합니다.

```text
PROPERTY_SEED_ENABLED=true
PROPERTY_SEED_OWNER_EMAIL=본인인증이_완료된_테스트_계정_이메일
PROPERTY_SEED_IMAGE_BASE_URL=http://localhost:8080/seed-images
```

## 공용 개발 DB에서 실행

여러 팀원이 하나의 개발 DB를 공유하는 경우 반드시 대표자 한 명만 시더를 실행합니다.

중복 판정에는 소유자도 포함되므로 서로 다른 이메일로 같은 공용 DB에서 실행하면 동일한 164개가 다시 생성될 수 있습니다.

공용 DB 실행 순서:

1. 팀에서 시드 담당자와 시드 소유 계정을 한 명 정합니다.
2. 다른 팀원은 `enabled: false`를 유지합니다.
3. 담당자만 `enabled: true`로 서버를 한 번 실행합니다.
4. 완료 로그와 매물 개수를 확인합니다.
5. 담당자도 즉시 `enabled: false`로 되돌립니다.

## 실행 및 완료 확인

설정을 적용한 뒤 백엔드 서버를 실행합니다.

정상적으로 완료되면 다음 형식의 로그가 출력됩니다.

```text
Property seed completed: version=1.0.0, created=164, skipped=0, total=164
```

동일한 소유자와 DB에서 다시 실행하면 기존 데이터가 건너뛰어집니다.

```text
Property seed completed: version=1.0.0, created=0, skipped=164, total=164
```

소유자 이메일이 없거나 사용자를 찾을 수 없거나 본인인증이 완료되지 않은 경우 서버 시작이 중단됩니다. 로그를 확인한 뒤 계정과 설정을 수정합니다.

## 실행 후 반드시 비활성화

시드 생성이 끝나면 다음과 같이 변경합니다.

```yaml
app:
  seed:
    properties:
      enabled: false
```

또는 `PROPERTY_SEED_ENABLED=false`로 변경합니다.

활성화 상태로 두더라도 동일한 계정에서는 기존 매물을 건너뛰지만, 매 서버 시작마다 164건의 중복 확인 쿼리가 실행되므로 계속 켜둘 필요가 없습니다.

## 화면과 API 확인

다음 항목을 순서대로 확인합니다.

1. `GET /properties`에서 `[시드]` 제목의 매물이 조회되는지 확인합니다.
2. 검색 지도에서 서울 각 구와 수도권·지방에 매물이 분포하는지 확인합니다.
3. `GET /properties/clusters`에서 줌 레벨에 따라 클러스터 개수가 변하는지 확인합니다.
4. 원룸, 투룸, 오피스텔, 아파트 필터가 동작하는지 확인합니다.
5. 가격 마커를 한 번 눌러 썸네일 미리보기가 표시되는지 확인합니다.
6. 미리보기를 다시 눌러 상세 페이지로 이동하는지 확인합니다.
7. 이미지가 없는 매물에서 `사진 준비 중` UI가 표시되는지 확인합니다.
8. 대표 아파트 매매 매물에서 가격 예측 API를 확인합니다.

정적 이미지 주소는 브라우저에서도 직접 확인할 수 있습니다.

```text
http://localhost:8080/seed-images/one-room.png
http://localhost:8080/seed-images/two-room.png
http://localhost:8080/seed-images/officetel.png
http://localhost:8080/seed-images/apartment-interior.png
http://localhost:8080/seed-images/apartment-exterior.png
http://localhost:8080/seed-images/kitchen-bathroom.png
```

백엔드 포트나 배포 주소가 다르면 `image-base-url`을 해당 서버의 공개 주소로 변경합니다.

## 시더 동작 방식

- 기준 지역: `src/main/resources/seed/property-regions-v1.json`
- 실행 코드: `PropertySeedInitializer`
- 이미지: `src/main/resources/static/seed-images`
- 실행 조건: `local` 프로필이면서 `app.seed.properties.enabled=true`
- 중복 기준: 소유자 ID + 주소 + 상세 주소
- 식별 문구: 제목 `[시드]`, 설명 `[개발용 시드 #114]`

시더는 매물 등록 API를 호출하지 않고 저장소를 통해 직접 데이터를 생성합니다. 따라서 다음 기능을 대량으로 호출하지 않습니다.

- S3 presigned URL 검증
- 건축물대장 자동 수집
- 아파트 단지 정보 자동 수집
- 실거래가 월별 조회
- AI 다면평가 생성

가격 예측과 AI 평가는 시드 생성 완료 후 필요한 대표 매물만 선택해 호출합니다.

## 주의 사항

- `application-local.yml`의 개인 이메일과 비밀키를 커밋하지 않습니다.
- 실제 매물로 오해하지 않도록 `[시드]` 표시를 제거하지 않습니다.
- 운영 DB에서는 실행하지 않습니다.
- 공용 DB에서 팀원별로 반복 실행하지 않습니다.
- 시드 매물 삭제가 필요하면 DB를 초기화할 수 있는 개인 로컬 환경에서 실행하는 것을 권장합니다.
- 연관된 입찰, 찜, 리뷰 데이터를 만든 뒤에는 단순 SQL 삭제 시 FK 오류가 발생할 수 있으므로 직접 일괄 삭제하지 않습니다.
