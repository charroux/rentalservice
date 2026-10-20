# Lab 08: Redis data and messaging

## Goal

Manipulate the five Redis structures introduced in the module, observe their
semantics, and connect Lists and durability to the rental platform.

Estimated time: 75–90 minutes.

## Prerequisites

- Docker
- Module 07 completed
- The repository root as the current directory

Start Redis and verify that the service is running:

```powershell
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d redis
docker compose --env-file .env.dev -f docker-compose.dev.yml ps redis
docker compose --env-file .env.dev -f docker-compose.dev.yml exec redis redis-cli PING
```

The last command must return `PONG`. If `exec` reports that Redis is not
running, inspect `ps redis` and the container logs before continuing:

```powershell
docker compose --env-file .env.dev -f docker-compose.dev.yml logs redis
```

Run the remaining Redis commands from an interactive client:

```powershell
docker compose --env-file .env.dev -f docker-compose.dev.yml exec redis redis-cli
```

Use only keys prefixed with `training:`. Do not modify the application's event
queues.

## Part A: Inspect the keyspace safely

1. Create `training:probe` with any String value.
2. Use `EXISTS`, `TYPE`, and `TTL` to describe it.
3. Add a 60-second lifetime, then observe `TTL` twice at different times.
4. Delete the key and observe the special `TTL` result for a missing key.
5. Run `SCAN 0 MATCH training:* COUNT 100`. If Redis returns a non-zero cursor,
   repeat `SCAN` with that cursor until it returns `0`.

Record what `TTL` returns for a persistent key and for a missing key. Explain
why incremental `SCAN` is safer than `KEYS *` on a production database.

## Part B: String — value, counter, and conditional write

Create and inspect the following keys:

- `training:rental:RENT-42:status`, initially `PENDING`, then `CONFIRMED`;
- `training:rental:RENT-42:summary`, with a lifetime of 300 seconds;
- `training:metrics:auctions`, incremented once and then by five;
- `training:claim:RENT-42`, created conditionally with `NX` and a 30-second
  lifetime.

Use `SET`, `GET`, `MGET`, `TTL`, `INCR`, and `INCRBY`. Attempt the conditional
creation twice with different worker identifiers and explain both results.

Questions:

1. What value does the counter contain after both increments?
2. What happens if `INCR` targets a String that does not contain an integer?
3. Why is `SET ... NX EX` useful but insufficient by itself for a robust
   distributed lock?

## Part C: Hash — update fields independently

Represent rental `RENT-42` as the Hash `training:rental:RENT-42` with these
fields: `status`, `carModel`, `finalPrice`, and `retryCount`.

1. Create all four fields with `HSET`.
2. Read only `status`, then read `carModel` and `finalPrice` together.
3. Inspect the complete Hash and count its fields.
4. Increment `retryCount` atomically.
5. Test whether `finalPrice` exists, then remove `retryCount`.
6. Apply a five-minute expiration and verify that it belongs to the whole key,
   not to one field.

Use `HGET`, `HMGET`, `HGETALL`, `HLEN`, `HINCRBY`, `HEXISTS`, `HDEL`, `EXPIRE`,
and `TTL`. Record the return value from the initial `HSET`, then update an
existing field and explain why the two `HSET` results differ.

## Part D: Set — unique membership and set algebra

Use these keys:

- `training:car:42:features`: `electric`, `automatic`, `gps`, and a duplicate
  `gps` in the same insertion;
- `training:request:required`: `automatic`, `gps`, and `child-seat`.

1. Add the members and record the result of `SADD`.
2. Count the car's features.
3. Test membership for `gps` and `sunroof`, first separately and then together.
4. Find the intersection of the two Sets.
5. Find required features missing from the car.
6. Remove one feature and inspect the result.

Use `SADD`, `SCARD`, `SISMEMBER`, `SMISMEMBER`, `SINTER`, `SDIFF`, `SREM`, and
`SMEMBERS`. Explain why a Set cannot be used to preserve display order. State
which command you would use instead of `SMEMBERS` to inspect a very large Set.

## Part E: Sorted Set — ranking and score ranges

Create `training:insurance:offers` with these daily prices as scores:

| Member | Score |
|---|---:|
| `OFFER-A` | 12.5 |
| `OFFER-B` | 9 |
| `OFFER-C` | 16 |

1. Display every offer from cheapest to most expensive, including scores.
2. Return only the two cheapest offers.
3. Find both the rank and score of `OFFER-A`.
4. Select offers costing at most 12.5.
5. Decrease the score of `OFFER-A` by one.
6. Re-add `OFFER-B` with a new score and determine whether a duplicate appears.
7. Remove `OFFER-C`.

Use `ZADD`, `ZRANGE`, `ZRANK`, `ZSCORE`, `ZINCRBY`, and `ZREM`. Then add two
temporary members with the same score and observe how Redis orders them.

## Part F: List — FIFO and recoverable processing

Use only these disposable Lists:

- `training:queue:ready`
- `training:queue:processing`

1. Append `event-1`, `event-2`, and `event-3` to `ready` with `RPUSH`.
2. Inspect its length and a bounded range of elements.
3. Remove one item with `LPOP` and verify that `RPUSH` + `LPOP` is FIFO.
4. Atomically move the next item from the left of `ready` to the right of
   `processing` with `LMOVE`.
5. Inspect both Lists. Treat the moved item as successfully processed and
   acknowledge it with `LREM`.
6. Move another item to `processing`, simulate a crash before acknowledgement,
   then design and execute an `LMOVE` that returns it to `ready`.

Explain:

- why two consumers calling `LPOP` on the same List receive different events;
- what `BLPOP` or `BLMOVE` changes compared with polling;
- where duplicate processing can occur in the ready/processing protocol;
- why `LRANGE 0 -1` should remain a diagnostic command for small Lists.

## Part G: Persistence and messaging choices

1. Read the Redis `command` and volume configuration in
   `docker-compose.dev.yml`.
2. Write a uniquely identifiable persistent String under `training:`.
3. Restart only Redis without deleting its volume:

   ```powershell
   docker compose --env-file .env.dev -f docker-compose.dev.yml restart redis
   ```

4. Reconnect with `redis-cli` and verify whether the key remains available.
5. Without executing it, explain the effect of `docker compose ... down -v` on
   the named Redis volume.
6. Compare Pub/Sub, Lists, and Streams for an event published while its consumer
   is offline, competing consumers, independent consumer groups, and replay.
7. Explain why the current platform uses a router and one ready List per
   consumer instead of making Rental and Insurance consume the same List.

## Cleanup

Inspect the keys before deleting them, then remove only the lab namespace:

```text
SCAN 0 MATCH training:* COUNT 100
```

Use the returned keys with `DEL`. Do not use `FLUSHDB`, because the Redis
database may also contain application queues.

## Evidence to submit

- A command transcript for Parts A–F, including command return values
- A table mapping each access pattern to the selected Redis structure
- Persistence observations before and after the Redis restart
- A Pub/Sub, Lists, and Streams comparison table
- Short answers to every question and explanation in the lab

## Review questions

1. Which keys need a TTL, and which must remain until explicitly processed?
2. Which commands are atomic, and where does the workflow still span multiple
   operations?
3. Which keys or Lists can grow without a bound and therefore need monitoring?
4. Which Redis configuration assumptions affect durability?
5. Which structure would you choose for eligibility, ranking, object fields,
   counters, and work queues—and why?
