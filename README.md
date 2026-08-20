# Delivery Tracker

Микросервисная система для управления доставками, курьерами и уведомлениями.

Пользователь создаёт доставку, система автоматически назначает свободного курьера, отслеживает жизненный цикл заказа и создаёт уведомления о каждом важном событии.

Взаимодействие между микросервисами реализовано асинхронно через **Apache Kafka**.

---

## Архитектура

Проект состоит из трёх независимых микросервисов:

```text
                     DELIVERY_CREATED
                           │
                           ▼
┌────────────────┐       Kafka       ┌─────────────────┐
│ delivery       │──────────────────►│ courier         │
│ service        │                   │ service         │
└───────▲────────┘                   └────────┬────────┘
        │                                     │
        │                              COURIER_ASSIGNED
        │                                     │
        └────────────── Kafka ◄───────────────┘
                         │
                         │
                         ▼
                ┌──────────────────────┐
                │ notification-service │
                └──────────────────────┘
```

Каждый микросервис имеет собственную PostgreSQL-базу данных:

```text
delivery-service
      ↓
delivery_db

courier-service
      ↓
courier_db

notification-service
      ↓
notification_db
```

---

## Микросервисы

### Delivery Service

Порт:

```text
8080
```

Отвечает за создание и управление доставками.

Основные возможности:

* создание доставки;
* получение списка доставок;
* получение доставки по ID;
* редактирование данных доставки;
* удаление доставки;
* фильтрация по статусу и курьеру;
* управление жизненным циклом доставки;
* отправка Kafka-событий.

Статусы доставки:

```text
CREATED
COURIER_ASSIGNED
PICKED_UP
COMPLETED
CANCELLED
```

Основной жизненный цикл:

```text
CREATED
   ↓
COURIER_ASSIGNED
   ↓
PICKED_UP
   ↓
COMPLETED
```

---

### Courier Service

Порт:

```text
8081
```

Отвечает за управление курьерами и автоматическое назначение свободного курьера на доставку.

Статусы курьера:

```text
AVAILABLE
BUSY
OFFLINE
```

При получении события `DELIVERY_CREATED` сервис:

1. находит свободного курьера;
2. переводит его в `BUSY`;
3. сохраняет ID текущей доставки;
4. отправляет событие `COURIER_ASSIGNED`.

После завершения доставки:

```text
BUSY
 ↓
AVAILABLE
```

а `currentDeliveryId` снова становится `null`.

---

### Notification Service

Порт:

```text
8082
```

Отвечает за автоматическое создание и хранение уведомлений.

Сервис подписан на Kafka-события:

```text
DELIVERY_CREATED
COURIER_ASSIGNED
DELIVERY_PICKED_UP
DELIVERY_COMPLETED
```

Примеры уведомлений:

```text
Доставка #15 создана

К доставке #15 назначен курьер #3

Курьер забрал доставку #15

Доставка #15 успешно завершена
```

Уведомления нельзя создавать вручную через REST API — они появляются только в результате Kafka-событий.

Для защиты от повторной обработки Kafka-событий используется `eventId`.

---

## Kafka

Используются два основных топика:

```text
delivery.events
courier.events
```

### delivery.events

События:

```text
DELIVERY_CREATED
DELIVERY_PICKED_UP
DELIVERY_COMPLETED
```

### courier.events

События:

```text
COURIER_ASSIGNED
```

Пример структуры события доставки:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "DELIVERY_CREATED",
  "deliveryId": 15,
  "occurredAt": "2026-08-21T01:00:00"
}
```

Пример события назначения курьера:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "COURIER_ASSIGNED",
  "deliveryId": 15,
  "courierId": 3,
  "occurredAt": "2026-08-21T01:00:01"
}
```

---

## Технологии

* Java 25
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Validation
* Spring Kafka
* PostgreSQL
* Flyway
* Apache Kafka
* Docker
* Docker Compose
* Maven
* Lombok
* Spring Boot Actuator
* Springdoc OpenAPI / Swagger
* JUnit 5
* Mockito

---

## Структура проекта

