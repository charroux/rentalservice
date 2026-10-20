# Spring Data Redis List tutorial

This standalone demonstration belongs to training module 08b. It deliberately
does not depend on the rental platform source code or infrastructure.

From this directory:

```powershell
docker compose up --build -d redis rental-producer
```

Create a rental event, inspect it with `redis-cli`, then start the consumer:

```powershell
curl.exe -X POST http://localhost:8091/rentals -H "Content-Type: application/json" -d '{"rentalId":"RENT-42","vehicleId":"CAR-7","dailyPrice":62}'
docker compose exec redis redis-cli LRANGE tutorial:v1:rental-events 0 -1
docker compose up --build -d insurance-consumer
curl.exe http://localhost:8092/insurance/offers/RENT-42
```

Stop and remove only the tutorial environment:

```powershell
docker compose down -v
```
