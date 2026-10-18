# 실제 도로망 기반 Isochrone 설정

이 기능은 OpenStreetMap 도로 데이터를 PostgreSQL에 적재하고 PostGIS/pgRouting으로 계산한다.
외부 유료 API는 사용하지 않지만 데이터베이스 저장공간과 계산 자원은 필요하다.

## 1. 데이터베이스 확장 준비

`docker-compose.dev.yml`의 PostgreSQL은 PostGIS와 pgRouting이 포함된 이미지로 구성되어 있다.

기존 Docker 볼륨에서는 초기화 SQL이 다시 실행되지 않으므로 아래 명령을 한 번 실행한다.

```powershell
docker compose -f docker-compose.dev.yml exec homes-postgres `
  psql -U homes -d homes_db -c "CREATE EXTENSION IF NOT EXISTS postgis; CREATE EXTENSION IF NOT EXISTS pgrouting;"
```

Windows에 직접 설치한 PostgreSQL을 사용하는 경우 해당 DB에 같은 확장을 생성한다.

## 2. OSM 원본 다운로드

프로젝트 루트에 `routing-data` 폴더를 만들고 대한민국 OSM PBF 파일을 저장한다.

- 다운로드: https://download.geofabrik.de/asia/south-korea-latest.osm.pbf
- 저장 위치: `routing-data/south-korea-latest.osm.pbf`
- 2026-10 기준 약 276MB

`routing-data`는 Git에 포함하지 않는다.

## 3. 테스트 권역 추출 및 XML 변환

`osm2pgrouting` 3.0은 PBF를 직접 읽지 않으므로 OSM XML로 변환해야 한다.
전국 데이터를 한 번에 변환하면 XML이 약 2.7GB가 되고 import 중 16GB 이상의 메모리가 필요할 수 있다.
로컬 개발에서는 필요한 권역을 추출한 뒤 적재하는 방식을 권장한다.

서울·수도권 테스트 데이터 추출:

```powershell
docker run --rm `
  -v ${PWD}/routing-data:/data `
  stefda/osmium-tool `
  osmium extract --bbox 126.5,36.9,127.8,38.0 --strategy complete_ways `
  /data/south-korea-latest.osm.pbf -o /data/seoul-metro.osm.pbf --overwrite
```

도로 객체만 OSM XML로 변환:

```powershell
docker run --rm `
  -v ${PWD}/routing-data:/data `
  stefda/osmium-tool `
  osmium tags-filter /data/seoul-metro.osm.pbf w/highway `
  -o /data/seoul-metro-highways.osm -f osm --overwrite
```

## 4. 도로 그래프 적재

Compose PostgreSQL을 사용하는 기본 설정:

```text
OSM_INPUT_FILE=seoul-metro-highways.osm
```

```powershell
docker compose -f docker-compose.dev.yml --profile routing-import run --rm routing-import
```

Windows 로컬 PostgreSQL을 사용하는 경우 `.env`에 다음 값을 추가한다.

```text
ROUTING_DB_HOST=host.docker.internal
ROUTING_DB_PORT=5432
ROUTING_DB_NAME=homes_db
ROUTING_DB_USER=postgres
OSM_INPUT_FILE=seoul-metro-highways.osm
```

```powershell
docker compose -f docker-compose.dev.yml --profile routing-import run --rm --no-deps routing-import
```

`ROUTING_IMPORT_CLEAN=true`가 기본값이므로 기존 `ways`, `ways_vertices_pgr` 도로 그래프를 교체한다.
일반 매물·회원 테이블은 삭제하지 않는다.

## 5. 확인

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
