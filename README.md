# Dynamic Security Analysis Server

[![Actions Status](https://github.com/gridsuite/dynamic-security-analysis-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/gridsuite/dynamic-security-analysis-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.gridsuite%3Adynamic-security-analysis-server&metric=coverage)](https://sonarcloud.io/component_measures?id=org.gridsuite%3Adynamic-security-analysis-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

## Description

The **dynamic-security-analysis-server** is a microservice of the [GridSuite](https://github.com/gridsuite) platform dedicated to **dynamic security analysis computation**.

Dynamic security analysis extends a dynamic simulation (time-domain, dynamic-model based) by evaluating the impact of a set of contingencies on top of a previously computed dynamic simulation output state. It relies on the [dynamic-simulation-server](https://github.com/gridsuite/dynamic-simulation-server) result (output state, dynamic model, parameters) as its starting point, then simulates each contingency using a dynamic security analysis provider (Dynawo).

It provides the following capabilities:

- **Run dynamic security analysis computations** starting from an existing dynamic simulation result, using configurable providers (Dynawo).
- **Manage parameter sets** (create, read, update, duplicate, delete) with provider-aware parameter values.
- **Track computation status** and invalidate/stop running computations.
- **Delete results** individually or in bulk.
- **Download debug files** produced by the underlying dynamic security analysis provider.
- Run computations **asynchronously** (via a RabbitMQ message queue).

---

## Technical Stack

- Spring Boot (Web, Data JPA, Actuator, Cloud Stream)
- PostgreSQL
- Liquibase
- RabbitMQ via Spring Cloud Stream
- API documentation: OpenAPI / Swagger (`springdoc`)
- Micrometer / Prometheus
- [gridsuite-computation](https://github.com/gridsuite/computation)
- [powsybl-dynawo](https://github.com/powsybl/powsybl-dynawo) (Dynawo dynamic security analysis provider)

---

## Development Scripts

Build Docker image

```shell
mvn install -DskipTests -Dpowsybl.docker.install
```

Please read [liquibase usage](https://github.com/powsybl/powsybl-parent/#liquibase-usage) for instructions to automatically generate changesets. After you generated a changeset do not forget to add it to git and in `src/main/resources/db/changelog/db.changelog-master.yml`.

---

## Interactions with Other Microservices

```text
┌───────────────────────────────────┐
│  dynamic-security-analysis-server │──► network-store-server      (read network topology)
│                                   │──► actions-server            (resolve contingency lists)
│                                   │──► dynamic-simulation-server (fetch output state, dynamic model and parameters)
│                                   │──► report-server             (post computation functional logs)
└───────────────────────────────────┘
          ▲  ▼
       RabbitMQ (dsa.run / dsa.cancel / dsa.result / dsa.stopped / dsa.cancelfailed / dsa.debug)
```

---

## Asynchronous Execution Flow

1. The controller publishes a message on the `dsa.run` queue.
2. Parallel consumers (`consumeRun1`, `consumeRun2`) process messages concurrently for load balancing.
3. The computation result is published on `dsa.result`.
4. Cancellation of a running computation goes through the `dsa.cancel` queue.
5. Debug information (when debug mode is enabled) is published on `dsa.debug`.
6. Dead-letter queues (`dsa.run.dlx`) and quorum queues ensure reliability.

---

## Parameters

Dynamic security analysis parameters include:

- **Provider** selection (Dynawo).
- **Provider-specific parameter values**, exposed and persisted per parameter set.

Parameter sets can be created, duplicated, updated, retrieved and deleted through the REST API.

---

## Built on gridsuite-computation

The following capabilities are provided by the gridsuite-computation shared library:

- asynchronous run/cancel pipeline,
- transactional result notifications,
- report integration,
- Micrometer observability.

The dynamic-security-analysis-server itself focuses on dynamic-security-analysis-specific logic (parameters, contingency resolution, dynamic simulation context retrieval, provider integration) and delegates the common computation infrastructure to this lib.

---

## Useful Links

You can find [information on Dynawo here](https://dynawo.github.io/).
