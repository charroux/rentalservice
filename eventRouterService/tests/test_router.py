import json
from pathlib import Path

from event_router.router import EventRouter, PUBLISHED_QUEUE, ROUTING_QUEUE, load_subscriptions


class FakeRedis:
    def __init__(self):
        self.lists: dict[str, list[str]] = {}

    def lmove(self, source: str, destination: str, wherefrom: str, whereto: str):
        values = self.lists.setdefault(source, [])
        if not values:
            return None
        value = values.pop(0 if wherefrom == "LEFT" else -1)
        target = self.lists.setdefault(destination, [])
        target.insert(0 if whereto == "LEFT" else len(target), value)
        return value

    def rpush(self, name: str, value: str):
        self.lists.setdefault(name, []).append(value)
        return len(self.lists[name])

    def lrem(self, name: str, count: int, value: str):
        values = self.lists.setdefault(name, [])
        values.remove(value)
        return 1


def test_loads_and_routes_to_every_matching_subscription(tmp_path: Path):
    for consumer in ("rental", "insurance"):
        (tmp_path / f"{consumer}.json").write_text(
            json.dumps({
                "consumer": consumer,
                "eventType": "AuctionWon",
                "eventVersion": 1,
                "queue": f"{consumer}:ready",
            }),
            encoding="utf-8",
        )
    redis = FakeRedis()
    event = json.dumps({"eventType": "AuctionWon", "eventVersion": 1, "eventId": "evt-1"})
    redis.lists[PUBLISHED_QUEUE] = [event]

    assert EventRouter(redis, load_subscriptions(tmp_path)).route_one()
    assert redis.lists["rental:ready"] == [event]
    assert redis.lists["insurance:ready"] == [event]
    assert redis.lists[ROUTING_QUEUE] == []


def test_keeps_unknown_event_recoverable(tmp_path: Path):
    (tmp_path / "rental.json").write_text(
        json.dumps({
            "consumer": "rental",
            "eventType": "AuctionWon",
            "eventVersion": 1,
            "queue": "rental:ready",
        }),
        encoding="utf-8",
    )
    redis = FakeRedis()
    event = json.dumps({"eventType": "Unknown", "eventVersion": 1, "eventId": "evt-2"})
    redis.lists[PUBLISHED_QUEUE] = [event]

    assert not EventRouter(redis, load_subscriptions(tmp_path)).route_one()
    assert redis.lists[ROUTING_QUEUE] == [event]


def test_recovers_routing_queue(tmp_path: Path):
    redis = FakeRedis()
    redis.lists[ROUTING_QUEUE] = ["one", "two"]
    router = EventRouter(redis, ())

    assert router.recover() == 2
    assert redis.lists[PUBLISHED_QUEUE] == ["one", "two"]
