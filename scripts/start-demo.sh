#!/usr/bin/env bash
# Starts SQL Server (via docker compose), creates the smartrebook database if it doesn't
# already exist, then starts the three Spring Boot services (hotel, car, booking) in the
# background, health-polling each before moving on. Safe to re-run: every step is idempotent,
# so this can be used to resume a partially-started demo environment.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

MSSQL_SA_PASSWORD="${MSSQL_SA_PASSWORD:-DevOnly_P@ssw0rd123}"
MSSQL_DB="${MSSQL_DB:-smartrebook}"
BOOKING_SERVICE_PORT="${BOOKING_SERVICE_PORT:-8080}"
HOTEL_SUPPLIER_PORT="${HOTEL_SUPPLIER_PORT:-8081}"
CAR_SUPPLIER_PORT="${CAR_SUPPLIER_PORT:-8082}"

PID_DIR="$ROOT_DIR/scripts/.pids"
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$PID_DIR" "$LOG_DIR"

echo "== Costco Travel Smart Rebook — start-demo =="

echo "[1/5] Starting SQL Server (docker compose)..."
docker compose up -d

echo "[2/5] Waiting for SQL Server to become healthy..."
for i in $(seq 1 30); do
  status="$(docker inspect --format='{{.State.Health.Status}}' smartrebook-sqlserver 2>/dev/null || echo "starting")"
  if [ "$status" = "healthy" ]; then
    echo "    SQL Server is healthy."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "    SQL Server did not report healthy in time; continuing anyway (it may still be starting)." >&2
  fi
  sleep 2
done

echo "[3/5] Ensuring database '$MSSQL_DB' exists (idempotent)..."
MSYS_NO_PATHCONV=1 docker exec smartrebook-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$MSSQL_SA_PASSWORD" \
  -Q "IF DB_ID('$MSSQL_DB') IS NULL CREATE DATABASE $MSSQL_DB;" \
  || echo "    Warning: could not confirm database creation (SQL Server may still be starting up)." >&2

wait_for_health() {
  local name="$1"
  local port="$2"
  local attempts=30
  for i in $(seq 1 "$attempts"); do
    code="$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}/actuator/health" || echo "000")"
    if [ "$code" = "200" ]; then
      echo "    $name is up (http://localhost:${port})."
      return 0
    fi
    sleep 2
  done
  echo "    Warning: $name did not report healthy on port $port after $((attempts * 2))s." >&2
  return 1
}

is_running() {
  local pidfile="$1"
  [ -f "$pidfile" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null
}

start_service() {
  local name="$1"
  local port="$2"
  local jar_glob="$3"
  local pidfile="$PID_DIR/${name}.pid"

  if is_running "$pidfile"; then
    echo "    $name already running (pid $(cat "$pidfile")); skipping."
    return 0
  fi

  if curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}/actuator/health" 2>/dev/null | grep -q 200; then
    echo "    $name already responding on port $port (started outside this script); skipping launch."
    return 0
  fi

  # shellcheck disable=SC2086
  local jar_file
  jar_file="$(ls $jar_glob 2>/dev/null | head -n1 || true)"
  if [ -z "$jar_file" ]; then
    echo "    Skipping $name: no build artifact found matching $jar_glob (run ./mvnw clean package first)." >&2
    return 1
  fi

  echo "    Launching $name from $jar_file ..."
  nohup java -jar "$jar_file" > "$LOG_DIR/${name}.out.log" 2>&1 &
  echo $! > "$pidfile"
}

echo "[4/5] Starting Spring Boot services (hotel, car, booking)..."
start_service "hotel-supplier-service" "$HOTEL_SUPPLIER_PORT" "$ROOT_DIR/hotel-supplier-service/target/hotel-supplier-service-*.jar"
wait_for_health "hotel-supplier-service" "$HOTEL_SUPPLIER_PORT" || true

start_service "car-supplier-service" "$CAR_SUPPLIER_PORT" "$ROOT_DIR/car-supplier-service/target/car-supplier-service-*.jar"
wait_for_health "car-supplier-service" "$CAR_SUPPLIER_PORT" || true

start_service "booking-service" "$BOOKING_SERVICE_PORT" "$ROOT_DIR/booking-service/target/booking-service-*.war"
wait_for_health "booking-service" "$BOOKING_SERVICE_PORT" || true

echo "[5/5] Done."
echo ""
echo "=============================================================="
echo " Costco Travel Smart Rebook — demo environment"
echo "--------------------------------------------------------------"
echo " Member Portal   http://localhost:${BOOKING_SERVICE_PORT}"
echo " Operations      http://localhost:${BOOKING_SERVICE_PORT}/ops"
echo " Demo booking    CT-DEMO-78291"
echo "=============================================================="
