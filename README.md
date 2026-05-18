# Magazines Catalog — Backend

REST API для каталога журналов: регистрация пользователей, модерация публикаций, выпуски (PDF), отзывы и избранное.

**Стек:** Kotlin 1.9 · JVM 17 · Ktor 2.3 · PostgreSQL · Exposed · Flyway · Koin · JWT

---

## Быстрый старт

### Требования

- **JDK 17+**
- **Docker** и **Docker Compose** (PostgreSQL + pgAdmin)
- **Gradle** (или используйте `./gradlew` из репозитория)

### 1. Поднять инфраструктуру

```bash
docker compose up -d
```

| Сервис     | Адрес                         | Учётные данные (по умолчанию)   |
|------------|-------------------------------|---------------------------------|
| PostgreSQL | `localhost:5432`              | БД `magazines_catalog`, user `app`, password `app_password` |
| pgAdmin    | http://localhost:5050         | `admin@local.dev` / `admin`     |

При первом запуске приложения Flyway применит миграции из `src/main/resources/db/migration/`.

### 2. Настроить переменные окружения

```bash
cp .env.example .env
```

Отредактируйте `.env` (см. раздел [Настройка `.env`](#настройка-env) ниже).

Перед запуском сервера переменные должны быть доступны процессу JVM. Пример для **Linux / macOS**:

```bash
set -a && source .env && set +a
```

**Windows (PowerShell):**

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
  }
}
```

### 3. Запустить API

```bash
./gradlew run
```

Проверка:

```bash
curl http://localhost:8080/
# → Magazines Catalog API v1.0
```

---

## Настройка `.env`

Шаблон — в [`.env.example`](.env.example). Значения подставляются в [`application.conf`](src/main/resources/application.conf) через переменные окружения.

| Переменная           | По умолчанию (локально)                              | Описание |
|----------------------|------------------------------------------------------|----------|
| `SERVER_PORT`        | `8080`                                               | Порт HTTP-сервера |
| `DATABASE_URL`       | `jdbc:postgresql://localhost:5432/magazines_catalog` | JDBC URL PostgreSQL |
| `DATABASE_USER`      | `app`                                                | Пользователь БД |
| `DATABASE_PASSWORD`  | `app_password`                                       | Пароль БД |
| `STORAGE_PATH`       | `./storage`                                          | Каталог обложек и PDF |
| `JWT_SECRET`         | `dev-secret-change-in-production`                    | Секрет подписи JWT (HS256) |
| `JWT_ISSUER`         | `magazines-catalog`                                  | Issuer токена |
| `JWT_AUDIENCE`       | `magazines-catalog-clients`                          | Audience токена |
| `JWT_REALM`          | `Magazines Catalog API`                              | Realm для auth challenge |

> Для локальной разработки с `docker compose` из этого репозитория достаточно скопировать `.env.example` в `.env` без изменений (кроме `JWT_SECRET`).

### Генерация `JWT_SECRET`

В production **обязательно** замените dev-секрет на случайную строку **не короче 32 символов**.

**Linux / macOS:**

```bash
openssl rand -base64 32
```

**Windows (PowerShell):**

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

Вставьте результат в `.env`:

```env
JWT_SECRET=<сгенерированное-значение>
```

---

## API — все эндпоинты

Базовый URL: `http://localhost:8080`

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| **Система** ||||
| `GET` | `/` | Публичный | Проверка работы API |
| `GET` | `/files/covers/*` | Публичный | Статика обложек журналов |
| `GET` | `/files/pdfs/*` | JWT | Статика PDF выпусков |
| **Аутентификация** (`/api/auth`) ||||
| `POST` | `/api/auth/register` | Публичный | Регистрация `{ email, password, displayName? }` → `{ token, user }` |
| `POST` | `/api/auth/login` | Публичный | Вход `{ email, password }` → `{ token, user }` |
| `GET` | `/api/auth/me` | JWT | Текущий пользователь |
| **Категории** (`/api/categories`) ||||
| `GET` | `/api/categories` | Публичный | Список категорий |
| `POST` | `/api/categories` | Admin | Создать категорию |
| `PUT` | `/api/categories/{id}` | Admin | Обновить категорию |
| `DELETE` | `/api/categories/{id}` | Admin | Удалить категорию |
| **Журналы** (`/api/magazines`) ||||
| `GET` | `/api/magazines` | Публичный | Каталог одобренных журналов. Query: `category`, `search`, `page`, `pageSize` |
| `GET` | `/api/magazines/mine` | JWT | Мои журналы (включая `PENDING`) |
| `GET` | `/api/magazines/{id}` | JWT опционально | Карточка журнала; `PENDING` виден владельцу и admin |
| `POST` | `/api/magazines` | JWT | Создать журнал (`USER` → `PENDING`, `ADMIN` → `APPROVED`) |
| `PUT` | `/api/magazines/{id}` | JWT | Обновить (владелец или admin) |
| `DELETE` | `/api/magazines/{id}` | JWT | Удалить (владелец или admin) |
| `POST` | `/api/magazines/{id}/cover` | JWT | Загрузить обложку (`multipart`, поле `cover`) |
| **Выпуски** ||||
| `GET` | `/api/magazines/{id}/issues` | Публичный | Список выпусков журнала |
| `POST` | `/api/magazines/{id}/issues` | JWT | Создать выпуск (`multipart`: `pdf`, `issueNumber`, …) |
| `DELETE` | `/api/issues/{id}` | JWT | Удалить выпуск (владелец журнала или admin) |
| **Отзывы** ||||
| `GET` | `/api/magazines/{id}/reviews` | Публичный | Отзывы на одобренный журнал |
| `POST` | `/api/magazines/{id}/reviews` | JWT | Создать отзыв `{ rating: 1..5, comment? }` |
| `PUT` | `/api/reviews/{id}` | JWT | Обновить свой отзыв |
| `DELETE` | `/api/reviews/{id}` | JWT | Удалить (автор или admin) |
| **Избранное** ||||
| `GET` | `/api/users/me/favorites` | JWT | Список избранных журналов |
| `POST` | `/api/users/me/favorites/{magazineId}` | JWT | Добавить в избранное |
| `DELETE` | `/api/users/me/favorites/{magazineId}` | JWT | Убрать из избранного |
| **Модерация** (`/api/admin/magazines`) ||||
| `GET` | `/api/admin/magazines/pending` | Admin | Журналы на модерации |
| `POST` | `/api/admin/magazines/{id}/approve` | Admin | Одобрить журнал |
| `POST` | `/api/admin/magazines/{id}/reject` | Admin | Отклонить `{ reason }` |

