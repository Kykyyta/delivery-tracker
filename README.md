# Delivery Tracker

`Delivery Tracker` — учебный pet-project на Java/Spring Boot, реализованный как микросервисная система для управления доставками, курьерами, пользователями и уведомлениями.

Пользователь регистрируется и получает JWT, создаёт доставку, система автоматически назначает свободного курьера через Apache Kafka, отслеживает жизненный цикл доставки и создаёт персональные уведомления о важных событиях.

Проект демонстрирует работу с **Spring Security + JWT**, ролевой и ресурсной авторизацией, асинхронным взаимодействием через **Kafka**, отдельной PostgreSQL-базой для каждого сервиса, Flyway-миграциями и Docker Compose.

---

## Основные возможности

- регистрация и аутентификация пользователей;
- JWT-аутентификация без серверной HTTP-сессии;
- роли `CUSTOMER`, `COURIER`, `ADMIN`;
- role-based authorization через Spring Security;
- ownership authorization по `userId` из JWT;
- создание и управление доставками;
- автоматическое назначение свободного курьера;
- управление жизненным циклом доставки;
- автоматическое освобождение курьера после завершения или отмены доставки;
- персональные уведомления клиента;
- асинхронное взаимодействие сервисов через Apache Kafka;
- защита Notification Service от повторной обработки одного и того же события по `eventId`;
- валидация входных данных и централизованная обработка API-ошибок;
- отдельная PostgreSQL-база для каждого микросервиса;
- Flyway-миграции;
- контейнеризация всех сервисов и инфраструктуры через Docker Compose.

---

## Архитектура

Проект состоит из четырёх Spring Boot микросервисов:

```text
                           ┌─────────────────┐
                           │   auth-service  │
                           │      :8083      │
                           └────────┬────────┘
                                    │
                                    │ JWT
                                    ▼
        ┌────────────────────────────────────────────────────┐
        │                    API clients                     │
        └──────────────┬──────────────────┬──────────────────┘
                       │                  │
                       ▼                  ▼
              ┌─────────────────┐  ┌─────────────────┐
              │ delivery-service│  │ courier-service │
              │      :8080      │  │      :8081      │
              └────────┬────────┘  └────────┬────────┘
                       │                    │
                       │ delivery.events    │ courier.events
                       │                    │
                       └──────────┬─────────┘
                                  ▼
                         ┌─────────────────┐
                         │  Apache Kafka   │
                         │      :9092      │
                         └────────┬────────┘
                                  │
                                  ▼
                       ┌──────────────────────┐
                       │ notification-service │
                       │        :8082         │
                       └──────────────────────┘
```

Каждый сервис владеет своей базой данных:

```text
auth-service          -> delivery_auth
delivery-service      -> delivery_db
courier-service       -> courier_db
notification-service  -> notification_db
```

Между базами данных нет общих JPA-связей. Связь между доменами выполняется через идентификаторы и Kafka-события.

---

## Микросервисы

### Auth Service — `8083`

Отвечает за пользователей, пароли, роли и JWT.

Основные функции:

- публичная регистрация `CUSTOMER`;
- вход по email/password;
- хранение паролей через BCrypt;
- генерация JWT;
- claims `userId` и `role` внутри JWT;
- создание аккаунтов с ролью `COURIER` только администратором;
- получение данных текущего пользователя через `/api/auth/me`.

JWT подписывается алгоритмом **HS256**. Один и тот же `JWT_SECRET` используется `auth-service` для подписи и остальными сервисами для проверки токена.

### Delivery Service — `8080`

Отвечает за доставки.

Статусы:

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

Отмена разрешена из:

```text
CREATED -> CANCELLED
COURIER_ASSIGNED -> CANCELLED
```

Физическое удаление доставки доступно только `ADMIN` и только для статусов:

```text
CANCELLED
COMPLETED
```

Это предотвращает удаление активной доставки, пока курьер ещё связан с ней.

### Courier Service — `8081`

Отвечает за профили курьеров и их рабочее состояние.

Статусы:

