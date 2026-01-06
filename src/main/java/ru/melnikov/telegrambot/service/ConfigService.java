package ru.melnikov.telegrambot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.config.BotSettingsConfig;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final BotSettingsConfig settingsConfig;
    private final ObjectMapper yamlMapper = new ObjectMapper(
            new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    );

    /**
     * Получить текущую конфигурацию
     */
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();

        // Основные настройки бота
        config.put("bot", Map.of(
                "enabled", settingsConfig.getBot().getEnabled(),
                "username", settingsConfig.getBot().getUsername(),
                "token_present", settingsConfig.getBot().getToken() != null
        ));

        // Настройки напоминаний
        config.put("reminders", Map.of(
                "schedule", Map.of(
                        "enabled", settingsConfig.getReminders().getSchedule().getEnabled(),
                        "time", settingsConfig.getReminders().getSchedule().getTime(),
                        "days", settingsConfig.getReminders().getSchedule().getDays(),
                        "days_description", settingsConfig.getReminders().getSchedule().getDaysDescription()
                ),
                "deadlines", Map.of(
                        "enabled", settingsConfig.getReminders().getDeadlines().getEnabled(),
                        "time", settingsConfig.getReminders().getDeadlines().getTime(),
                        "days", settingsConfig.getReminders().getDeadlines().getDays(),
                        "days_description", settingsConfig.getReminders().getDeadlines().getDaysDescription()
                ),
                "before_class", Map.of(
                        "enabled", settingsConfig.getReminders().getBeforeClass().getEnabled(),
                        "minutes", settingsConfig.getReminders().getBeforeClass().getMinutes()
                ),
                "week_type", Map.of(
                        "reference_date", settingsConfig.getReminders().getWeekType().getReferenceDate(),
                        "reference_week_type", settingsConfig.getReminders().getWeekType().getReferenceWeekType()
                )
        ));

        // Администраторы
        config.put("admins", Map.of(
                "usernames", settingsConfig.getAdmins().getUsernames(),
                "user_ids", settingsConfig.getAdmins().getUserIds()
        ));

        config.put("last_updated", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        ));

        return config;
    }

    /**
     * Получить конфигурацию в читаемом формате
     */
    public String getReadableConfig() {
        Map<String, Object> config = getConfig();

        StringBuilder sb = new StringBuilder();
        sb.append("⚙️ *ТЕКУЩАЯ КОНФИГУРАЦИЯ БОТА*\n\n");

        // Бот
        sb.append("🤖 *БОТ:*\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> bot = (Map<String, Object>) config.get("bot");
        sb.append(String.format("• Включен: %s\n",
                Boolean.TRUE.equals(bot.get("enabled")) ? "✅" : "❌"));
        sb.append(String.format("• Username: @%s\n\n", bot.get("username")));

        // Напоминания
        sb.append("🔔 *НАПОМИНАНИЯ:*\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> reminders = (Map<String, Object>) config.get("reminders");

        @SuppressWarnings("unchecked")
        Map<String, Object> schedule = (Map<String, Object>) reminders.get("schedule");
        sb.append(String.format("📅 *Расписание:* %s\n",
                Boolean.TRUE.equals(schedule.get("enabled")) ? "✅" : "❌"));
        sb.append(String.format("   Время: %s\n", schedule.get("time")));
        sb.append(String.format("   Дни: %s\n\n", schedule.get("days_description")));

        @SuppressWarnings("unchecked")
        Map<String, Object> deadlines = (Map<String, Object>) reminders.get("deadlines");
        sb.append(String.format("⏰ *Дедлайны:* %s\n",
                Boolean.TRUE.equals(deadlines.get("enabled")) ? "✅" : "❌"));
        sb.append(String.format("   Время: %s\n", deadlines.get("time")));
        sb.append(String.format("   Дни: %s\n\n", deadlines.get("days_description")));

        @SuppressWarnings("unchecked")
        Map<String, Object> beforeClass = (Map<String, Object>) reminders.get("before_class");
        sb.append(String.format("🔔 *Перед парой:* %s\n",
                Boolean.TRUE.equals(beforeClass.get("enabled")) ? "✅" : "❌"));
        sb.append(String.format("   Минут до: %s\n\n", beforeClass.get("minutes")));

        @SuppressWarnings("unchecked")
        Map<String, Object> weekType = (Map<String, Object>) reminders.get("week_type");
        sb.append("🗓️ *ТИП НЕДЕЛИ:*\n");
        sb.append(String.format("   Дата отсчета: %s\n", weekType.get("reference_date")));
        sb.append(String.format("   Тип недели: %s\n\n",
                "even".equals(weekType.get("reference_week_type")) ? "ЗНАМЕНАТЕЛЬ" : "ЧИСЛИТЕЛЬ"));

        // Администраторы
        sb.append("👑 *АДМИНИСТРАТОРЫ:*\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> admins = (Map<String, Object>) config.get("admins");
        @SuppressWarnings("unchecked")
        java.util.List<String> usernames = (java.util.List<String>) admins.get("usernames");
        if (!usernames.isEmpty()) {
            sb.append("   Usernames: ");
            for (int i = 0; i < usernames.size(); i++) {
                sb.append("@").append(usernames.get(i));
                if (i < usernames.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("\n");
        }

        @SuppressWarnings("unchecked")
        java.util.List<Long> userIds = (java.util.List<Long>) admins.get("user_ids");
        if (!userIds.isEmpty()) {
            sb.append("   User IDs: ");
            for (int i = 0; i < userIds.size(); i++) {
                sb.append(userIds.get(i));
                if (i < userIds.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("\n");
        }

        sb.append("\n🕒 *Обновлено:* ").append(config.get("last_updated"));

        return sb.toString();
    }

    /**
     * Обновить настройку
     */
    public boolean updateSetting(String path, String value) {
        try {
            // Создаем резервную копию
            createBackup();

            // Читаем текущий YAML файл
            String yamlContent = readYamlFile();

            // Обновляем значение
            String updatedYaml = updateYamlValue(yamlContent, path, value);

            // Записываем обратно
            writeYamlFile(updatedYaml);

            log.info("✅ Настройка обновлена: {} = {}", path, value);
            return true;

        } catch (Exception e) {
            log.error("❌ Ошибка обновления настройки: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Создать резервную копию конфигурации
     */
    public void createBackup() throws IOException {
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );

        // Читаем текущий YAML
        String yamlContent = readYamlFile();

        // Создаем безопасную версию (маскируем токен)
        String safeContent = maskSensitiveData(yamlContent);

        // Записываем резервную копию
        Path backupDir = Paths.get("config_backups");
        Files.createDirectories(backupDir);

        Path backupPath = backupDir.resolve("application_" + timestamp + ".yml");
        Files.write(backupPath, safeContent.getBytes());

        log.info("📁 Создана резервная копия: {}", backupPath);
    }

    /**
     * Прочитать YAML файл
     */
    private String readYamlFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("application.yml");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
            return reader.lines().collect(java.util.stream.Collectors.joining("\n"));
        }
    }

    /**
     * Обновить значение в YAML
     */
    private String updateYamlValue(String yamlContent, String path, String value) {
        String[] parts = path.split("\\.");
        StringBuilder regex = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append("\\s*");
            }
            regex.append(parts[i]).append(":");
        }

        String pattern = regex.toString() + "\\s*[^\\n\\s][^\\n]*";
        String replacement = String.format("%s %s", String.join(":", parts), value);

        return yamlContent.replaceAll(pattern, replacement);
    }

    /**
     * Записать YAML файл
     */
    private void writeYamlFile(String content) throws IOException {
        Path path = Paths.get("src/main/resources/application.yml");
        Files.write(path, content.getBytes());

        // Также обновляем в текущей директории (для IDE)
        Path currentDirPath = Paths.get("application.yml");
        Files.write(currentDirPath, content.getBytes());
    }

    /**
     * Получить список доступных настроек
     */
    public String getAvailableSettings() {
        return """
        ⚙️ *ДОСТУПНЫЕ НАСТРОЙКИ ДЛЯ ИЗМЕНЕНИЯ:*
        
        🤖 *Основные настройки бота:*
        • `bot.enabled` – true/false (включить/выключить бота)
        • `bot.username` – имя бота (без @)
        
        🔔 *Напоминания о расписании:*
        • `reminders.schedule.enabled` – true/false
        • `reminders.schedule.time` – "HH:mm" (например "08:00")
        • `reminders.schedule.days` – "1111100" (Пн-Вс, 1=включено, 0=выключено)
        
        ⏰ *Напоминания о дедлайнах:*
        • `reminders.deadlines.enabled` – true/false
        • `reminders.deadlines.time` – "HH:mm"
        • `reminders.deadlines.days` – "0101010"
        
        🔔 *Напоминания перед парой:*
        • `reminders.before-class.enabled` – true/false
        • `reminders.before-class.minutes` – число (например 10)
        
        🗓️ *Настройки недель:*
        • `reminders.week-type.reference-date` – "yyyy-MM-dd"
        • `reminders.week-type.reference-week-type` – "odd"/"even"
        
        👑 *Администраторы:*
        • `admins.usernames[0]` – первый username
        • `admins.userIds[0]` – первый user ID
        
        💡 *Пример использования:*
        `/config set reminders.schedule.time "08:00"`
        `/config set reminders.before-class.minutes 15`
        `/config set bot.enabled true`
        
        ⚠️ *Изменения вступят в силу после перезапуска бота!*
        """;
    }

    /**
     * Маскирует чувствительные данные в конфигурации
     */
    private String maskSensitiveData(String yamlContent) {
        // Маскируем токен бота
        return yamlContent.replaceAll(
                "token:\\s*\".*?\"",
                "token: \"*******\""
        );
    }
}