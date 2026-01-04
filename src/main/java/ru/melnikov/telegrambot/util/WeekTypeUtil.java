package ru.melnikov.telegrambot.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.melnikov.telegrambot.config.BotSettingsConfig;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeekTypeUtil {

    private final BotSettingsConfig settingsConfig;

    /**
     * Определяет текущий тип недели
     */
    public String getCurrentWeekType() {
        return getWeekTypeForDate(LocalDate.now());
    }

    /**
     * Определяет тип недели для указанной даты
     * Вся логика расчета здесь, а не в конфиге!
     */
    public String getWeekTypeForDate(LocalDate date) {
        try {
            // Получаем настройки из конфигурации
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
     * Русское название для типа недели
     */
    public String getWeekTypeDisplayName(String weekType) {
        if (weekType == null) return "ВСЕ";
        return "even".equals(weekType) ? "ЗНАМЕНАТЕЛЬ" : "ЧИСЛИТЕЛЬ";
    }

    /**
     * Эмодзи для типа недели
     */
    public String getWeekTypeEmoji(String weekType) {
        if (weekType == null) return "🔄";
        return "even".equals(weekType) ? "2️⃣" : "1️⃣";
    }

    /**
     * Проверяет, четная ли неделя для даты
     */
    public boolean isEvenWeek(LocalDate date) {
        return "even".equals(getWeekTypeForDate(date));
    }

    /**
     * Проверяет, нечетная ли неделя для даты
     */
    public boolean isOddWeek(LocalDate date) {
        return "odd".equals(getWeekTypeForDate(date));
    }

    /**
     * Форматированная строка с информацией о неделе
     */
    public String formatWeekInfo(LocalDate date) {
        String type = getWeekTypeForDate(date);
        return String.format("%s %s", getWeekTypeEmoji(type), getWeekTypeDisplayName(type));
    }

    /**
     * Форматированная строка для текущей недели
     */
    public String formatCurrentWeekInfo() {
        return formatWeekInfo(LocalDate.now());
    }

    /**
     * Фильтрует расписание для конкретной даты
     */
    public java.util.List<ru.melnikov.telegrambot.model.Schedule> filterScheduleForDate(
            java.util.List<ru.melnikov.telegrambot.model.Schedule> schedules,
            LocalDate date) {

        String targetWeekType = getWeekTypeForDate(date);

        return schedules.stream()
                .filter(s -> {
                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    return scheduleWeekType.equals(targetWeekType) || scheduleWeekType.equals("all");
                })
                .sorted(java.util.Comparator.comparing(ru.melnikov.telegrambot.model.Schedule::getTimeStart))
                .toList();
    }

    /**
     * Получает дату отсчета из конфигурации
     */
    public LocalDate getReferenceDate() {
        return settingsConfig.getReminders().getWeekType().getReferenceDateAsLocalDate();
    }

    /**
     * Получает тип недели отсчета из конфигурации
     */
    public String getReferenceWeekType() {
        return settingsConfig.getReminders().getWeekType().getReferenceWeekType();
    }

    /**
     * Получает отображаемое имя типа недели отсчета
     */
    public String getReferenceWeekTypeDisplayName() {
        String refType = getReferenceWeekType();
        return "even".equals(refType) ? "ЗНАМЕНАТЕЛЬ" : "ЧИСЛИТЕЛЬ";
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
                getReferenceDate(),
                getReferenceWeekType(),
                getWeekTypeDisplayName(getReferenceWeekType()),
                LocalDate.now(),
                getCurrentWeekType(),
                getWeekTypeDisplayName(getCurrentWeekType())
        );
    }
}