```text
delivery-tracker/
│
├── delivery-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── courier-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── notification-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## Запуск проекта

Для запуска необходим установленный и запущенный Docker Desktop.

### 1. Создать `.env`

В корневой папке проекта:

```text
.env
```

Пример:

```env
POSTGRES_PASSWORD=your_password
```

Настоящий `.env` добавлен в `.gitignore` и не должен попадать в Git.

---

### 2. Запустить систему

Из корня проекта:

```bash
docker compose up -d --build
```

Docker запустит:

```text
delivery-service
courier-service
notification-service

delivery-db
courier-db
notification-db

kafka
```

Всего — 7 контейнеров.

Проверить состояние:

```bash
docker compose ps
```

---

### 3. Остановить систему

```bash
docker compose down
```

PostgreSQL-данные при этом сохраняются в Docker volumes.

---

## Swagger

### Delivery Service

```text
http://localhost:8080/swagger-ui.html
```

### Courier Service

```text
http://localhost:8081/swagger-ui.html
```

### Notification Service

```text
http://localhost:8082/swagger-ui.html
```

---

## Actuator

Проверка состояния сервисов:

```text
http://localhost:8080/actuator/health
http://localhost:8081/actuator/health
http://localhost:8082/actuator/health
```

Также доступны:

```text
/actuator/info
/actuator/metrics
```

---

## Основные REST API

### Delivery Service

```text
POST   /api/deliveries

GET    /api/deliveries
GET    /api/deliveries/{id}

PUT    /api/deliveries/{id}

PATCH  /api/deliveries/{id}/pickup
PATCH  /api/deliveries/{id}/complete
PATCH  /api/deliveries/{id}/cancel

DELETE /api/deliveries/{id}
```

Фильтрация:

```text
GET /api/deliveries?status=CREATED
GET /api/deliveries?courierId=1
```

---

### Courier Service

```text
POST   /api/couriers

GET    /api/couriers
GET    /api/couriers/{id}

PUT    /api/couriers/{id}

PATCH  /api/couriers/{id}/online
PATCH  /api/couriers/{id}/offline

DELETE /api/couriers/{id}
```

Фильтрация:

```text
GET /api/couriers?status=AVAILABLE
```

---

### Notification Service

```text
GET   /api/notifications
GET   /api/notifications/{id}

PATCH /api/notifications/{id}/read
```

Фильтрация:

```text
GET /api/notifications?deliveryId=15
GET /api/notifications?read=false
GET /api/notifications?deliveryId=15&read=false
```

---

## Пример полного сценария

```text
Пользователь создаёт Delivery
        ↓
CREATED
        ↓
DELIVERY_CREATED
        ↓
Kafka
        ↓
Courier Service
        ↓
AVAILABLE Courier найден
        ↓
BUSY
        ↓
COURIER_ASSIGNED
        ↓
Kafka
        ↓
Delivery Service
        ↓
COURIER_ASSIGNED
        ↓
PICKED_UP
        ↓
DELIVERY_PICKED_UP
        ↓
Kafka
        ↓
Notification Service
        ↓
COMPLETED
        ↓
DELIVERY_COMPLETED
        ↓
Kafka
       ↙   ↘
Courier   Notification
Service      Service
   ↓
BUSY → AVAILABLE
```

---

## База данных

Для управления схемой используется **Flyway**.

Каждый сервис имеет собственные SQL-миграции:

```text
src/main/resources/db/migration
```

Например:

```text
V1__create_deliveries_table.sql
V1__create_couriers_table.sql
V1__create_notifications_table.sql
```

Hibernate работает в режиме:

```text
ddl-auto: validate
```

поэтому структура базы изменяется только через миграции Flyway.

---

## Тестирование

Для unit-тестов используются:

* JUnit 5
* Mockito

Бизнес-логика `delivery-service` покрыта unit-тестами с использованием mock-зависимостей.

---

## Особенности проекта

* микросервисная архитектура;
* отдельная БД для каждого сервиса;
* асинхронное взаимодействие через Kafka;
* DTO и Mapper-слой;
* централизованная обработка исключений;
* валидация входных данных;
* контролируемые переходы бизнес-статусов;
* фильтрация данных;
* Flyway migrations;
* Swagger/OpenAPI;
* Actuator;
* Docker Compose;
* идемпотентная обработка уведомлений по `eventId`.
