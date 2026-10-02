FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY eventRouterService/pyproject.toml ./
COPY eventRouterService/src ./src
RUN pip install --no-cache-dir .

COPY contracts/subscriptions ./subscriptions

USER nobody
EXPOSE 8090
CMD ["rental-event-router"]
