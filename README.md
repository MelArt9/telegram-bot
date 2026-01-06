# 🤖 Telegram Bot Assistant - Бот-помощник для студенческих групп

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Spring_Boot-4.0.1-green?style=for-the-badge&logo=spring" alt="Spring Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Telegram_Bot_API-6.7.0-blue?style=for-the-badge&logo=telegram" alt="Telegram">
</p>

<p align="center">
  <strong>Умный ассистент для организации учебного процесса</strong><br>
  Автоматизация дедлайнов, напоминаний и ресурсов для студенческих групп
</p>

---

## 📋 Содержание
- [✨ Возможности](#возможности)
- [🚀 Быстрый старт](#быстрый-старт)
- [⚙️ Установка](#установка)
- [📦 Конфигурация](#конфигурация)
- [📱 Команды бота](#команды-бота)
- [🐳 Docker развертывание](#docker-развертывание)
- [🔧 Systemd развертывание](#systemd-развертывание)
- [🗄️ Структура БД](#структура-бд)
- [🔐 Безопасность](#безопасность)
- [📊 Мониторинг](#мониторинг)
- [🤝 Вклад в проект](#вклад-в-проект)
- [📄 Лицензия](#лицензия)

---
#возможности
## ✨ Возможности

### 📅 **Управление учебным процессом**
| Функция | Описание | Эмодзи |
|---------|----------|--------|
| **Дедлайны** | Трекинг сроков сдачи работ, экзаменов, проектов | ⏰ |
| **Напоминания** | Автоматические уведомления о важных событиях | 🔔 |
| **Расписание** | Персональное и групповое расписание занятий | 📋 |
| **Ресурсы** | Общая база полезных ссылок и материалов | 🔗 |
| **Расписание** | Сегодня, определенный день недели и на четную/нечетную неделю | 📊 |
| **Тэг** | Упоминание всех присутствующих в группе и по группам отдельно | 🔔 |

### 👥 **Управление группами**
- Поддержка множества студенческих групп
- Ролевая система (студенты, преподаватели, администраторы)
- Массовые уведомления и рассылки
- Интеграция с существующими чатами

### 🔧 **Технические возможности**
- Работа 24/7 как системный сервис
- Масштабируемая архитектура
- REST API для интеграций
- Детальное логирование и мониторинг
- Автоматические бэкапы

---

## 🚀 Быстрый старт

### 📋 Предварительные требования
- **Java 17+**
- **PostgreSQL 14+**
- **Maven 3.8+**
- **Telegram Bot Token** (получить у [@BotFather](https://t.me/BotFather))

### 🔧 Установка за 5 минут
```
# 1. Клонирование репозитория
git clone https://github.com/yourusername/telegram-bot.git
cd telegram-bot

# 2. Настройка базы данных
createdb maga
createuser telegram_bot_user -P
# Введите пароль при запросе

# 3. Конфигурация
cp src/main/resources/application.example.yml src/main/resources/application.yml
nano src/main/resources/application.yml
# Добавьте ваш токен бота

# 4. Сборка проекта
mvn clean package -DskipTests

# 5. Запуск
java -jar target/telegram-bot-0.0.1-SNAPSHOT.jar
```
---

## ⚙️ Установка
### Подробная установка на Ubuntu/Debian
```
# Обновление системы
sudo apt update && sudo apt upgrade -y

# Установка Java
sudo apt install openjdk-17-jdk -y

# Установка PostgreSQL
sudo apt install postgresql postgresql-contrib -y

# Установка Maven
sudo apt install maven -y

# Создание базы данных
sudo -u postgres psql << EOF
CREATE DATABASE maga;
CREATE USER telegram_bot_user WITH PASSWORD 'secure_password_here';
GRANT ALL PRIVILEGES ON DATABASE maga TO telegram_bot_user;
\c maga
GRANT ALL ON SCHEMA public TO telegram_bot_user;
EOF

# Клонирование и настройка проекта
git clone https://github.com/yourusername/telegram-bot.git
cd telegram-bot

# Настройка конфигурации
cat > src/main/resources/application.yml << 'EOF'
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/maga
    username: telegram_bot_user
    password: ${DB_PASSWORD:secure_password_here}
  jpa:
    hibernate:
      ddl-auto: update

bot:
  token: ${BOT_TOKEN:your_bot_token_here}
  username: your_bot_username

server:
  port: 8080
EOF

# Сборка
mvn clean package -DskipTests
```
---

## 📦 Конфигурация
### Основной конфигурационный файл
```src/main/resources/application.yml:```

```
# Основные настройки приложения
app:
  name: "Telegram Bot Assistant"
  version: "1.0.0"
  timezone: "Europe/Moscow"

# Настройки Telegram бота
bot:
  token: "${BOT_TOKEN}"  # Получить у @BotFather
  username: "your_bot_username"
  webhook:
    enabled: false
    path: "/webhook"
  
# Настройки базы данных
spring:
  datasource:
    url: "jdbc:postgresql://localhost:5432/maga"
    username: "telegram_bot_user"
    password: "${DB_PASSWORD}"
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: "update"
    show-sql: false
    properties:
      hibernate:
        dialect: "org.hibernate.dialect.PostgreSQLDialect"
        format_sql: true
  
# Настройки сервера
server:
  port: 8080
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/json,application/javascript
  servlet:
    session:
      timeout: 30m

# Настройки планировщика
scheduler:
  deadlines:
    enabled: true
    cron: "0 */30 * * * *"  # Проверка каждые 30 минут
  reminders:
    enabled: true
    cron: "0 * * * * *"      # Проверка каждую минуту
  cleanup:
    enabled: true
    cron: "0 0 3 * * *"      # Ежедневно в 3:00

# Логирование
logging:
  level:
    ru.melnikov.telegrambot: INFO
    org.springframework: WARN
    org.hibernate: WARN
  file:
    name: /var/log/telegram-bot/bot.log
    max-size: 10MB
    max-history: 30
```

### Переменные окружения
Создайте файл ```.env``` в корне проекта:

```
# Безопасное хранение секретов
BOT_TOKEN="1234567890:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw"
DB_PASSWORD="VeryStrongPassword123!"
ADMIN_PASSWORD="AdminSecurePass456!"
```

Или установите переменные окружения:

```
export BOT_TOKEN="ваш_токен"
export DB_PASSWORD="пароль_бд"
```
---

## 📱 Команды бота
### Основные команды

| Команда | Описание | Пример |
|---------|----------|--------|
| ```/start``` | Начать работу с ботом | ```/start``` |
| ```/help``` | Получить справку по командам | ```/help``` |
| ```/today``` | Расписание на сегодня | ```/today``` |
| ```/day``` | Расписание по дню недели | ```/day [от 1-7]``` |
| ```/week``` | Расписание на текущую неделю (автоопределение) | ```/week``` |
| ```/week``` | Расписание на указанную неделю | ```/week [odd / even]``` |
| ```/deadlines``` | Дедлайны работ | ```/deadlines``` |
| ```/links``` | Полезные ресурсы | ```/links``` |
| ```/tag``` | Упомянуть группу | ```/tag all``` |

---

## 🐳 Docker развертывание
### Docker Compose конфигурация
```docker-compose.yml:```

```
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: maga
      POSTGRES_USER: telegram_bot_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U telegram_bot_user"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - bot-network

  telegram-bot:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      BOT_TOKEN: ${BOT_TOKEN}
      DB_PASSWORD: ${DB_PASSWORD}
      SPRING_PROFILES_ACTIVE: docker
      TZ: Europe/Moscow
    ports:
      - "8080:8080"
    volumes:
      - ./logs:/app/logs
      - ./backups:/app/backups
    restart: unless-stopped
    networks:
      - bot-network

volumes:
  postgres_data:

networks:
  bot-network:
    driver: bridge
```

```Dockerfile:```

```
FROM openjdk:17-jdk-slim

WORKDIR /app

# Установка curl для healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Создание непривилегированного пользователя
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

# Копирование JAR файла
COPY target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Запуск с Docker Compose

```
# 1. Подготовка переменных окружения
cp .env.example .env
# Отредактируйте .env файл, добавьте токен бота

# 2. Сборка и запуск
docker-compose up -d --build

# 3. Проверка статуса
docker-compose ps

# 4. Просмотр логов
docker-compose logs -f telegram-bot

# 5. Остановка
docker-compose down

# 6. Остановка с удалением томов
docker-compose down -v
```
---

## 🔧 Systemd развертывание
### Создание сервисного файла

```/etc/systemd/system/telegram-bot.service```:

```
[Unit]
Description=Telegram Bot Assistant
After=network.target postgresql.service
Wants=postgresql.service
Documentation=https://github.com/yourusername/telegram-bot

[Service]
Type=simple
User=telegrambot
Group=telegrambot
WorkingDirectory=/opt/telegram-bot
EnvironmentFile=/etc/default/telegram-bot

# Параметры Java
Environment="JAVA_OPTS=-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"
ExecStart=/usr/bin/java $JAVA_OPTS -jar app.jar
SuccessExitStatus=143

# Перезапуск при сбоях
Restart=always
RestartSec=10
StartLimitInterval=300
StartLimitBurst=5

# Безопасность
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
PrivateDevices=true
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectControlGroups=true
ReadWritePaths=/var/log/telegram-bot /opt/telegram-bot/logs
ReadOnlyPaths=/

# Логирование
StandardOutput=journal
StandardError=journal
SyslogIdentifier=telegram-bot

[Install]
WantedBy=multi-user.target
```

### Настройка и запуск

```
# 1. Создание пользователя для бота
sudo useradd -r -s /bin/false -m -d /opt/telegram-bot telegrambot

# 2. Создание директорий
sudo mkdir -p /opt/telegram-bot /var/log/telegram-bot /backups/telegram-bot
sudo chown -R telegrambot:telegrambot /opt/telegram-bot /var/log/telegram-bot /backups/telegram-bot

# 3. Копирование приложения
sudo cp target/*.jar /opt/telegram-bot/app.jar
sudo chown telegrambot:telegrambot /opt/telegram-bot/app.jar

# 4. Создание файла с переменными окружения
sudo tee /etc/default/telegram-bot > /dev/null << EOF
BOT_TOKEN=ваш_токен_бота
DB_PASSWORD=пароль_базы_данных
ADMIN_PASSWORD=пароль_администратора
JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"
EOF

# 5. Загрузка systemd и запуск
sudo systemctl daemon-reload
sudo systemctl enable telegram-bot
sudo systemctl start telegram-bot

# 6. Проверка статуса
sudo systemctl status telegram-bot

# 7. Просмотр логов
sudo journalctl -u telegram-bot -f
```

### Команды управления systemd сервисом

```
# Статус бота
sudo systemctl status telegram-bot

# Запуск
sudo systemctl start telegram-bot

# Остановка
sudo systemctl stop telegram-bot

# Перезапуск (после обновления)
sudo systemctl restart telegram-bot

# Включение автозагрузки
sudo systemctl enable telegram-bot

# Отключение автозагрузки
sudo systemctl disable telegram-bot

# Просмотр логов
sudo journalctl -u telegram-bot -f                    # в реальном времени
sudo journalctl -u telegram-bot -n 100 --no-pager     # последние 100 строк
sudo journalctl -u telegram-bot --since "1 hour ago"  # за последний час

# Очистка старых логов
sudo journalctl --vacuum-time=7d
```

---

## 🗄️ Структура БД
### Основные таблицы

```
-- Пользователи
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(100),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Группы
CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Дедлайны
CREATE TABLE deadlines (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    deadline_date TIMESTAMP NOT NULL,
    subject VARCHAR(100),
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Полезные ссылки
CREATE TABLE links (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    description TEXT,
    category VARCHAR(50),
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Напоминания
CREATE TABLE reminders (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    reminder_date TIMESTAMP NOT NULL,
    is_recurring BOOLEAN DEFAULT false,
    recurrence_pattern VARCHAR(50),
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Чаты бота
CREATE TABLE bot_chats (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    chat_type VARCHAR(20),
    title VARCHAR(255),
    username VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    is_bot_admin BOOLEAN DEFAULT false,
    bot_permissions JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Логи команд
CREATE TABLE command_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    command VARCHAR(100) NOT NULL,
    parameters JSONB,
    chat_id BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Индексы для оптимизации
```
-- Индексы для быстрого поиска
CREATE INDEX idx_deadlines_date ON deadlines(deadline_date);
CREATE INDEX idx_deadlines_priority ON deadlines(priority);
CREATE INDEX idx_deadlines_created_by ON deadlines(created_by);

CREATE INDEX idx_links_category ON links(category);
CREATE INDEX idx_links_created_by ON links(created_by);

CREATE INDEX idx_reminders_date ON reminders(reminder_date);
CREATE INDEX idx_reminders_created_by ON reminders(created_by);
CREATE INDEX idx_reminders_recurring ON reminders(is_recurring);

CREATE INDEX idx_bot_chats_active ON bot_chats(is_active);
CREATE INDEX idx_bot_chats_chat_id ON bot_chats(chat_id);

CREATE INDEX idx_command_logs_created_at ON command_logs(created_at);
CREATE INDEX idx_command_logs_user_id ON command_logs(user_id);
CREATE INDEX idx_command_logs_command ON command_logs(command);
```
---

## 🔐 Безопасность
### Рекомендации по безопасности
1. Использование переменных окружения для хранения секретов
2. Регулярное обновление зависимостей
3. Настройка firewall на сервере
4. Регулярные бэкапы базы данных
5. Мониторинг подозрительной активности

### Настройка UFW (Firewall)
```
# Установка UFW
sudo apt install ufw -y

# Базовые правила
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Разрешение нужных портов
sudo ufw allow 22/tcp comment 'SSH'
sudo ufw allow 5432/tcp comment 'PostgreSQL'
sudo ufw allow 8080/tcp comment 'Telegram Bot'

# Включение firewall
sudo ufw --force enable

# Проверка статуса
sudo ufw status verbose
```

### SSL/TLS настройка (для HTTPS)
```
# В application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: tomcat
  port: 8443
```

---
## 📊 Мониторинг
### Health checks
```
# Проверка здоровья приложения
curl http://localhost:8080/actuator/health

# Подробная информация
curl http://localhost:8080/actuator/health/details

# Информация о приложении
curl http://localhost:8080/actuator/info

# Метрики
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### Скрипт мониторинга
```monitor-bot.sh:```

```
#!/bin/bash

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🤖 Мониторинг Telegram бота"
echo "============================"

# Проверка статуса systemd сервиса
if systemctl is-active --quiet telegram-bot; then
    echo -e "${GREEN}✅ Бот работает${NC}"
else
    echo -e "${RED}❌ Бот не запущен${NC}"
    exit 1
fi

# Проверка порта
if ss -tulpn | grep -q :8080; then
    echo -e "${GREEN}✅ Порт 8080 слушает${NC}"
else
    echo -e "${RED}❌ Порт 8080 не слушает${NC}"
fi

# Проверка PostgreSQL
if systemctl is-active --quiet postgresql; then
    echo -e "${GREEN}✅ PostgreSQL работает${NC}"
else
    echo -e "${RED}❌ PostgreSQL не запущен${NC}"
fi

# Проверка памяти
MEM_USAGE=$(free -m | awk 'NR==2{printf "%.2f%%", $3*100/$2}')
echo -e "${YELLOW}📊 Использование памяти: $MEM_USAGE${NC}"

# Проверка диска
DISK_USAGE=$(df -h / | awk 'NR==2{print $5}')
echo -e "${YELLOW}💾 Использование диска: $DISK_USAGE${NC}"

# Проверка логов на ошибки
ERROR_COUNT=$(sudo journalctl -u telegram-bot --since "1 hour ago" | grep -c "ERROR")
if [ "$ERROR_COUNT" -gt 0 ]; then
    echo -e "${RED}⚠️  Найдено $ERROR_COUNT ошибок за последний час${NC}"
else
    echo -e "${GREEN}✅ Ошибок за последний час не найдено${NC}"
fi

echo "============================"
```
---

## 🤝 Вклад в проект
Мы приветствуем вклад в развитие проекта! Вот как вы можете помочь:

#### Процесс внесения изменений
1. Fork репозитория
2. Создайте ветку для вашей фичи:

```
git checkout -b feature/amazing-feature
```

3. Внесите изменения и добавьте тесты
4. Запустите тесты:

```
mvn test
```

5. Создайте Pull Request

#### Стиль кода
- Следуйте Java Code Conventions
- Используйте осмысленные имена переменных и методов
- Добавляйте комментарии для сложной логики
- Пишите тесты для нового функционала

#### Структура коммитов
```
feat: добавление новой функции
fix: исправление ошибки
docs: обновление документации
style: форматирование кода
refactor: рефакторинг кода
test: добавление тестов
chore: обновление зависимостей
```
---

## 📄 Лицензия
Этот проект распространяется под лицензией MIT. Подробнее см. в файле LICENSE.

```
MIT License

Copyright (c) 2026 Artem Melnikov

Разрешается бесплатное использование, копирование, изменение, объединение, публикация, распространение, сублицензирование и/или продажа копий Программного обеспечения при соблюдении следующих условий:

Вышеуказанное уведомление об авторских правах и данное разрешение должны быть включены во все копии или значительные части Программного обеспечения.

ПРОГРАММНОЕ ОБЕСПЕЧЕНИЕ ПРЕДОСТАВЛЯЕТСЯ "КАК ЕСТЬ", БЕЗ КАКИХ-ЛИБО ГАРАНТИЙ, ЯВНЫХ ИЛИ ПОДРАЗУМЕВАЕМЫХ, ВКЛЮЧАЯ, НО НЕ ОГРАНИЧИВАЯСЬ ГАРАНТИЯМИ ТОВАРНОЙ ПРИГОДНОСТИ, СООТВЕТСТВИЯ ПО ЕГО КОНКРЕТНОМУ НАЗНАЧЕНИЮ И ОТСУТСТВИЯ НАРУШЕНИЙ. НИ В КОЕМ СЛУЧАЕ АВТОРЫ ИЛИ ПРАВООБЛАДАТЕЛИ НЕ НЕСУТ ОТВЕТСТВЕННОСТИ ПО ИСКАМ О ВОЗМЕЩЕНИИ УЩЕРБА, УБЫТКОВ ИЛИ ДРУГИХ ТРЕБОВАНИЙ ПО ДЕЙСТВУЮЩЕМУ ПРАВУ, ДОГОВОРУ ИЛИ ИНОМУ, ВОЗНИКШИМ ИЗ, ИМЕЮЩИМ ПРИЧИНОЙ ИЛИ СВЯЗАННЫМ С ПРОГРАММНЫМ ОБЕСПЕЧЕНИЕМ ИЛИ ИСПОЛЬЗОВАНИЕМ ПРОГРАММНОГО ОБЕСПЕЧЕНИЯ ИЛИ ИНЫМИ ДЕЙСТВИЯМИ С ПРОГРАММНЫМ ОБЕСПЕЧЕНИЕМ.
```

<div align="center"> <p>Сделано с ❤️ для студенческих групп</p>
<sub>Если этот проект был полезен, поставьте ⭐ на GitHub!</sub>

</div>
