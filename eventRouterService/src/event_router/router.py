from __future__ import annotations

from dataclasses import dataclass
import json
import logging
from pathlib import Path
from typing import Protocol

PUBLISHED_QUEUE = "events:simple:published"
ROUTING_QUEUE = "events:simple:routing"


class RedisLists(Protocol):
    def lmove(self, first_list: str, second_list: str, wherefrom: str, whereto: str): ...
    def rpush(self, name: str, value: str): ...
    def lrem(self, name: str, count: int, value: str): ...


@dataclass(frozen=True)
class Subscription:
    consumer: str
    event_type: str
    event_version: int
    queue: str

    def accepts(self, event: dict[str, object]) -> bool:
        return (
            event.get("eventType") == self.event_type
            and event.get("eventVersion") == self.event_version
        )


def load_subscriptions(directory: Path) -> tuple[Subscription, ...]:
    subscriptions: list[Subscription] = []
    for path in sorted(directory.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        subscriptions.append(
            Subscription(
                consumer=_required_text(data, "consumer", path),
                event_type=_required_text(data, "eventType", path),
                event_version=_required_int(data, "eventVersion", path),
                queue=_required_text(data, "queue", path),
            )
        )
    if not subscriptions:
        raise ValueError(f"No subscription descriptors found in {directory}")
    return tuple(subscriptions)


class EventRouter:
    def __init__(self, redis: RedisLists, subscriptions: tuple[Subscription, ...]):
        self.redis = redis
        self.subscriptions = subscriptions
        self.logger = logging.getLogger(__name__)

    def route_one(self) -> bool:
        raw_event = self.redis.lmove(PUBLISHED_QUEUE, ROUTING_QUEUE, "LEFT", "RIGHT")
        if raw_event is None:
            return False
        if isinstance(raw_event, bytes):
            raw_event = raw_event.decode("utf-8")

        try:
            event = json.loads(raw_event)
            targets = [item for item in self.subscriptions if item.accepts(event)]
            if not targets:
                raise ValueError(
                    f"No subscription for {event.get('eventType')} v{event.get('eventVersion')}"
                )
            for subscription in targets:
                self.redis.rpush(subscription.queue, raw_event)
            self.redis.lrem(ROUTING_QUEUE, 1, raw_event)
            self.logger.info(
                "Routed event %s to %s consumer queue(s)", event.get("eventId"), len(targets)
            )
            return True
        except Exception:
            self.logger.exception("Routing failed; event remains in the routing queue")
            return False

    def recover(self) -> int:
        recovered = 0
        while self.redis.lmove(ROUTING_QUEUE, PUBLISHED_QUEUE, "RIGHT", "LEFT") is not None:
            recovered += 1
        return recovered


def _required_text(data: dict[str, object], name: str, path: Path) -> str:
    value = data.get(name)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{path}: {name} must be a non-empty string")
    return value


def _required_int(data: dict[str, object], name: str, path: Path) -> int:
    value = data.get(name)
    if not isinstance(value, int):
        raise ValueError(f"{path}: {name} must be an integer")
    return value
