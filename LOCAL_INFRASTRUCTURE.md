# Local infrastructure

The infrastructure-only Compose file starts the databases, RabbitMQ, Elasticsearch,
Kibana, and a Fluent Bit collector without building the Java services.

## Start

```bash
docker compose -f docker-compose.infrastructure.yml up -d
```

Add pgAdmin when a browser UI for PostgreSQL is useful:

```bash
docker compose -f docker-compose.infrastructure.yml --profile tools up -d
```

Check container readiness and logs:

```bash
docker compose -f docker-compose.infrastructure.yml ps
docker compose -f docker-compose.infrastructure.yml logs -f
```

## Local endpoints

| Component | Address | Credentials/database |
| --- | --- | --- |
| Elasticsearch | http://localhost:9200 | Security disabled for local use |
| Kibana | http://localhost:5601 | Create data view `microservices-*` |
| RabbitMQ UI | http://localhost:15672 | `guest` / `guest` |
| pgAdmin (tools profile) | http://localhost:5050 | `admin@local.dev` / `Passw0rd!` |
| Identity SQL Server | `localhost:1434` | `sa` / `Passw0rd!`, database `master` |
| Ordering SQL Server | `localhost:1435` | `sa` / `Passw0rd!`, database `master` |
| Product MySQL | `localhost:3306` | `root` / `Passw0rd!`, database `productdb` |
| Customer PostgreSQL | `localhost:5432` | `postgres` / `Passw0rd!`, database `customerdb` |
| Basket Redis | `localhost:6379` | No password |
| Inventory MongoDB | `localhost:27017` | No password; databases `inventorydb` and `jobdb` |

Fluent Bit reads Docker JSON logs and sends them to daily Elasticsearch indices named
`microservices-YYYY.MM.DD`. In Kibana, open **Stack Management > Index Patterns**, create
`microservices-*`, select `@timestamp`, then use **Discover**.

Quick Elasticsearch checks:

```bash
curl http://localhost:9200/_cluster/health?pretty
curl http://localhost:9200/_cat/indices/microservices-*?v
```

The Java services already default to these localhost ports, so they can run from the IDE
after the infrastructure is healthy. Fluent Bit collects services started as Docker
containers; an IDE-started service continues to log to the IDE console. Run the full
Compose files under `Solution Items` when its application logs should also appear in Kibana.

## Stop

```bash
docker compose -f docker-compose.infrastructure.yml down
```

To also delete all local database and log data:

```bash
docker compose -f docker-compose.infrastructure.yml down -v
```
