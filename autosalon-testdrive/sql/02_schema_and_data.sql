-- =====================================================================
--  Автосалон: заявки на тест-драйв автомобилей
--  Скрипт 2 из 2: таблицы, ограничения и начальные тестовые данные.
--
--  Выполнять в базе autosalon:
--    psql -U postgres -d autosalon -f sql/02_schema_and_data.sql
--  или в pgAdmin: Query Tool на базе autosalon -> открыть файл -> Execute (F5).
--
--  Скрипт можно запускать повторно: он удаляет таблицы и создаёт их заново.
--  Так удобно вернуть базу в исходное состояние, например перед защитой.
--  Даты тест-драйвов отсчитываются от текущей даты (CURRENT_DATE), поэтому
--  в базе всегда есть и прошедшие, и предстоящие тест-драйвы.
-- =====================================================================

SET client_encoding = 'UTF8';
-- Не показывать замечания вида «таблица не существует, пропускается» при первом запуске
SET client_min_messages = WARNING;

BEGIN;

DROP TABLE IF EXISTS test_drive_requests;
DROP TABLE IF EXISTS cars;
DROP TABLE IF EXISTS clients;

-- ---------------------------------------------------------------------
--  Клиенты автосалона
-- ---------------------------------------------------------------------
CREATE TABLE clients
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    full_name      VARCHAR(150) NOT NULL,
    phone          VARCHAR(12)  NOT NULL,
    email          VARCHAR(100),
    birth_date     DATE         NOT NULL,
    license_number VARCHAR(10)  NOT NULL,
    registered_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_clients PRIMARY KEY (id),
    CONSTRAINT uq_clients_phone UNIQUE (phone),
    CONSTRAINT uq_clients_email UNIQUE (email),
    CONSTRAINT uq_clients_license_number UNIQUE (license_number),
    CONSTRAINT ck_clients_full_name CHECK (char_length(btrim(full_name)) >= 2),
    CONSTRAINT ck_clients_phone CHECK (phone ~ '^\+7[0-9]{10}$'),
    CONSTRAINT ck_clients_email CHECK (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT ck_clients_license_number CHECK (license_number ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clients_birth_date CHECK (birth_date >= DATE '1900-01-01')
);

COMMENT ON TABLE clients IS 'Клиенты автосалона';
COMMENT ON COLUMN clients.full_name IS 'ФИО клиента';
COMMENT ON COLUMN clients.phone IS 'Телефон в формате +7XXXXXXXXXX';
COMMENT ON COLUMN clients.email IS 'Электронная почта (необязательно)';
COMMENT ON COLUMN clients.birth_date IS 'Дата рождения (тест-драйв только с 18 лет)';
COMMENT ON COLUMN clients.license_number IS 'Номер водительского удостоверения, 10 цифр';
COMMENT ON COLUMN clients.registered_at IS 'Дата и время регистрации клиента';

-- ---------------------------------------------------------------------
--  Автомобили автосалона
-- ---------------------------------------------------------------------
CREATE TABLE cars
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY,
    brand            VARCHAR(50)   NOT NULL,
    model            VARCHAR(50)   NOT NULL,
    manufacture_year INTEGER       NOT NULL,
    vin              VARCHAR(17)   NOT NULL,
    color            VARCHAR(30)   NOT NULL,
    price            NUMERIC(12, 2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',

    CONSTRAINT pk_cars PRIMARY KEY (id),
    CONSTRAINT uq_cars_vin UNIQUE (vin),
    CONSTRAINT ck_cars_manufacture_year CHECK (manufacture_year BETWEEN 1990 AND 2100),
    CONSTRAINT ck_cars_vin CHECK (vin ~ '^[A-HJ-NPR-Z0-9]{17}$'),
    CONSTRAINT ck_cars_price CHECK (price > 0),
    CONSTRAINT ck_cars_status CHECK (status IN ('AVAILABLE', 'IN_SERVICE', 'SOLD'))
);

COMMENT ON TABLE cars IS 'Автомобили автосалона';
COMMENT ON COLUMN cars.vin IS 'VIN: 17 символов, латинские буквы (кроме I, O, Q) и цифры';
COMMENT ON COLUMN cars.price IS 'Цена в рублях';
COMMENT ON COLUMN cars.status IS 'AVAILABLE - в наличии, IN_SERVICE - на обслуживании, SOLD - продан';

-- ---------------------------------------------------------------------
--  Заявки на тест-драйв (основная сущность)
--  client_id -> clients.id, car_id -> cars.id
-- ---------------------------------------------------------------------
CREATE TABLE test_drive_requests
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY,
    client_id        BIGINT      NOT NULL,
    car_id           BIGINT      NOT NULL,
    scheduled_at     TIMESTAMP   NOT NULL,
    duration_minutes INTEGER     NOT NULL DEFAULT 30,
    status           VARCHAR(20) NOT NULL DEFAULT 'NEW',
    comment          VARCHAR(500),
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_test_drive_requests PRIMARY KEY (id),
    CONSTRAINT fk_test_drive_requests_client FOREIGN KEY (client_id)
        REFERENCES clients (id) ON DELETE RESTRICT,
    CONSTRAINT fk_test_drive_requests_car FOREIGN KEY (car_id)
        REFERENCES cars (id) ON DELETE RESTRICT,
    CONSTRAINT ck_test_drive_requests_duration CHECK (duration_minutes BETWEEN 15 AND 120),
    CONSTRAINT ck_test_drive_requests_status
        CHECK (status IN ('NEW', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT ck_test_drive_requests_updated_at CHECK (updated_at >= created_at)
);

COMMENT ON TABLE test_drive_requests IS 'Заявки клиентов на тест-драйв автомобилей';
COMMENT ON COLUMN test_drive_requests.scheduled_at IS 'Дата и время начала тест-драйва';
COMMENT ON COLUMN test_drive_requests.duration_minutes IS 'Длительность тест-драйва, 15-120 минут';
COMMENT ON COLUMN test_drive_requests.status IS
    'NEW - новая, CONFIRMED - подтверждена, COMPLETED - завершена, CANCELLED - отменена, NO_SHOW - неявка';

-- Индексы для частых запросов: заявки клиента, заявки по автомобилю, поиск по дате
CREATE INDEX idx_test_drive_requests_client ON test_drive_requests (client_id);
CREATE INDEX idx_test_drive_requests_car ON test_drive_requests (car_id);
CREATE INDEX idx_test_drive_requests_scheduled_at ON test_drive_requests (scheduled_at);

-- Один автомобиль не может иметь две активные заявки с одинаковым временем начала
CREATE UNIQUE INDEX uq_test_drive_requests_active_slot
    ON test_drive_requests (car_id, scheduled_at)
    WHERE status IN ('NEW', 'CONFIRMED');

-- ---------------------------------------------------------------------
--  Начальные тестовые данные
-- ---------------------------------------------------------------------
INSERT INTO clients (full_name, phone, email, birth_date, license_number, registered_at)
VALUES ('Иванов Алексей Сергеевич', '+79161234501', 'a.ivanov@mail.ru', DATE '1990-04-12', '7712345601',
        CURRENT_DATE - 120 + TIME '10:15'),
       ('Смирнова Екатерина Андреевна', '+79262345602', 'e.smirnova@yandex.ru', DATE '1995-08-23', '7723456702',
        CURRENT_DATE - 95 + TIME '12:40'),
       ('Кузнецов Дмитрий Игоревич', '+79033456703', 'd.kuznetsov@gmail.com', DATE '1987-01-30', '5034567803',
        CURRENT_DATE - 80 + TIME '16:05'),
       ('Попова Анна Викторовна', '+79854567804', 'anna.popova@mail.ru', DATE '2001-11-05', '7745678904',
        CURRENT_DATE - 60 + TIME '11:30'),
       ('Васильев Михаил Олегович', '+79165678905', 'm.vasilev@yandex.ru', DATE '1979-06-17', '5056789005',
        CURRENT_DATE - 45 + TIME '18:20'),
       ('Соколова Ольга Николаевна', '+79266789006', NULL, DATE '1999-02-14', '7767890106',
        CURRENT_DATE - 30 + TIME '09:50'),
       ('Морозов Артём Павлович', '+79037890107', 'artem.morozov@gmail.com', DATE '2004-09-01', '7778901207',
        CURRENT_DATE - 21 + TIME '14:10'),
       ('Новикова Мария Денисовна', '+79858901208', 'm.novikova@mail.ru', DATE '1985-12-20', '5089012308',
        CURRENT_DATE - 14 + TIME '17:45'),
       ('Фёдоров Игорь Валентинович', '+79099012309', 'i.fedorov@mail.ru', DATE '1992-03-08', '7790123409',
        CURRENT_DATE - 7 + TIME '13:00'),
       ('Лебедева Ксения Романовна', '+79169123410', 'k.lebedeva@yandex.ru', DATE '1997-07-19', '5001234510',
        CURRENT_DATE - 3 + TIME '15:25');

INSERT INTO cars (brand, model, manufacture_year, vin, color, price, status)
VALUES ('Toyota', 'Camry', 2024, 'XW7BF4FK10S123451', 'Белый', 3850000.00, 'AVAILABLE'),
       ('Toyota', 'RAV4', 2024, 'JTMW43FV40D123452', 'Серебристый', 4200000.00, 'AVAILABLE'),
       ('Kia', 'K5', 2023, 'XWEG3417BP0123453', 'Чёрный', 3100000.00, 'AVAILABLE'),
       ('Kia', 'Sportage', 2024, 'XWEPH81ADR0123454', 'Синий', 3650000.00, 'IN_SERVICE'),
       ('Hyundai', 'Tucson', 2023, 'XWEJ3813BP0123455', 'Серый', 3400000.00, 'AVAILABLE'),
       ('Haval', 'Jolion', 2024, 'XZGFF06A1RA123456', 'Красный', 2300000.00, 'AVAILABLE'),
       ('Geely', 'Monjaro', 2024, 'Y4K8762S1RB123457', 'Чёрный', 4500000.00, 'AVAILABLE'),
       ('BMW', 'X5', 2023, 'WBA11EX020CL23458', 'Чёрный', 11500000.00, 'SOLD'),
       ('Lada', 'Vesta', 2025, 'XTAGFL110SY123459', 'Белый', 1450000.00, 'AVAILABLE'),
       ('Chery', 'Tiggo 7 Pro Max', 2024, 'LVVDB21B5RD123460', 'Серый', 2900000.00, 'AVAILABLE');

-- client_id и car_id ссылаются на строки выше (таблицы только что созданы, поэтому ID начинаются с 1)
INSERT INTO test_drive_requests (client_id, car_id, scheduled_at, duration_minutes, status, comment,
                                 created_at, updated_at)
VALUES
    -- Прошедшие тест-драйвы (заявки закрыты)
    (1, 1, CURRENT_DATE - 10 + TIME '11:00', 60, 'COMPLETED', 'Понравилась плавность хода, просил расчёт кредита',
     CURRENT_DATE - 14 + TIME '09:20', CURRENT_DATE - 10 + TIME '12:10'),
    (2, 3, CURRENT_DATE - 8 + TIME '15:30', 45, 'COMPLETED', 'Сравнивала с Toyota Camry',
     CURRENT_DATE - 12 + TIME '10:05', CURRENT_DATE - 8 + TIME '16:20'),
    (3, 8, CURRENT_DATE - 20 + TIME '12:00', 90, 'COMPLETED', 'Клиент купил автомобиль',
     CURRENT_DATE - 25 + TIME '13:00', CURRENT_DATE - 20 + TIME '13:40'),
    (4, 5, CURRENT_DATE - 5 + TIME '18:00', 30, 'NO_SHOW', 'Клиент не пришёл, телефон недоступен',
     CURRENT_DATE - 7 + TIME '11:00', CURRENT_DATE - 5 + TIME '18:45'),
    (5, 2, CURRENT_DATE - 3 + TIME '10:00', 60, 'CANCELLED', 'Клиент перенёс покупку на следующий год',
     CURRENT_DATE - 6 + TIME '15:30', CURRENT_DATE - 4 + TIME '09:00'),
    (6, 4, CURRENT_DATE - 2 + TIME '13:00', 60, 'CANCELLED', 'Автомобиль отправлен на плановое ТО',
     CURRENT_DATE - 9 + TIME '12:00', CURRENT_DATE - 3 + TIME '17:30'),
    -- Тест-драйв вчера: подтверждён, но заявку ещё не закрыли (её можно завершить или отметить неявку)
    (1, 7, CURRENT_DATE - 1 + TIME '16:00', 60, 'CONFIRMED', 'Интересует полный привод',
     CURRENT_DATE - 4 + TIME '10:30', CURRENT_DATE - 2 + TIME '11:00'),
    -- Предстоящие тест-драйвы
    (2, 1, CURRENT_DATE + 1 + TIME '10:00', 60, 'CONFIRMED', 'Повторный тест-драйв перед покупкой',
     CURRENT_DATE - 1 + TIME '12:00', CURRENT_DATE - 1 + TIME '12:30'),
    (3, 6, CURRENT_DATE + 1 + TIME '14:00', 30, 'NEW', NULL,
     CURRENT_DATE - 1 + TIME '08:00', CURRENT_DATE - 1 + TIME '08:00'),
    (7, 9, CURRENT_DATE + 2 + TIME '12:00', 45, 'NEW', 'Первый автомобиль, нужен инструктаж',
     CURRENT_DATE - 2 + TIME '19:10', CURRENT_DATE - 2 + TIME '19:10'),
    (8, 2, CURRENT_DATE + 2 + TIME '17:00', 60, 'CONFIRMED', 'Нужно детское кресло',
     CURRENT_DATE - 3 + TIME '14:00', CURRENT_DATE - 2 + TIME '10:00'),
    (5, 7, CURRENT_DATE + 3 + TIME '11:30', 90, 'NEW', 'Хочет проверить машину на трассе',
     CURRENT_DATE - 1 + TIME '16:40', CURRENT_DATE - 1 + TIME '16:40'),
    (4, 3, CURRENT_DATE + 5 + TIME '15:00', 45, 'NEW', NULL,
     CURRENT_DATE - 1 + TIME '20:15', CURRENT_DATE - 1 + TIME '20:15'),
    (1, 5, CURRENT_DATE + 7 + TIME '19:00', 60, 'NEW', 'Удобно только вечером',
     CURRENT_DATE - 1 + TIME '21:00', CURRENT_DATE - 1 + TIME '21:00'),
    (6, 1, CURRENT_DATE + 4 + TIME '09:30', 30, 'CANCELLED', 'Клиент выбрал другую комплектацию',
     CURRENT_DATE - 5 + TIME '10:00', CURRENT_DATE - 1 + TIME '09:15');

COMMIT;

-- Проверка: сколько строк загружено в каждую таблицу
SELECT 'clients' AS table_name, COUNT(*) AS row_count FROM clients
UNION ALL
SELECT 'cars', COUNT(*) FROM cars
UNION ALL
SELECT 'test_drive_requests', COUNT(*) FROM test_drive_requests;
