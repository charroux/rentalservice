# Spring Data MongoDB tutorial demo

This standalone application belongs to training module 11c. It is isolated
from the rental platform and uses ports `8093` and `27018`.

```powershell
docker compose up --build -d
docker compose ps
```

Create a rental from PowerShell:

```powershell
$body = @{
  rentalId = "RENT-42"
  customerId = "CUSTOMER-7"
  vehicle = @{ plateNumber = "EDU-042"; category = "SUV" }
  status = "OPEN"
  options = @("GPS")
  price = @{ amount = 149.90; currency = "EUR" }
} | ConvertTo-Json -Depth 3 -Compress

Invoke-RestMethod -Method Post -Uri "http://localhost:8093/rentals" `
  -ContentType "application/json" -Body $body
Invoke-RestMethod "http://localhost:8093/rentals/RENT-42"
```

Inspect the mapped document:

```powershell
docker compose exec mongodb mongosh rental_spring_training `
  --quiet --eval "db.rentals.findOne({ rental_id: 'RENT-42' })"
```

Remove only this tutorial environment when finished:

```powershell
docker compose down -v
```
