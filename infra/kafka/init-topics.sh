#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="kafka:29092"

echo "Waiting for Kafka..."

until /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --list >/dev/null 2>&1; do
  sleep 2
done

echo "Kafka is available. Creating topics..."

for topic in \
  payment.requested.v1 \
  payment.processed.v1 \
  payment.requested.v1-dlt \
  payment.processed.v1-dlt
do
  echo "Creating topic: $topic"

  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$BOOTSTRAP_SERVER" \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1
done

echo "Available topics:"

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --list

echo "Kafka initialization completed."