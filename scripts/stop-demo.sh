#!/usr/bin/env bash
# Stops the three Spring Boot processes started by start-demo.sh, using the PID files it wrote.
# Leaves the SQL Server container running by default (least surprising before a live demo — a
# re-run of start-demo.sh should not need a fresh DB pull/migration every time). Pass --with-db
# to also bring the docker compose stack down.
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PID_DIR="$ROOT_DIR/scripts/.pids"

stop_service() {
  local name="$1"
  local pidfile="$PID_DIR/${name}.pid"

  if [ ! -f "$pidfile" ]; then
    echo "    $name: no PID file found, skipping."
    return 0
  fi

  local pid
  pid="$(cat "$pidfile")"
  if kill -0 "$pid" 2>/dev/null; then
    echo "    Stopping $name (pid $pid)..."
    kill "$pid" 2>/dev/null
    for i in $(seq 1 10); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
  else
    echo "    $name: process $pid not running."
  fi
  rm -f "$pidfile"
}

echo "== Costco Travel Smart Rebook — stop-demo =="
stop_service "booking-service"
stop_service "car-supplier-service"
stop_service "hotel-supplier-service"

if [ "${1:-}" = "--with-db" ]; then
  echo "Stopping SQL Server (docker compose down)..."
  docker compose down
else
  echo "SQL Server container left running (pass --with-db to also stop it)."
fi

echo "Done."
