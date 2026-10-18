#!/bin/sh
set -eu

input_file="/data/${OSM_INPUT_FILE:-south-korea-highways.osm}"
if [ ! -f "$input_file" ]; then
  echo "Routing input file not found: $input_file" >&2
  exit 1
fi

case "$input_file" in
  *.pbf)
    echo "osm2pgrouting requires OSM XML. Convert the PBF file before importing." >&2
    exit 1
    ;;
esac

set -- \
  --f "$input_file" \
  --conf /usr/local/share/osm2pgrouting/mapconfig.xml \
  --dbname "${ROUTING_DB_NAME:-homes_db}" \
  --username "${ROUTING_DB_USER:-homes}" \
  --host "${ROUTING_DB_HOST:-homes-postgres}" \
  --port "${ROUTING_DB_PORT:-5432}" \
  --password "$PGPASSWORD"

if [ "${ROUTING_IMPORT_CLEAN:-true}" = "true" ]; then
  set -- "$@" --clean
fi

exec osm2pgrouting "$@"
