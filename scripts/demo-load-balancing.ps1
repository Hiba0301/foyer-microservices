# Version Windows (PowerShell) : 10 appels via la gateway, puis 10 appels via Feign
1..10 | ForEach-Object { Invoke-RestMethod http://localhost:8080/api/foyers/instance | ConvertTo-Json -Compress }
Write-Output "--- via Feign (universite-service -> foyer-service) ---"
Invoke-RestMethod "http://localhost:8080/api/universites/lb-test?appels=10" | ConvertTo-Json
