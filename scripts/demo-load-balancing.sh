#!/usr/bin/env bash
# 10 appels à /api/foyers/instance via la gateway : on doit voir alterner les ports 8081 et 8083.
for i in $(seq 1 10); do
  curl -s http://localhost:8080/api/foyers/instance; echo
done
echo "--- via Feign (universite-service -> foyer-service) ---"
curl -s "http://localhost:8080/api/universites/lb-test?appels=10"; echo
