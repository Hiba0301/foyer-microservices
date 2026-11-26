#!/usr/bin/env bash
# =====================================================================
#  Démarre toute l'architecture en local (macOS / Linux), dans le bon ordre :
#    infrastructure Docker -> config-server -> eureka-server -> foyer-service (8081 + 8083)
#    -> universite-service -> bloc-service -> gateway-service
#
#  Usage (depuis la racine du projet) :
#    ./scripts/demarrer-local.sh               # démarre avec les jars déjà construits
#    ./scripts/demarrer-local.sh --build       # recompile d'abord (mvn clean install -DskipTests)
#    ./scripts/demarrer-local.sh --monitoring  # ajoute Prometheus + Grafana (étape 8)
#
#  Les logs de chaque service sont dans logs/<service>.log  (ex. tail -f logs/foyer-8081.log)
#  Arrêt : ./scripts/arreter-local.sh
# =====================================================================
set -euo pipefail

cd "$(dirname "$0")/.."
RACINE="$(pwd)"
LOGS="$RACINE/logs"
PIDS="$LOGS/pids"
VERSION="1.0.0-SNAPSHOT"

BUILD=false
MONITORING=false
for arg in "$@"; do
  case "$arg" in
    --build) BUILD=true ;;
    --monitoring) MONITORING=true ;;
    *) echo "Option inconnue : $arg"; exit 1 ;;
  esac
done

vert()  { printf "\033[32m%s\033[0m\n" "$*"; }
rouge() { printf "\033[31m%s\033[0m\n" "$*"; }
info()  { printf "\033[36m==> %s\033[0m\n" "$*"; }

# ---------------------------------------------------------------------
# 1. Vérifications
# ---------------------------------------------------------------------
info "Vérification de Docker"
if ! docker info >/dev/null 2>&1; then
  rouge "Docker n'est pas démarré : lance Docker Desktop puis relance le script."
  exit 1
fi

info "Vérification des ports des microservices"
PORTS_APPS=(8888 8761 8081 8083 8082 8084 8080)
occupe=false
for p in "${PORTS_APPS[@]}"; do
  if lsof -nP -iTCP:"$p" -sTCP:LISTEN >/dev/null 2>&1; then
    rouge "  Port $p déjà utilisé : $(lsof -nP -iTCP:"$p" -sTCP:LISTEN | awk 'NR==2{print $1" (PID "$2")"}')"
    occupe=true
  fi
done
if $occupe; then
  rouge "Arrête ce qui utilise ces ports (service lancé dans IntelliJ, Jenkins, Nexus : docker stop jenkins nexus)."
  exit 1
fi
vert "  Ports 8888 8761 8081 8083 8082 8084 8080 libres"

# Ports de l'infrastructure : acceptés s'ils sont pris par NOS conteneurs
INFRA=(mysql-foyer:3307 mysql-universite:3308 mysql-bloc:3309 axonserver:8124 zipkin:9411)
$MONITORING && INFRA+=(prometheus:9090 grafana:3000)
for entree in "${INFRA[@]}"; do
  nom="${entree%%:*}"; port="${entree##*:}"
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1 \
     && [ "$(docker inspect -f '{{.State.Running}}' "$nom" 2>/dev/null)" != "true" ]; then
    rouge "  Port $port occupé par autre chose que le conteneur $nom :"
    docker ps --format '  {{.Names}}  {{.Ports}}' | grep ":$port->" || lsof -nP -iTCP:"$port" -sTCP:LISTEN
    exit 1
  fi
done
vert "  Ports de l'infrastructure OK"

# ---------------------------------------------------------------------
# 2. Compilation (optionnelle) et présence des jars
# ---------------------------------------------------------------------
if $BUILD; then
  info "Compilation : mvn clean install -DskipTests"
  mvn -q clean install -DskipTests
  vert "  Compilation terminée"
fi

MODULES=(config-server eureka-server foyer-service universite-service bloc-service gateway-service)
for m in "${MODULES[@]}"; do
  if [ ! -f "$m/target/$m-$VERSION.jar" ]; then
    rouge "Jar introuvable : $m/target/$m-$VERSION.jar -> relance avec --build"
    exit 1
  fi
done

# ---------------------------------------------------------------------
# 3. Infrastructure Docker
# ---------------------------------------------------------------------
SERVICES_DOCKER=(mysql-foyer mysql-universite mysql-bloc axonserver zipkin)
$MONITORING && SERVICES_DOCKER+=(prometheus grafana)
info "Démarrage de l'infrastructure Docker : ${SERVICES_DOCKER[*]}"
docker compose up -d "${SERVICES_DOCKER[@]}"

attendre_mysql() {
  local nom="$1" i
  for i in $(seq 1 60); do
    if [ "$(docker inspect -f '{{.State.Health.Status}}' "$nom" 2>/dev/null)" = "healthy" ]; then
      vert "  $nom : healthy"; return 0
    fi
    sleep 2
  done
  rouge "  $nom n'est pas healthy après 2 minutes (docker logs $nom)"; exit 1
}
for b in mysql-foyer mysql-universite mysql-bloc; do attendre_mysql "$b"; done

# ---------------------------------------------------------------------
# 4. Microservices (java -jar, en arrière-plan, un log par service)
# ---------------------------------------------------------------------
mkdir -p "$LOGS"
: > "$PIDS"

attendre_sante() {
  local nom="$1" port="$2" i
  for i in $(seq 1 90); do
    if curl -sf "http://localhost:$port/actuator/health" 2>/dev/null | grep -q '^{"status":"UP"'; then
      vert "  $nom (port $port) : UP"; return 0
    fi
    sleep 2
  done
  rouge "  $nom (port $port) ne répond pas après 3 minutes : regarde logs/$nom.log"
  exit 1
}

lancer() {
  local nom="$1" module="$2" port="$3"; shift 3
  info "Démarrage de $nom"
  # lancé depuis la racine : config-server trouve ./config-repo
  nohup java -jar "$module/target/$module-$VERSION.jar" "$@" > "$LOGS/$nom.log" 2>&1 &
  echo "$! $nom" >> "$PIDS"
  attendre_sante "$nom" "$port"
}

lancer config-server      config-server      8888
lancer eureka-server      eureka-server      8761
lancer foyer-8081         foyer-service      8081
lancer foyer-8083         foyer-service      8083 --server.port=8083
lancer universite-service universite-service 8082
lancer bloc-service       bloc-service       8084
lancer gateway-service    gateway-service    8080

# ---------------------------------------------------------------------
# 5. Résumé
# ---------------------------------------------------------------------
echo
vert "Tout est démarré."
echo "  Eureka        http://localhost:8761"
echo "  Gateway       http://localhost:8080/api/foyers/getAll"
echo "  Swagger       http://localhost:8081/swagger-ui.html  (8082, 8084 aussi)"
echo "  Axon Server   http://localhost:8024"
echo "  Zipkin        http://localhost:9411"
$MONITORING && echo "  Prometheus    http://localhost:9090   Grafana http://localhost:3000 (admin/admin)"
echo
echo "  Logs : tail -f logs/<service>.log   ($(awk '{print $2}' "$PIDS" | tr '\n' ' '))"
echo "  Arrêt : ./scripts/arreter-local.sh"