```text
AVAILABLE
BUSY
OFFLINE
```

После события `DELIVERY_CREATED` сервис ищет первого доступного связанного курьера:

```text
AVAILABLE
   ↓
BUSY
currentDeliveryId = deliveryId
```

После `DELIVERY_COMPLETED` или `DELIVERY_CANCELLED` курьер освобождается:

```text
BUSY
   ↓
AVAILABLE
currentDeliveryId = null
```

Профиль курьера связан с пользователем `auth-service` через `Courier.userId`.

### Notification Service — `8082`

Создаёт и хранит уведомления на основании Kafka-событий.

Типы уведомлений:

```text
DELIVERY_CREATED
COURIER_ASSIGNED
DELIVERY_PICKED_UP
DELIVERY_COMPLETED
DELIVERY_CANCELLED
```

Уведомление содержит `customerId`, поэтому `CUSTOMER` может читать и отмечать прочитанными только собственные уведомления.

Повторная обработка одного и того же события предотвращается через уникальный `eventId`.

---

## Spring Security и JWT

Все REST API, кроме регистрации, логина и health endpoint, защищены Spring Security.

После успешного логина `auth-service` возвращает JWT:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

JWT содержит данные пользователя, необходимые для авторизации:

```json
{
  "sub": "customer@test.com",
  "userId": 10,
  "role": "CUSTOMER"
}
```

В защищённых запросах токен передаётся в заголовке:

```http
Authorization: Bearer <JWT>
```

В проекте используются два уровня авторизации:

```text
1. Role-based authorization
   Spring Security проверяет CUSTOMER / COURIER / ADMIN.

2. Ownership authorization
   Service layer сравнивает userId из JWT с владельцем ресурса.
```

Примеры ownership:

```text
CUSTOMER:
JWT.userId == Delivery.customerId
JWT.userId == Notification.customerId

COURIER:
JWT.userId == Courier.userId
JWT.userId == Delivery.courierUserId
```

---

## Роли и права доступа

### Delivery API

| Endpoint | CUSTOMER | COURIER | ADMIN |
|---|---|---|---|
| `POST /api/deliveries` | ✅ | ❌ | ✅ |
| `GET /api/deliveries` | ✅ только свои | ✅ только назначенные | ✅ все |
| `GET /api/deliveries/{id}` | ✅ только свою | ✅ только назначенную | ✅ любую |
| `PUT /api/deliveries/{id}` | ✅ только свою | ❌ | ✅ |
| `PATCH /api/deliveries/{id}/pickup` | ❌ | ✅ только назначенную | ✅ |
| `PATCH /api/deliveries/{id}/complete` | ❌ | ✅ только назначенную | ✅ |
| `PATCH /api/deliveries/{id}/cancel` | ✅ только свою | ❌ | ✅ |
| `DELETE /api/deliveries/{id}` | ❌ | ❌ | ✅ `CANCELLED/COMPLETED` |

### Courier API

| Endpoint | CUSTOMER | COURIER | ADMIN |
|---|---|---|---|
| `POST /api/couriers` | ❌ | ❌ | ✅ |
| `GET /api/couriers` | ❌ | ✅ только свой профиль | ✅ все |
| `GET /api/couriers/{id}` | ❌ | ✅ только свой | ✅ |
| `PUT /api/couriers/{id}` | ❌ | ✅ только свой | ✅ |
| `PATCH /api/couriers/{id}/online` | ❌ | ✅ только свой | ✅ |
| `PATCH /api/couriers/{id}/offline` | ❌ | ✅ только свой | ✅ |
| `DELETE /api/couriers/{id}` | ❌ | ❌ | ✅ если курьер не `BUSY` |

### Notification API

| Endpoint | CUSTOMER | COURIER | ADMIN |
|---|---|---|---|
| `GET /api/notifications` | ✅ только свои | ❌ | ✅ все |
| `GET /api/notifications/{id}` | ✅ только своё | ❌ | ✅ |
| `PATCH /api/notifications/{id}/read` | ✅ только своё | ❌ | ✅ |

