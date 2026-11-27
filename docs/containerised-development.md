# Containerised development

This guide covers running the Kafka Payment Lab with Docker Compose. The project is an educational simulator: it does not move real money or connect to a payment provider.

## Prerequisites

- Docker Engine or Docker Desktop.
- Docker Compose v2 (`docker compose` command).
- For running Gradle tasks on the host: JDK 21 and Gradle compatible with the project build. The repository uses Gradle Kotlin DSL.

## Configure local environment

From the repository root:

```bash
cp audit-service/.env.example audit-service/.env
cp infra/kafka/.env.example infra/kafka/.env
cp infra/postgres/.env.example infra/postgres/.env
cp payment-api/.env.example payment-api/.env
cp payment-processor/.env.example payment-processor/.env
```

Review `.env` and change the local PostgreSQL credentials if desired. The sample values are for local development only. Do not use them for a hosted deployment or commit your `.env` file.

The Compose configuration supplies defaults for PostgreSQL credentials and for `APP_SECURITY_ENABLED`. JWT protection for the admin endpoint is disabled by default. To enable it, set an OIDC issuer and enable security in `.env`:

```dotenv
APP_SECURITY_ENABLED=true
```

The token presented to the admin endpoint must contain the `payments.admin` scope. Use an issuer and test token from a development identity provider; do not put client secrets or signing keys in this repository.

## Build and start the stack

```bash
docker compose up --build -d
docker compose ps
```

Compose starts PostgreSQL and Kafka, runs the one-shot `kafka-init` service to create the application topics, and then starts the three Spring Boot applications. The applications use the Compose network addresses `postgres:5432` and `kafka:29092`.

To follow the startup logs:

```bash
docker compose logs -f postgres kafka kafka-init payment-api payment-processor audit-service
```

## Services and ports

| Service | Host URL / port | Purpose |
|---|---|---|
| Payment API | `http://localhost:8080` | Submit and query simulated payments |
| Payment Processor | `http://localhost:8081` | Processor application health/metrics |
| Audit Service | `http://localhost:8082` | Audit application health/metrics |
| PostgreSQL | `localhost:5432` | Three application databases in one local server |
| Kafka | `localhost:9092` | Host-accessible Kafka listener |
| Prometheus | `http://localhost:9090` | Optional metrics scraper |
| Grafana | `http://localhost:3000` | Optional dashboards |

Inside the Compose network, applications connect to Kafka at `kafka:29092`. Applications run directly on your host should use `localhost:9092` and `localhost:5432` instead.

Each Spring Boot application exposes Actuator health and Prometheus endpoints on its configured HTTP port:

- `GET /actuator/health`
- `GET /actuator/health/readiness`
- `GET /actuator/prometheus`

For example, the Payment API health endpoint is `http://localhost:8080/actuator/health`.

## Submit a simulated payment

```bash
curl -i -X POST http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -d '{"amount":12.50,"currency":"GBP"}'
```

Use the payment ID returned by the API to query its state:

```bash
curl -i http://localhost:8080/payments/<PAYMENT_ID>
```

The initial state is expected to be `PENDING`; after the processor handles the request and the API consumes the result event, the simulated decision is reflected in the payment state. The admin search endpoint is `GET /admin/payments`; when JWT security is enabled it requires the `payments.admin` scope.

## Optional Prometheus and Grafana

Start the observability profile after (or alongside) the application stack:

```bash
docker compose --profile observability up --build -d
```

Open Prometheus at `http://localhost:9090` and Grafana at `http://localhost:3000`. The sample Grafana credentials are `admin` / `admin`; change them before exposing Grafana beyond your local machine. The initial dashboard covers the metrics provisioned by this project. Kafka consumer-group lag is not included in this first iteration.

## Inspect Kafka topics and consumer groups

List topics from inside the Kafka container:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

Describe a topic:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe --topic payment.requested.v1
```

List consumer groups:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --list
```

Describe a group (use the group ID configured by the corresponding application):

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group payment-processing
```

## Stop and reset

Stop and remove containers and the Compose network while retaining named database/Kafka volumes:

```bash
docker compose down
```

To remove the persisted PostgreSQL and Kafka data as well (destructive):

```bash
docker compose down -v
```

## Run applications on the host instead

Start only the infrastructure services:

```bash
docker compose up -d postgres kafka kafka-init
```

Wait for `kafka-init` to finish, then run each application in a separate terminal:

```bash
gradle :payment-api:bootRun
gradle :payment-processor:bootRun
gradle :audit-service:bootRun
```

Host-run applications should connect to `localhost:5432` and `localhost:9092`; do not use the Compose-only hostnames `postgres` and `kafka` from your host shell.

## Troubleshooting

### Application logs report `UNKNOWN_TOPIC_OR_PARTITION`

Check that Kafka is healthy and that the topic initialisation container completed successfully:

```bash
docker compose ps -a
docker compose logs kafka kafka-init
```

If topic initialisation failed, inspect the error, then rerun it after Kafka is healthy:

```bash
docker compose up -d kafka
# after Kafka reports healthy:
docker compose run --rm kafka-init
```

### Applications cannot connect to Kafka

- From another Compose container, use `kafka:29092`.
- From an application running on your host, use `localhost:9092`.
- Inspect the broker and application logs with `docker compose logs kafka payment-api payment-processor audit-service`.

### Applications cannot connect to PostgreSQL

- From a Compose container, use `postgres:5432`.
- From a host-run application, use `localhost:5432`.
- Check readiness with `docker compose ps` and `docker compose logs postgres`.

### A clean local reset is needed

`docker compose down -v` removes the named PostgreSQL and Kafka data volumes. This deletes local database records and Kafka topic data. Use it only when you are happy to discard the local state.

### A port is already in use

Stop the process using the conflicting host port, or change the relevant host-side port mapping in `compose.yml`. Keep the internal service ports and advertised Kafka listener addresses consistent with the applications' connection settings.

## Notes and limitations

This setup is intended for local development and portfolio demonstration. It uses a single Kafka broker and one PostgreSQL server hosting separate databases. It is not a highly available deployment. Do not expose the services publicly without appropriate authentication, TLS, secret management, network restrictions, backups and operational monitoring.
