-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(100),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) DEFAULT 'STUDENT',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Таблица групп
CREATE TABLE IF NOT EXISTS groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
    );

-- Таблица связей пользователей и групп
CREATE TABLE IF NOT EXISTS user_groups (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    group_id BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
    );

-- Таблица расписания
CREATE TABLE IF NOT EXISTS schedule (
    id BIGSERIAL PRIMARY KEY,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    time_start TIME NOT NULL,
    time_end TIME NOT NULL,
    subject VARCHAR(255) NOT NULL,
    teacher VARCHAR(255),
    location VARCHAR(255),
    is_online BOOLEAN DEFAULT false,
    week_type VARCHAR(10) DEFAULT 'all',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Таблица дедлайнов
CREATE TABLE IF NOT EXISTS deadlines (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    deadline_at TIMESTAMP NOT NULL,
    description TEXT,
    link_url TEXT,
    link_text VARCHAR(255),
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Таблица ссылок
CREATE TABLE IF NOT EXISTS links (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Таблица чатов бота
CREATE TABLE IF NOT EXISTS bot_chats (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL UNIQUE,
    chat_type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    username VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    is_bot_admin BOOLEAN DEFAULT false,
    bot_permissions TEXT DEFAULT '{}',
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Таблица напоминаний
CREATE TABLE IF NOT EXISTS reminders (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    reminder_type VARCHAR(50) NOT NULL,
    schedule_time TIME NOT NULL,
    days_of_week VARCHAR(7),
    is_active BOOLEAN DEFAULT true,
    config JSONB DEFAULT '{}',
    last_sent_at TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Таблица логов команд
CREATE TABLE IF NOT EXISTS command_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(100),
    chat_id BIGINT,
    command VARCHAR(50) NOT NULL,
    arguments TEXT,
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    execution_time_ms BIGINT,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Создаем индексы для производительности
CREATE INDEX IF NOT EXISTS idx_users_telegram_id ON users(telegram_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_schedule_day_week ON schedule(day_of_week, week_type);
CREATE INDEX IF NOT EXISTS idx_schedule_time ON schedule(time_start, time_end);
CREATE INDEX IF NOT EXISTS idx_deadlines_deadline_at ON deadlines(deadline_at);
CREATE INDEX IF NOT EXISTS idx_deadlines_created_by ON deadlines(created_by);
CREATE INDEX IF NOT EXISTS idx_bot_chats_chat_id ON bot_chats(chat_id);
CREATE INDEX IF NOT EXISTS idx_bot_chats_is_active ON bot_chats(is_active);
CREATE INDEX IF NOT EXISTS idx_command_logs_user_id ON command_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_command_logs_command ON command_logs(command);
CREATE INDEX IF NOT EXISTS idx_command_logs_created_at ON command_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_command_logs_success ON command_logs(success);

-- Комментарии к таблицам
COMMENT ON TABLE users IS 'Пользователи Telegram бота';
COMMENT ON TABLE schedule IS 'Расписание занятий';
COMMENT ON TABLE deadlines IS 'Дедлайны заданий';
COMMENT ON TABLE links IS 'Полезные ссылки';
COMMENT ON TABLE bot_chats IS 'Чаты, где работает бот';
COMMENT ON TABLE reminders IS 'Напоминания для чатов';
COMMENT ON TABLE command_logs IS 'Логи выполненных команд';

-- Вставляем тестовые данные (опционально)
-- =========================
-- USERS
-- =========================
INSERT INTO users (telegram_id, username, first_name, last_name)
VALUES
    (1650402846, 'mi1ord', 'Артем', 'Мельников'),
    (2020915227, 'ShiroZetsubo', 'Анастасия', 'Щербак'),
    (904083418, 'kartofffk', 'Антон', 'Чумаченко'),
    (775635945, 'dnchikkkkk', 'Денис', 'Карелов'),
    (731391256, 'ArtemKissly', 'Артем', 'Сапунов'),
    (1737916809, 'le_petit_georgin', 'Георгий', 'Кузнецов'),
    (1289028828, 'aquarell_dev', 'Илья', 'Крутских'),
    (400104189, 'ken1ze', 'Егор', 'Проценко'),
    (1464745730, 'Brassboec', 'Арсений', 'Золин'),
    (934978222, 'maximkodaze', 'Максим', 'Сергиенко'),
    (1024825703, 'Proxor_V', 'Прохор', 'Велитченко');

-- =========================
-- GROUPS
-- =========================
INSERT INTO groups (name, description)
VALUES
    ('starosta', 'Староста'),
    ('all', 'Все студенты группы');

-- =========================
-- SCHEDULE
-- =========================
INSERT INTO schedule (day_of_week, time_start, time_end, subject, teacher, location, is_online, week_type)
VALUES
    (1, '10:00', '11:30', 'Базы данных', 'Иванов И.И.', 'Ауд. 302', false, 'all'),
    (1, '12:00', '13:30', 'Машинное обучение', 'Петров П.П.', 'Zoom', true, 'odd'),
    (3, '14:00', '15:30', 'Алгоритмы', 'Сидоров С.С.', 'Ауд. 215', false, 'even'),
    (5, '09:00', '10:30', 'Архитектура ПО', 'Кузнецов К.К.', 'Zoom', true, 'all');

-- =========================
-- DEADLINES
-- =========================
INSERT INTO deadlines (title, deadline_at, description, created_by)
VALUES
    ('Лабораторная работа №2', NOW() + INTERVAL '5 days', 'Сдать в LMS', 1),
    ('Курсовой проект', NOW() + INTERVAL '14 days', 'Загрузить PDF в систему', 1);

-- =========================
-- LINKS
-- =========================
INSERT INTO links (title, url, created_by)
VALUES
    ('Google Drive группы', 'https://drive.google.com/', 1),
    ('Moodle', 'https://moodle.university.ru', 1),
    ('Zoom', 'https://zoom.us', 1);