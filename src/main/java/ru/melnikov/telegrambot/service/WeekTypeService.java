// Файл: /src/main/java/ru/melnikov/telegrambot/service/WeekTypeService.java
package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.config.BotSettingsConfig;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeekTypeService {

    private final BotSettingsConfig settingsConfig;

    /**
     * Определяет текущий тип недели на основе конфигурации из YAML
     * @return "odd" - числитель, "even" - знаменатель
     */
    public String getCurrentWeekType() {
        LocalDate today = LocalDate.now();
        return getWeekTypeForDate(today);
    }

    /**
     * Определяет тип недели для указанной даты на основе конфигурации из YAML
     * @param date Дата для проверки
     * @return "odd" - числитель, "even" - знаменатель
     */
    public String getWeekTypeForDate(LocalDate date) {
        try {
            // Получаем настройки из YAML конфигурации
            LocalDate referenceDate = settingsConfig.getReminders().getWeekType().getReferenceDateAsLocalDate();
            String referenceWeekType = settingsConfig.getReminders().getWeekType().getReferenceWeekType();

            log.debug("Определение типа недели: referenceDate={}, referenceWeekType={}, date={}",
                    referenceDate, referenceWeekType, date);

            // Приводим даты к понедельникам для корректного расчета недель
            LocalDate refMonday = referenceDate.with(DayOfWeek.MONDAY);
            LocalDate dateMonday = date.with(DayOfWeek.MONDAY);

            // Рассчитываем количество недель между датами
            long weeksBetween = ChronoUnit.WEEKS.between(refMonday, dateMonday);

            // Определяем тип недели
            boolean isEvenWeek;
            if ("even".equalsIgnoreCase(referenceWeekType)) {
                // Если referenceDate была четной неделей
                isEvenWeek = (weeksBetween % 2 == 0);
            } else {
                // Если referenceDate была нечетной неделей
                isEvenWeek = (weeksBetween % 2 != 0);
            }

            String result = isEvenWeek ? "even" : "odd";
            log.debug("Результат: weeksBetween={}, isEvenWeek={}, weekType={}", weeksBetween, isEvenWeek, result);

            return result;

        } catch (Exception e) {
            log.error("Ошибка определения типа недели: {}", e.getMessage(), e);
            // Возвращаем "odd" как значение по умолчанию при ошибке
            return "odd";
        }
    }

    /**
     * Возвращает отображаемое название типа недели
     */
    public String getWeekTypeDisplayName(String weekType) {
        return settingsConfig.getReminders().getWeekType().getWeekTypeDisplayName(weekType);
    }

    /**
     * Возвращает эмодзи для типа недели
     */
    public String getWeekTypeEmoji(String weekType) {
        return settingsConfig.getReminders().getWeekType().getWeekTypeEmoji(weekType);
    }

    /**
     * Проверяет, является ли указанная дата четной неделей
     */
    public boolean isEvenWeek(LocalDate date) {
        return "even".equals(getWeekTypeForDate(date));
    }

    /**
     * Проверяет, является ли указанная дата нечетной неделей
     */
    public boolean isOddWeek(LocalDate date) {
        return "odd".equals(getWeekTypeForDate(date));
    }

    /**
     * Возвращает информацию о конфигурации недель
     */
    public String getConfigurationInfo() {
        return String.format("""
            Конфигурация недель:
            📅 Дата отсчета: %s
            🗓️ Тип на дату отсчета: %s (%s)
            ⚙️ Текущая дата: %s
            📊 Текущий тип недели: %s (%s)
            """,
                settingsConfig.getReminders().getWeekType().getReferenceDate(),
                settingsConfig.getReminders().getWeekType().getReferenceWeekType(),
                getWeekTypeDisplayName(settingsConfig.getReminders().getWeekType().getReferenceWeekType()),
                LocalDate.now(),
                getCurrentWeekType(),
                getWeekTypeDisplayName(getCurrentWeekType())
        );
    }
}