#!/usr/bin/env bash
# =============================================================================
#  CareerBridge — Build & run script (TLS-resilient)
#
#  USAGE:
#    ./build.sh                  → build all images + start core services
#    ./build.sh full             → build all images + start ALL services
#    ./build.sh --no-cache       → force full clean rebuild (core profile)
#    ./build.sh --no-cache full  → force full clean rebuild (all services)
#    ./build.sh down             → stop and remove containers
#    ./build.sh clean            → stop + prune dangling images + volumes
#    ./build.sh prune            → nuclear: remove ALL unused Docker data
#    ./build.sh logs [service]   → tail logs (optional: specific service)
#    ./build.sh status           → containers + image sizes + disk usage
#    ./build.sh rebuild <svc>    → rebuild and restart one specific service
#    ./build.sh import-ca <cert> → import a custom CA cert into Maven truststore
# =============================================================================
set -euo pipefail

# ── Enable BuildKit (required for --mount=type=cache) ───────────────────────
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# ── Force Maven to use TLS 1.2+ and our settings.xml ────────────────────────
export MAVEN_OPTS="\
  -Dmaven.wagon.http.ssl.insecure=true \
  -Dmaven.wagon.http.ssl.allowall=true \
  -Dhttps.protocols=TLSv1.2,TLSv1.3"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${BLUE}ℹ  $*${NC}"; }
success() { echo -e "${GREEN}✅ $*${NC}"; }
warn()    { echo -e "${YELLOW}⚠  $*${NC}"; }
error()   { echo -e "${RED}✗  $*${NC}"; exit 1; }
header()  { echo -e "\n${CYAN}══ $* ══${NC}"; }

check_env() {
  if [ ! -f .env ]; then
    warn ".env file not found — copying .env.example to .env"
    if [ -f .env.example ]; then
      cp .env.example .env
      warn "Please edit .env and fill in real secrets before running again."
      exit 1
    else
      error ".env.example not found. Create .env with required variables."
    fi
  fi
}

# Import a custom CA certificate into the Maven settings truststore
# Usage: ./build.sh import-ca /path/to/corporate-ca.crt
import_ca() {
  local CERT_FILE="${1:-}"
  [ -z "$CERT_FILE" ] && error "Usage: ./build.sh import-ca /path/to/ca.crt"
  [ ! -f "$CERT_FILE" ] && error "Certificate file not found: $CERT_FILE"

  info "Importing CA certificate: $CERT_FILE"

  # Copy cert into the docker/maven directory so Docker COPY can access it
  cp "$CERT_FILE" docker/maven/corporate-ca.crt

  # Patch each Dockerfile to import the cert during build
  for DF in */Dockerfile; do
    if ! grep -q "corporate-ca.crt" "$DF"; then
      # Insert keytool import after the settings.xml COPY line
      sed -i '/COPY docker\/maven\/settings.xml/a\\
# ── TLS fix: import corporate CA so JDK trusts the proxy certificate\nCOPY docker/maven/corporate-ca.crt /tmp/corporate-ca.crt\nRUN keytool -importcert -noprompt -trustcacerts \\\n    -alias corporate-ca \\\n    -file /tmp/corporate-ca.crt \\\n    -keystore "$JAVA_HOME/lib/security/cacerts" \\\n    -storepass changeit || true' "$DF"
    fi
  done

  success "CA certificate imported. Rebuild with: ./build.sh --no-cache"
}

show_status() {
  header "Running containers"
  docker compose --profile full ps
  echo ""
  header "CareerBridge image sizes"
  docker images --filter "reference=careerbridge/*" \
    --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedSince}}" | sort
  echo ""
  header "Docker disk usage"
  docker system df
}

print_urls() {
  local full=${1:-false}
  echo ""
  echo "  🌐  Frontend          →  http://localhost:3000"
  echo "  🔀  API Gateway       →  http://localhost:8080"
  echo "  📋  Eureka            →  http://localhost:8761"
  echo "  ⚙️   Config Server    →  http://localhost:8888"
  if [ "$full" = "true" ]; then
    echo "  🔔  Notification Svc  →  http://localhost:8085"
    echo "  🛡️   Admin Service    →  http://localhost:8086"
  else
    warn "notification-service and admin-service are NOT started."
    warn "Run './build.sh full' to include them."
  fi
  echo ""
  echo "  Commands:"
  echo "    ./build.sh logs [service]    tail logs"
  echo "    ./build.sh status            containers + image sizes"
  echo "    ./build.sh rebuild <svc>     rebuild one service"
  echo "    ./build.sh down              stop everything"
  echo "    ./build.sh clean             stop + prune dangling"
  echo "    ./build.sh prune             nuclear cleanup"
  echo "    ./build.sh import-ca <cert>  import corporate CA cert"
}

case "${1:-}" in

  down)
    info "Stopping all containers..."
    docker compose --profile full down
    success "Containers stopped."
    ;;

  clean)
    info "Stopping containers + pruning dangling images and unused volumes..."
    docker compose --profile full down
    docker image prune -f
    docker volume prune -f
    docker builder prune -f
    success "Cleanup complete."
    docker system df
    ;;

  prune)
    warn "This removes ALL unused Docker images, containers, networks and build cache."
    warn "Your named volumes (mysql_data, kafka_data, etc.) are preserved."
    read -rp "Are you sure? [y/N] " confirm
    [[ "$confirm" =~ ^[Yy]$ ]] || { info "Aborted."; exit 0; }
    docker compose --profile full down
    docker system prune -af
    success "Full Docker prune complete."
    docker system df
    ;;

  import-ca)
    import_ca "${2:-}"
    ;;

  --no-cache)
    check_env
    PROFILE="${2:-}"
    if [ "$PROFILE" = "full" ]; then
      info "Building ALL images (NO cache — full rebuild)..."
      docker compose --profile full build --no-cache --parallel
      info "Starting ALL services..."
      docker compose --profile full up -d
      success "All services started (no-cache build)."
      print_urls true
    else
      info "Building core images (NO cache — full rebuild)..."
      docker compose build --no-cache --parallel
      info "Starting core services..."
      docker compose up -d
      success "Core services started (no-cache build)."
      print_urls false
    fi
    show_status
    ;;

  logs)
    shift || true
    docker compose --profile full logs -f --tail=200 "${@}"
    ;;

  status)
    show_status
    ;;

  rebuild)
    SERVICE="${2:-}"
    [ -z "$SERVICE" ] && error "Usage: ./build.sh rebuild <service-name>"
    check_env
    info "Rebuilding service: $SERVICE..."
    DOCKER_BUILDKIT=1 docker compose --profile full build "$SERVICE"
    docker compose --profile full up -d --no-deps "$SERVICE"
    success "Service $SERVICE rebuilt and restarted."
    ;;

  full)
    check_env
    info "Building ALL images (incl. notification-service, admin-service)..."
    docker compose --profile full build --parallel
    info "Starting ALL services in detached mode..."
    docker compose --profile full up -d
    success "All services started."
    print_urls true
    ;;

  *)
    check_env
    info "Building core images (using BuildKit cache)..."
    docker compose build --parallel
    info "Starting core services in detached mode..."
    docker compose up -d
    success "Core services started."
    print_urls false
    ;;

esac