---

## Kafka

Используются два топика:

```text
delivery.events
courier.events
```

### `delivery.events`

События:

```text
DELIVERY_CREATED
DELIVERY_PICKED_UP
DELIVERY_COMPLETED
DELIVERY_CANCELLED
```

Структура:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "DELIVERY_CREATED",
  "deliveryId": 15,
  "customerId": 10,
  "occurredAt": "2026-08-26T20:00:00"
}
```

`courier-service` использует `DELIVERY_CREATED` для назначения курьера, а `DELIVERY_COMPLETED` и `DELIVERY_CANCELLED` — для его освобождения.

`notification-service` использует события доставки для создания персональных уведомлений.

### `courier.events`

События:

```text
COURIER_ASSIGNED
```

Структура:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "COURIER_ASSIGNED",
  "deliveryId": 15,
  "courierId": 3,
  "courierUserId": 20,
  "customerId": 10,
  "occurredAt": "2026-08-26T20:00:01"
}
```

`delivery-service` сохраняет `courierId` и `courierUserId`, после чего переводит доставку в `COURIER_ASSIGNED`.

`notification-service` создаёт уведомление о назначении курьера.

---

## Технологии

| Technology | Назначение |
|---|---|
| Java 25 | основной язык |
| Spring Boot 4.1.x | приложение и конфигурация |
| Spring Web MVC | REST API |
| Spring Security | authentication / authorization |
| OAuth2 Resource Server | проверка Bearer JWT |
| BCrypt | хеширование паролей |
| Spring Data JPA / Hibernate | работа с БД |
| PostgreSQL 17 | базы данных |
| Flyway | миграции схемы |
| Apache Kafka | асинхронные события |
| Spring Kafka | producer / consumer |
| Jakarta Validation | валидация DTO |
| Spring Boot Actuator | health/info/metrics |
| Springdoc OpenAPI | описание API бизнес-сервисов |
| JUnit 5 | тестирование |
| Mockito | unit-тесты |
| Maven | сборка |
| Docker / Docker Compose | контейнеризация и запуск инфраструктуры |

---

## Структура проекта

```text
delivery-tracker/
│
├── auth-service/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
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
└── README.md
```

---

## Docker Compose

Полный запуск создаёт **9 контейнеров**:

```text
4 Spring Boot services
4 PostgreSQL databases
1 Apache Kafka broker
```

Сервисы:

```text
auth-service          http://localhost:8083
delivery-service      http://localhost:8080
courier-service       http://localhost:8081
notification-service  http://localhost:8082
Kafka                 localhost:9092
```

---

## Быстрый запуск через Docker

### 1. Клонировать репозиторий

```bash
git clone <repository-url>
cd delivery-tracker
```

### 2. Создать `.env`

PowerShell:

```powershell
copy .env.example .env
```

Linux/macOS:

```bash
cp .env.example .env
```

Содержимое:

```env
POSTGRES_PASSWORD=your_password
JWT_SECRET=your_base64_jwt_secret
```

`JWT_SECRET` должен быть Base64-строкой из как минимум 32 случайных байт.

Пример генерации в PowerShell:

```powershell
[convert]::ToBase64String([byte[]](1..32 | foreach-object { get-random -minimum 0 -maximum 256 }))
```

Или через OpenSSL:

```bash
openssl rand -base64 32
```

Один и тот же `JWT_SECRET` автоматически передаётся всем четырём сервисам через Docker Compose.

### 3. Запустить проект

```bash
docker compose up --build -d
```

Проверить контейнеры:

```bash
docker compose ps
```

Посмотреть логи:

```bash
docker compose logs -f
```

### 4. Проверить health endpoints

```text
http://localhost:8083/actuator/health
http://localhost:8080/actuator/health
http://localhost:8081/actuator/health
http://localhost:8082/actuator/health
```

Ожидаемый ответ:

```json
{
  "status": "UP"
}
```

### 5. Остановить проект

```bash
docker compose down
```

