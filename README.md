# pks-5sem — контрольные работы, 5 семестр

Вариант команды: **«Автосалон»**, основная сущность — **заявка на тест-драйв автомобиля**.

| Работа | Папка |
|---|---|
| КР 1. Консольная информационная система (Java 17, JDBC, PostgreSQL, Apache POI) | [autosalon-testdrive](autosalon-testdrive) |

Следующие работы (КР 2 — JavaFX, КР 3 — Spring + REST API, КР 4 — Android) развивают ту же систему
с той же предметной областью и базой данных.

## Как запустить КР 1

Подробная инструкция — в [autosalon-testdrive/README.md](autosalon-testdrive/README.md). Кратко:

1. Установить JDK 17 или новее, PostgreSQL и IntelliJ IDEA.
2. Выполнить в PostgreSQL `autosalon-testdrive/sql/01_create_database.sql`, затем в базе `autosalon` —
   `autosalon-testdrive/sql/02_schema_and_data.sql`.
3. Указать пароль пользователя `postgres` в `autosalon-testdrive/src/main/resources/db.properties`.
4. Открыть папку `autosalon-testdrive` в IntelliJ IDEA и запустить класс `ru.mirea.autosalon.Main`.
