# Version Windows (PowerShell 7) : 10 requêtes en parallèle
$url = if ($args[0]) { $args[0] } else { "http://localhost:8080/api/universites/get?idUniversite=1" }
1..10 | ForEach-Object -Parallel {
    $r = Invoke-RestMethod -Uri $using:url
    "requête $_ -> $($r.foyer.nomFoyer)"
} -ThrottleLimit 10