**Заголовок авторизации** для защищённых маршрутов:

```http
Authorization: Bearer <token>
```

Postman-коллекция: [`docs/Magazines_Catalog_API.postman_collection.json`](docs/Magazines_Catalog_API.postman_collection.json)

---

## Примеры `curl`

Подставьте свой `TOKEN` и `MAGAZINE_ID` из ответов предыдущих шагов.

### Регистрация

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "reader@example.com",
    "password": "secret123",
    "displayName": "Reader"
  }'
```

### Логин

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "reader@example.com",
    "password": "secret123"
  }'
```

Сохраните `token` из ответа:

```bash
export TOKEN="<token из ответа>"
```

### Создание журнала (с токеном)

```bash
curl -s -X POST http://localhost:8080/api/magazines \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Kotlin Monthly",
    "publisher": "JetBrains",
    "categoryId": 1,
    "description": "Журнал о Kotlin"
  }'
```

Обычный пользователь получит журнал со статусом `PENDING`; он появится в публичном каталоге после одобления администратором.

### Текущий пользователь

```bash
curl -s http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## Тесты

Интеграционные тесты используют **Testcontainers** (PostgreSQL в Docker). Docker должен быть запущен.

```bash
./gradlew test
```

Отдельный класс:

```bash
./gradlew test --tests "com.magazines.AuthRoutesTest"
```

| Класс | Что покрывает |
|-------|----------------|
| `AuthRoutesTest` | Регистрация, логин, `/api/auth/me` |
| `CategoryRoutesTest` | Категории |
| `MagazineRoutesTest` | Создание, статусы, публичный каталог |
| `ReviewRoutesTest` | Отзывы, рейтинг |

Сборка с тестами:

```bash
./gradlew build
```

---

## Скриншоты

> Добавьте изображения в `docs/screenshots/` и замените плейсхолдеры ниже.

### Схема базы данных в pgAdmin

1. Откройте http://localhost:5050 и войдите (`admin@local.dev` / `admin`).
2. Добавьте сервер: Host `postgres` (из Docker-сети) или `host.docker.internal` / `localhost` с порта `5432`, user `app`, password `app_password`, database `magazines_catalog`.
3. В дереве: **Databases → magazines_catalog → Schemas → public → Tables**.

![Схема БД — обзор таблиц](docs/screenshots/pgadmin-tables.png)
<!-- TODO: docs/screenshots/pgadmin-tables.png -->

![Схема БД — ER-диаграмма](docs/screenshots/pgadmin-er-diagram.png)
<!-- TODO: docs/screenshots/pgadmin-er-diagram.png -->

---

## Структура проекта

```
src/main/kotlin/com/magazines/
├── Application.kt         # точка входа
├── config/                # конфигурация, DI, БД
├── plugins/               # Ktor: auth, CORS, routing, …
├── db/tables/             # Exposed-таблицы
├── domain/
│   ├── model/             # сущности
│   └── exception/         # доменные исключения
├── data/
│   ├── dto/               # DTO запросов/ответов
│   └── repository/        # репозитории
├── service/               # бизнес-логика
├── routes/                # HTTP-маршруты
└── util/                  # вспомогательные функции
```

## Полезные команды Gradle

| Команда | Описание |
|---------|----------|
| `./gradlew run` | Запуск сервера |
| `./gradlew test` | Только тесты |
| `./gradlew build` | Сборка + тесты |

---

## Аутентификация (кратко)

- Пароли хранятся как **bcrypt**-хэш.
- Токены — **JWT (HS256)**, срок жизни 24 часа.
- Роли: `USER`, `ADMIN` (роль admin задаётся в БД).

---

## Лицензия

Уточните лицензию в репозитории проекта.
