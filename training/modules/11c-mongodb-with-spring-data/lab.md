# Lab 11c: MongoDB with Spring Data

## Goal

Run and examine a small Spring Boot application that maps a rental aggregate
to MongoDB. Relate every Java abstraction to the BSON document and index that
you manipulated directly in Tutorial 11b.

The standalone code is in `training/demos/mongodb-spring-data`. It uses an
isolated database, volume, and ports; it does not modify the main application.

## Part A: Read the mapping before running it

Inspect `Rental.java`, `RentalRepository.java`, `RentalController.java`, and
`application.properties`. Before executing anything, write down:

1. the collection name;
2. the stored names of `rentalId`, `customerId`, and `createdAt`;
3. which values will be embedded;
4. which field is the technical identity and which is the business identity;
5. the queries implied by the repository method names.

Explain why `rentalId` is unique while MongoDB still creates `_id`.

## Part B: Start the isolated application

From the demo directory:

```powershell
cd training/demos/mongodb-spring-data
docker compose up --build -d
docker compose ps
docker compose logs --tail 100 rental-api
```

Wait until `rental-api` has started. If it exits, check that ports `8093` and
`27018` are free and inspect the logs before rebuilding.

## Part C: Create a document through Spring Data

In PowerShell, let `ConvertTo-Json` handle Windows quoting:

```powershell
$body = @{
  rentalId = "RENT-42"
  customerId = "CUSTOMER-7"
  vehicle = @{ plateNumber = "EDU-042"; category = "SUV" }
  status = "OPEN"
  options = @("GPS", "CHILD_SEAT")
  price = @{ amount = 149.90; currency = "EUR" }
} | ConvertTo-Json -Depth 3 -Compress

$created = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8093/rentals" `
  -ContentType "application/json" -Body $body
$created | ConvertTo-Json -Depth 5
```

Retrieve it by business identifier:

```powershell
Invoke-RestMethod "http://localhost:8093/rentals/RENT-42" |
  ConvertTo-Json -Depth 5
```

Which values were supplied by the client, Spring, and MongoDB respectively?

## Part D: Inspect the physical result with `mongosh`

```powershell
docker compose exec mongodb mongosh rental_spring_training
```

Then run:

```javascript
db.rentals.findOne({ rental_id: "RENT-42" })
db.rentals.getIndexes()
db.rentals.find({ customer_id: "CUSTOMER-7", status: "OPEN" })
  .explain("executionStats")
```

Check the BSON types, embedded structure, physical field names, and indexes.
Compare them with your predictions from Part A. Does the query plan use the
most selective possible index? Explain what compound index you would consider
for a large collection.

Exit `mongosh` with `exit`.

## Part E: Exercise a derived query

```powershell
Invoke-RestMethod `
  "http://localhost:8093/rentals?customerId=CUSTOMER-7&status=OPEN" |
  ConvertTo-Json -Depth 5
```

Locate the repository method responsible for the query. Decompose its name
into properties and operators. Then add a second rental for the same customer
with status `CLOSED` and verify that it is not returned.

## Part F: Observe integrity enforcement

Send the original POST a second time:

```powershell
try {
  Invoke-RestMethod -Method Post -Uri "http://localhost:8093/rentals" `
    -ContentType "application/json" -Body $body
} catch {
  $_.Exception.Response.StatusCode.value__
  $_.ErrorDetails.Message
}
```

Confirm HTTP `409`. Trace the result through four layers:

1. the unique MongoDB index;
2. the driver error;
3. Spring's `DuplicateKeyException` translation;
4. `ApiExceptionHandler`.

Explain why checking `findByRentalId` before `save` would not replace the
unique index when two requests arrive concurrently.

## Part G: Small autonomous extension

Add this access pattern without using `MongoTemplate`:

> Find all open rentals whose embedded vehicle category equals a requested
> category.

1. Add a derived repository method.
2. Add a `GET` endpoint with clear parameters.
3. Add the matching index to the mapping.
4. Rebuild the application and test the endpoint.
5. Use `explain("executionStats")` to verify the physical query.

Hints:

- derived queries navigate nested properties by property name;
- `_` can make the nested boundary explicit in a method name;
- the stored path is `vehicle.category`;
- an index that starts with `vehicle.category` may not be ideal if the real
  access pattern always combines category and status.

## Part H: Connect this tutorial to the platform

Compare the demonstration with `insuranceService` and answer:

- Why is `InsuranceOffer` an authoritative aggregate rather than a projection?
- Why does its outbox require a multidocument transaction?
- Why does that transaction require the main Compose MongoDB replica set?
- What complexity was intentionally absent from this tutorial?

Do not copy tutorial code into the business service. The purpose is to make the
Spring Data mechanisms visible before studying their architectural use.

## Cleanup and deliverables

```powershell
docker compose down -v
```

Provide:

- answers from Parts A, D, F, and H;
- the changed Java files for Part G;
- one API response and its corresponding BSON document;
- the relevant `explain("executionStats")` excerpt.
