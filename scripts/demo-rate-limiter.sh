#!/usr/bin/env bash
# Envoie 10 requêtes simultanées : au-delà de 5 par seconde, le Rate Limiter refuse l'appel
# vers foyer-service et universite-service renvoie le foyer en mode dégradé.
URL="${1:-http://localhost:8080/api/universites/get?idUniversite=1}"
for i in $(seq 1 10); do
  ( printf "requête %2d -> %s\n" "$i" "$(curl -s "$URL" | grep -o '"nomFoyer":"[^"]*"')" ) &
done
wait
