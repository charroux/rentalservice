# Spring Data Redis List tutorial

This standalone demonstration belongs to training module 08b. It deliberately
does not depend on the rental platform source code or infrastructure.

From this directory:

```powershell
docker compose up --build -d redis rental-producer
```

Create a rental event from PowerShell. Building JSON with `ConvertTo-Json`
avoids differences in quote handling between Windows shells:

```powershell
$body = @{
  rentalId = "RENT-42"
  vehicleId = "CAR-7"
  dailyPrice = 62
} | ConvertTo-Json -Compress

Invoke-RestMethod -Method Post -Uri "http://localhost:8091/rentals" `
  -ContentType "application/json" -Body $body

docker compose exec redis redis-cli LRANGE tutorial:v1:rental-events 0 -1
docker compose up --build -d insurance-consumer
Invoke-RestMethod http://localhost:8092/insurance/offers/RENT-42
```

From `cmd.exe`, use `curl.exe` and escape every JSON double quote:

```cmd
curl.exe -X POST http://localhost:8091/rentals -H "Content-Type: application/json" -d "{\"rentalId\":\"RENT-42\",\"vehicleId\":\"CAR-7\",\"dailyPrice\":62}"
```

The single-quoted JSON form commonly used in Bash must not be copied unchanged
to `cmd.exe`: single quotes are sent as literal characters and Spring returns
HTTP `400 Bad Request` because the request body is not valid JSON.

Stop and remove only the tutorial environment:

```powershell
docker compose down -v
```
