# Lab 11b: MongoDB documents with `mongosh`

## Goal

Model and manipulate rental documents directly in MongoDB before introducing
Spring Data, the insurance aggregate, or the query projection.

The lab uses only the disposable `rental_training` database. Do not write to
the application's `insurance` or `rental_query` databases.

## Prerequisites

Run commands from the repository root. Start the MongoDB replica set and wait
for its initialization container to complete:

```powershell
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d mongodb mongodb-init
docker compose --env-file .env.dev -f docker-compose.dev.yml ps mongodb mongodb-init
docker compose --env-file .env.dev -f docker-compose.dev.yml exec mongodb `
  mongosh --quiet --eval "db.adminCommand({ ping: 1 }).ok"
```

The last command must print `1`. If initialization fails, inspect both logs:

```powershell
docker compose --env-file .env.dev -f docker-compose.dev.yml logs mongodb mongodb-init
```

Open an interactive shell:

```powershell
docker compose --env-file .env.dev -f docker-compose.dev.yml exec mongodb mongosh
```

## Part A: Select an isolated database and inspect BSON

In `mongosh`, select the lab database and ensure that it starts empty:

```javascript
use rental_training
db.getName()
show collections
```

Insert the first rental:

```javascript
db.rentals.insertOne({
  rentalId: "RENT-42",
  customerId: "CUSTOMER-7",
  startedAt: ISODate("2026-10-21T08:00:00Z"),
  vehicle: {
    plateNumber: "EDU-042",
    category: "SUV"
  },
  status: "OPEN",
  options: ["GPS"],
  price: {
    amount: Decimal128("149.90"),
    currency: "EUR"
  },
  days: 3
})
```

Read it back with `findOne()`. Identify the types and roles of `_id`,
`startedAt`, `price.amount`, `vehicle`, and `options`. Explain why BSON type
information matters to comparisons and calculations.

## Part B: Insert several document shapes

Insert three more rentals:

```javascript
db.rentals.insertMany([
  {
    rentalId: "RENT-43",
    customerId: "CUSTOMER-8",
    startedAt: ISODate("2026-10-22T09:30:00Z"),
    vehicle: { plateNumber: "EDU-043", category: "CITY" },
    status: "CONFIRMED",
    options: ["CHILD_SEAT"],
    price: { amount: Decimal128("79.50"), currency: "EUR" },
    days: 2
  },
  {
    rentalId: "RENT-44",
    customerId: "CUSTOMER-7",
    startedAt: ISODate("2026-10-23T12:00:00Z"),
    vehicle: { plateNumber: "EDU-044", category: "SUV" },
    status: "CONFIRMED",
    options: ["GPS", "ADDITIONAL_DRIVER"],
    price: { amount: Decimal128("210.00"), currency: "EUR" },
    days: 4
  },
  {
    rentalId: "RENT-45",
    customerId: "CUSTOMER-9",
    startedAt: ISODate("2026-10-24T07:45:00Z"),
    vehicle: { plateNumber: "EDU-045", category: "UTILITY" },
    status: "OPEN",
    options: [],
    price: { amount: Decimal128("119.00"), currency: "EUR" },
    days: 1,
    notes: "Return before 18:00"
  }
])
```

Run `db.rentals.countDocuments({})`. Observe that only `RENT-45` has `notes`.
Explain why flexible documents do not remove the need for an intentional
application schema.

## Part C: Filter nested fields, arrays, and types

Write and run queries for each question:

1. Find the rental whose business identifier is `RENT-43`.
2. Find every open rental.
3. Find rentals for an `SUV` using dot notation.
4. Find rentals that contain the `GPS` option.
5. Find rentals lasting at least three days.
6. Find rentals started from 23 October 2026 onward using an `ISODate` value.
7. Find documents where `notes` exists.
8. Return only `rentalId`, `vehicle.category`, and `price.amount`, excluding
   `_id`.

Use `find()`, comparison operators such as `$gte`, `$exists`, and a field
projection. For every query, state which access pattern it represents.

## Part D: Update fields and arrays deliberately

Use update operators rather than replacing complete documents:

1. Set the status of `RENT-42` to `CONFIRMED`.
2. Increment its duration by one day.
3. Add `CHILD_SEAT` twice with `$addToSet`; prove that it appears only once.
4. Append `GPS` with `$push`; observe that `$push` does not enforce uniqueness.
5. Remove every `GPS` occurrence with `$pull`.
6. Add an `updatedAt` BSON date using `new Date()`.
7. Read the document after every operation and record `matchedCount` and
   `modifiedCount` from the update result.

Explain when an array behaves like an ordered sequence and when the domain
requires set semantics instead.

## Part E: Compare embedding and referencing

The current document embeds `vehicle` because the lab reads its snapshot with
the rental. Consider a fleet catalog that owns the mutable vehicle lifecycle.

Design, but do not yet implement, two alternatives:

1. keep the immutable plate and category snapshot embedded in each rental;
2. store only a `vehicleId` that references a `vehicles` collection.

For each alternative, analyze:

- the read needed to display rental history;
- what happens when a vehicle category changes;
- ownership of the data;
- consistency expectations;
- cardinality and growth.

Choose one model for rental history and justify it from those access patterns.

## Part F: Enforce identity and inspect query plans

First inspect the plan for a lookup by `rentalId`:

```javascript
db.rentals.find({ rentalId: "RENT-44" }).explain("executionStats")
```

Record the winning plan and the number of examined documents. Then create and
inspect a unique index:

```javascript
db.rentals.createIndex({ rentalId: 1 }, { unique: true })
db.rentals.getIndexes()
db.rentals.find({ rentalId: "RENT-44" }).explain("executionStats")
```

Compare the plans. Attempt to insert another document with
`rentalId: "RENT-44"` and capture the duplicate-key error.

Create a compound index for the query “open rentals by vehicle category”:

```javascript
db.rentals.createIndex({ status: 1, "vehicle.category": 1 })
```

Inspect the plan for that exact filter. Explain why indexes improve selected
reads but consume storage and add work to every affected write.

## Part G: Build an aggregation pipeline incrementally

Start with a pipeline containing only `$match` for confirmed rentals. Add one
stage at a time until it groups by vehicle category and returns:

- the number of rentals;
- the total number of rental days;
- the average daily amount.

Finish with a deterministic sort. A possible target shape is:

```javascript
{
  category: "SUV",
  rentalCount: 2,
  totalDays: 8,
  averageDailyAmount: Decimal128("179.95")
}
```

Use `$match`, `$group`, `$project`, and `$sort`. After each new stage, inspect
the intermediate documents and explain what the next stage receives.

## Part H: Express an atomic state transition

Reset `RENT-45` to `OPEN` if necessary, then run this transition twice:

```javascript
db.rentals.updateOne(
  { rentalId: "RENT-45", status: "OPEN" },
  { $set: { status: "CONFIRMED", confirmedAt: new Date() } }
)
```

Compare `matchedCount` and `modifiedCount` for both executions. Explain how the
filter prevents a second `OPEN → CONFIRMED` transition and why this guarantee
does not atomically publish an event to Redis.

## Cleanup

Verify the selected database before deleting anything:

```javascript
db.getName()
db.rentals.countDocuments({})
```

Only if `db.getName()` returns `rental_training`, remove the disposable lab
database:

```javascript
db.dropDatabase()
```

Exit `mongosh` with `exit`. Do not run Compose with `down -v`, because the
shared MongoDB volume may contain application data from other labs.

## Evidence to submit

- A `mongosh` transcript for Parts A–D
- The embedding-versus-reference decision from Part E
- Query plans before and after index creation
- The duplicate-key error produced by the unique index
- The final aggregation pipeline and its result
- Both results from the conditional state transition

## Review questions

1. How do a document, an embedded document, an array, and an index differ?
2. Which BSON types in the lab have no faithful plain-JSON equivalent?
3. Which access patterns justify the chosen document boundary?
4. Why is a unique index both an access path and an integrity mechanism?
5. What remains outside the atomic boundary of the final update?
6. Why is a field projection in `find()` different from the event-fed read
   projection introduced later in the course?
