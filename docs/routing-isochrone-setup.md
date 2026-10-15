# 실제 도로망 기반 Isochrone 설정

이 기능은 OpenStreetMap 도로 데이터를 로컬 PostgreSQL에 적재하고 PostGIS/pgRouting으로 계산한다.
외부 유료 API는 사용하지 않지만 데이터베이스 저장공간과 계산 자원은 필요하다.

## 1. 데이터베이스 이미지 변경

`docker-compose.dev.yml`의 PostgreSQL은 PostGIS와 pgRouting이 포함된 이미지로 구성되어 있다.

기존 Docker 볼륨을 유지하는 경우 초기화 SQL이 자동으로 다시 실행되지 않으므로 아래 명령을 한 번 실행한다.

```powershell
docker compose -f docker-compose.dev.yml exec homes-postgres `
  psql -U homes -d homes_db -c "CREATE EXTENSION IF NOT EXISTS postgis; CREATE EXTENSION IF NOT EXISTS pgrouting;"
```

## 2. OSM 데이터 준비

프로젝트 루트에 `routing-data` 폴더를 만들고 대한민국 OSM PBF 파일을 저장한다.

- 다운로드: https://download.geofabrik.de/asia/south-korea-latest.osm.pbf
- 기본 파일명: `routing-data/south-korea-latest.osm.pbf`
- 2026-10 기준 다운로드 파일은 약 275MB이며, DB 적재 후에는 더 많은 공간이 필요하다.

`routing-data`는 Git에 포함하지 않는다.

## 3. 도로 그래프 적재

먼저 데이터베이스를 실행한 다음 import profile을 한 번 실행한다.

```powershell
docker compose -f docker-compose.dev.yml up -d homes-postgres
docker compose -f docker-compose.dev.yml --profile routing-import run --rm routing-import
```

다른 파일명을 사용하려면 `.env`에 `OSM_PBF_FILE`을 지정한다.

```text
OSM_PBF_FILE=my-region.osm.pbf
```

가져오기는 `--clean` 모드이므로 기존 `ways`, `ways_vertices_pgr` 도로 그래프를 교체한다.
일반 매물 테이블은 삭제하지 않는다.

## 4. 확인

```sql
SELECT pgr_version();
SELECT COUNT(*) FROM ways;
SELECT COUNT(*) FROM ways_vertices_pgr;
```

이후 다음 API가 실제 도로 그래프를 사용한다.

```http
GET /properties/{propertyId}/isochrone?mode=walk&time=5
GET /properties/{propertyId}/isochrone?mode=walk&time=10
GET /properties/{propertyId}/isochrone?mode=drive&time=5
GET /properties/{propertyId}/isochrone?mode=drive&time=10
```

도로 데이터나 확장이 준비되지 않은 경우 API는 가상 원을 반환하지 않고 `ISOCHRONE503` 오류를 반환한다.