Удаление контейнеров **вместе с PostgreSQL volumes и данными**:

```bash
docker compose down -v
```

---

## Первый ADMIN

Публичный endpoint `/api/auth/register` всегда создаёт пользователя с ролью `CUSTOMER`.

Для первого администратора в dev-окружении сначала зарегистрируйте обычного пользователя:

```http
POST http://localhost:8083/api/auth/register
Content-Type: application/json
```

```json
{
  "name": "Admin",
  "email": "admin@test.com",
  "password": "password123"
}
```

После этого измените роль в `auth-db`:

```bash
docker compose exec auth-db psql -U postgres -d delivery_auth -c "UPDATE users SET role = 'ADMIN' WHERE email = 'admin@test.com';"
```

Проверка:

```bash
docker compose exec auth-db psql -U postgres -d delivery_auth -c "SELECT id, name, email, role FROM users;"
```

После изменения роли необходимо **залогиниться заново**, потому что ранее выданный JWT продолжает содержать старую роль.

---

## Пример аутентификации

### Регистрация CUSTOMER

```http
POST http://localhost:8083/api/auth/register
Content-Type: application/json
```

```json
{
  "name": "Customer One",
  "email": "customer1@test.com",
  "password": "password123"
}
```

### Login

```http
POST http://localhost:8083/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "customer1@test.com",
  "password": "password123"
}
```

Ответ:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Текущий пользователь

```http
GET http://localhost:8083/api/auth/me
Authorization: Bearer <JWT>
```

Пример:

```json
{
  "email": "customer1@test.com",
  "userId": 1,
  "role": "CUSTOMER"
}
```

---

## Создание COURIER

Создание курьера состоит из двух шагов.

### 1. ADMIN создаёт auth-аккаунт COURIER

```http
POST http://localhost:8083/api/auth/couriers
Authorization: Bearer <ADMIN_JWT>
Content-Type: application/json
```

```json
{
  "name": "Courier One",
  "email": "courier1@test.com",
  "password": "password123"
}
```

Из ответа нужно взять `id` созданного пользователя.

### 2. ADMIN создаёт Courier profile

```http
POST http://localhost:8081/api/couriers
Authorization: Bearer <ADMIN_JWT>
Content-Type: application/json
```

```json
{
  "userId": 3,
  "name": "Courier One",
  "phone": "+79990000002"
}
```

После этого `Courier.userId` связан с `User.id`, а курьер может войти через `auth-service` и использовать свой JWT.

---

## Пример полного сценария

### CUSTOMER создаёт доставку

```http
POST http://localhost:8080/api/deliveries
Authorization: Bearer <CUSTOMER_JWT>
Content-Type: application/json
```

```json
{
  "customerName": "Customer One",
  "customerPhone": "+79990000001",
  "pickupAddress": "Moscow, Tverskaya 1",
  "deliveryAddress": "Moscow, Arbat 10"
}
```

`customerId` не передаётся клиентом. Он берётся из доверенного `userId` внутри JWT.

Дальше система автоматически выполняет цепочку:

```text
CUSTOMER creates Delivery
        ↓
Delivery = CREATED
        ↓
DELIVERY_CREATED -> Kafka
        ↓
Courier Service selects AVAILABLE courier
        ↓
Courier = BUSY
        ↓
COURIER_ASSIGNED -> Kafka
        ↓
Delivery = COURIER_ASSIGNED
        ↓
Notification = COURIER_ASSIGNED
```

### Назначенный COURIER забирает доставку

```http
PATCH http://localhost:8080/api/deliveries/{id}/pickup
Authorization: Bearer <COURIER_JWT>
```

```text
COURIER_ASSIGNED -> PICKED_UP
```

### Назначенный COURIER завершает доставку

```http
PATCH http://localhost:8080/api/deliveries/{id}/complete
Authorization: Bearer <COURIER_JWT>
```

```text
PICKED_UP -> COMPLETED
Courier BUSY -> AVAILABLE
```

### CUSTOMER получает свои уведомления

