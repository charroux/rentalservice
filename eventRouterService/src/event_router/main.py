from __future__ import annotations

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import logging
import os
from pathlib import Path
from threading import Thread
import time

from redis import Redis

from .router import EventRouter, load_subscriptions


class HealthHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        if self.path == "/health":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"ok")
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format: str, *args: object) -> None:
        return


def main() -> None:
    logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
    subscriptions = load_subscriptions(
        Path(os.getenv("SUBSCRIPTIONS_DIR", "/app/subscriptions"))
    )
    redis = Redis(
        host=os.getenv("REDIS_HOST", "localhost"),
        port=int(os.getenv("REDIS_PORT", "6379")),
        decode_responses=True,
    )
    router = EventRouter(redis, subscriptions)

    Thread(
        target=ThreadingHTTPServer(("0.0.0.0", 8090), HealthHandler).serve_forever,
        daemon=True,
    ).start()

    while True:
        try:
            redis.ping()
            recovered = router.recover()
            if recovered:
                logging.warning("Recovered %s unacknowledged event(s)", recovered)
            break
        except Exception:
            logging.exception("Redis unavailable during startup")
            time.sleep(2)

    while True:
        routed = router.route_one()
        if not routed:
            time.sleep(0.5)


if __name__ == "__main__":
    main()
