-- =====================================================================
--  Автосалон: заявки на тест-драйв автомобилей
--  Скрипт 1 из 2: создание базы данных autosalon.
--
--  Выполнять один раз от имени пользователя postgres, подключившись к БД postgres:
--    psql -U postgres -f sql/01_create_database.sql
--  или в pgAdmin: Query Tool на базе postgres -> открыть файл -> Execute (F5).
-- =====================================================================

SET client_encoding = 'UTF8';

-- Если базу нужно пересоздать с нуля, раскомментируйте строку ниже (все данные будут удалены):
-- DROP DATABASE IF EXISTS autosalon WITH (FORCE);

CREATE DATABASE autosalon
    WITH ENCODING = 'UTF8'
    TEMPLATE = template0;

COMMENT ON DATABASE autosalon IS 'Автосалон: клиенты, автомобили и заявки на тест-драйв';
