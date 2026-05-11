-- Миграция: меняем ENUM role на VARCHAR + CHECK

-- 1. Добавляем новую колонку
ALTER TABLE users ADD COLUMN role_new VARCHAR(20);

-- 2. Копируем данные (приводим ENUM к тексту)
UPDATE users SET role_new = role::text;

-- 3. Удаляем старую колонку ENUM
ALTER TABLE users DROP COLUMN role;

-- 4. Переименовываем новую колонку
ALTER TABLE users RENAME COLUMN role_new TO role;

-- 5. Делаем колонку NOT NULL + DEFAULT + CHECK
ALTER TABLE users
    ALTER COLUMN role SET NOT NULL,
ALTER COLUMN role SET DEFAULT 'USER';

ALTER TABLE users ADD CONSTRAINT valid_user_role
    CHECK (role IN ('USER', 'ADMIN'));

-- То же самое для статуса журналов
ALTER TABLE magazines ADD COLUMN status_new VARCHAR(20);

UPDATE magazines SET status_new = status::text;

ALTER TABLE magazines DROP COLUMN status;

ALTER TABLE magazines RENAME COLUMN status_new TO status;

ALTER TABLE magazines
    ALTER COLUMN status SET NOT NULL,
ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE magazines ADD CONSTRAINT valid_magazine_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));