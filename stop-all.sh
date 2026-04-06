#!/bin/bash
# AgileGuard — Stop All Services
ROOT="$(cd "$(dirname "$0")" && pwd)"
LOGS="$ROOT/.logs"

CYAN='\033[0;36m'; GREEN='\033[0;32m'; NC='\033[0m'
info() { echo -e "${CYAN}[AgileGuard]${NC} $1"; }

for service in gateway auth-service jira-service ai-service github-service angular; do
  pidfile="$LOGS/$service.pid"
  if [ -f "$pidfile" ]; then
    pid=$(cat "$pidfile")
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid"
      info "Stopped $service (PID $pid)"
    fi
    rm -f "$pidfile"
  fi
done

echo -e "${GREEN}All AgileGuard services stopped.${NC}"