```http
GET http://localhost:8082/api/notifications
Authorization: Bearer <CUSTOMER_JWT>
```

### CUSTOMER отмечает уведомление прочитанным

```http
PATCH http://localhost:8082/api/notifications/{id}/read
Authorization: Bearer <CUSTOMER_JWT>
```

---

## Фильтрация

Delivery Service:

```http
GET /api/deliveries?status=CREATED
GET /api/deliveries?courierId=1
GET /api/deliveries?status=COMPLETED&courierId=1
```

Для `CUSTOMER` и `COURIER` ownership имеет приоритет: сервис всё равно возвращает только разрешённые текущему пользователю доставки.

Courier Service:

```http
GET /api/couriers?status=AVAILABLE
```

Notification Service:

```http
GET /api/notifications?deliveryId=15
GET /api/notifications?read=false
GET /api/notifications?deliveryId=15&read=true
```

Для `CUSTOMER` запросы всегда дополнительно ограничиваются его `customerId`.

---

## HTTP ошибки

API использует стандартные HTTP status codes:

```text
400 Bad Request    — ошибка валидации
401 Unauthorized   — JWT отсутствует, истёк или невалиден
403 Forbidden      — роль или ownership не разрешают операцию
404 Not Found      — ресурс не найден
409 Conflict       — конфликт бизнес-состояния или дубликат данных
```

Примеры `409 Conflict`:

```text
повторная регистрация одного email;
повторный phone/userId курьера;
недопустимый переход статуса;
попытка удалить активную доставку;
попытка удалить BUSY-курьера.
```

---

## Локальный запуск через IntelliJ IDEA

Для локального запуска приложения используют стандартные значения:

```text
PostgreSQL -> localhost:5432
Kafka      -> localhost:9092
```

В `Run/Debug Configuration` каждого Spring Boot Application необходимо передать environment variables:

```text
DB_PASSWORD=<postgres password>
JWT_SECRET=<same Base64 JWT secret for all services>
```

При необходимости можно переопределить:

```text
DB_URL
DB_USERNAME
KAFKA_BOOTSTRAP_SERVERS
SERVER_PORT
```

Для Docker Compose environment variables задаются отдельно через `.env`.

---

## Базы данных и Flyway

Hibernate работает в режиме:

```text
ddl-auto=validate
```

Схема создаётся и изменяется только через Flyway migrations.

Основные миграции:

```text
auth-service
V1  users

courier-service
V1  couriers
V2  user_id

delivery-service
V1  deliveries
V2  customer_id
V3  courier_user_id

notification-service
V1  notifications
V2  customer_id
```

---

## Тестирование

В проекте используются JUnit 5 и Mockito.

`DeliveryServiceTest` покрывает в том числе:

```text
создание доставки с customerId;
CUSTOMER ownership;
COURIER ownership;
ADMIN access;
назначение курьера;
pickup / complete / cancel;
Kafka producer calls;
валидные и невалидные переходы статусов;
удаление только CANCELLED / COMPLETED доставок.
```

Docker images собираются с `-DskipTests`, поэтому тестирование можно запускать отдельно из IDE или Maven в подготовленном окружении.

---

## Безопасность секретов

Файл `.env` не должен попадать в Git.

В репозитории хранится только шаблон:

```text
.env.example
```

Никогда не публикуйте реальные значения:

```text
POSTGRES_PASSWORD
JWT_SECRET
```

Если проект передаётся архивом, убедитесь, что настоящий `.env` также удалён из архива.

---

## Итог

Проект реализует полный учебный сценарий микросервисной backend-системы:

```text
authentication
        ↓
JWT + roles
        ↓
resource ownership
        ↓
delivery lifecycle
        ↓
Kafka events
        ↓
automatic courier assignment
        ↓
personal notifications
        ↓
Dockerized infrastructure
```

Основной акцент проекта — разделение ответственности между микросервисами, безопасная авторизация на уровне ролей и ресурсов, асинхронное взаимодействие через Kafka и воспроизводимый запуск всего окружения через Docker Compose.
