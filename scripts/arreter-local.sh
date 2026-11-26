#!/usr/bin/env bash
# =====================================================================
#  Arrête les microservices lancés par demarrer-local.sh,
#  puis l'infrastructure Docker (les données des bases sont CONSERVÉES).
#
#  Usage (depuis la racine du projet) :
#    ./scripts/arreter-local.sh             # microservices + conteneurs
#    ./scripts/arreter-local.sh --garder-docker   # microservices seulement
# =====================================================================
set -uo pipefail

cd "$(dirname "$0")/.."
PIDS="logs/pids"

if [ -f "$PIDS" ]; then
  # ordre inverse du démarrage : la gateway d'abord, le config-server en dernier
  while read -r pid nom; do
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" && echo "Arrêt de $nom (PID $pid)"
    fi
  done < <(tail -r "$PIDS" 2>/dev/null || tac "$PIDS")
  rm -f "$PIDS"
else
  echo "Aucun fichier logs/pids : rien à arrêter côté microservices."
fi

if [ "${1:-}" != "--garder-docker" ]; then
  # "stop" et non "down -v" : les volumes (bases MySQL, event store Axon) sont gardés
  docker compose stop
fi
