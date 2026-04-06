#!/bin/bash
# AgileGuard — Run all tests
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
CYAN='\033[0;36m'; GREEN='\033[0;32m'; NC='\033[0m'
info()    { echo -e "${CYAN}[Tests]${NC} $1"; }
success() { echo -e "${GREEN}[Tests]${NC} $1"; }

cd "$ROOT"
info "Running all Spring Boot unit + integration tests..."
mvn test -pl agileguard-auth-service,agileguard-jira-service,agileguard-ai-service,agileguard-github-service

success "All Java tests passed!"

if command -v ng &>/dev/null; then
  cd "$ROOT/agileguard-frontend"
  if [ -d "node_modules" ]; then
    info "Running Angular unit tests..."
    ng test --browsers=ChromeHeadless --watch=false
    success "Angular tests passed!"
  else
    info "Run 'npm install' in agileguard-frontend first to run Angular tests"
  fi
fi
