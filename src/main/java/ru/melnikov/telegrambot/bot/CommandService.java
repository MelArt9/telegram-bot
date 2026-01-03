package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.melnikov.telegrambot.bot.context.CommandContext;
import ru.melnikov.telegrambot.model.Deadline;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.service.*;
import ru.melnikov.telegrambot.util.TelegramUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommandService {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final DeadlineService deadlineService;
    private final LinkService linkService;
    private final GroupService groupService;
    private final KeyboardFactory keyboardFactory;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Locale RUSSIAN_LOCALE = new Locale("ru");

    // Дата начала отсчета для определения четности недели
    // Это должна быть дата, когда неделя была четной (или нечетной)
    private static final LocalDate REFERENCE_DATE_EVEN_WEEK = LocalDate.of(2024, 9, 2); // Пример: 2 сентября 2024 была четная неделя

    public SendMessage handle(CommandType type, CommandContext ctx) {
        return switch (type) {
            case START -> start(ctx);
            case TODAY -> today(ctx);
            case DAY -> day(ctx);
            case WEEK -> week(ctx);
            case DEADLINES -> deadlines(ctx);
            case LINKS -> links(ctx);
            case TAG -> tag(ctx);
            case HELP -> help(ctx);
            default -> unknown(ctx);
        };
    }

    private SendMessage unknown(CommandContext ctx) {
        return reply(ctx, "❌ *Неизвестная команда*\n\nВведите /help для списка команд");
    }

    private SendMessage reply(CommandContext ctx, String text) {
        return SendMessage.builder()
                .chatId(ctx.getChatId())
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(keyboardFactory.defaultKeyboard())
                .build();
    }

    private SendMessage start(CommandContext ctx) {
        userService.registerIfNotExists(
                ctx.getUser().getId(),
                ctx.getUser().getUserName(),
                ctx.getUser().getFirstName(),
                ctx.getUser().getLastName()
        );

        String welcomeMessage = """
                🎉 *Добро пожаловать в учебный помощник!* 🎉
                
                ✨ *Я помогу вам с:*
                📅 Расписанием занятий
                ⏰ Контролем дедлайнов
                🔗 Полезными ресурсами
                👥 Упоминанием групп
                
                💡 *Для начала работы используйте команды:*
                /today – расписание на сегодня
                /help – все команды помощника
                
                🚀 *Приятного использования!*
                """;

        return reply(ctx, welcomeMessage);
    }

    private SendMessage today(CommandContext ctx) {
        // Автоматически определяем текущую неделю
        String currentWeekType = getCurrentWeekType();
        String weekTypeDisplay = getWeekTypeDisplayName(currentWeekType);
        String weekTypeEmoji = getWeekTypeEmoji(currentWeekType);

        List<Schedule> scheduleList = scheduleService.findEntitiesToday();

        // Фильтруем расписание для текущей недели (текущий тип + all)
        List<Schedule> filteredScheduleList = scheduleList.stream()
                .filter(s -> {
                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                })
                .sorted(Comparator.comparing(Schedule::getTimeStart))
                .toList();

        if (filteredScheduleList.isEmpty()) {
            return reply(ctx, String.format("""
                    📭 *Сегодня занятий нет!* 📭
                    🗓️ *Тип недели:* %s %s
                    
                    🎉 *Можно отдохнуть или заняться саморазвитием:*
                    • Повторите пройденный материал
                    • Подготовьтесь к будущим занятиям
                    • Отдохните и наберитесь сил
                    
                    💡 *Что дальше?*
                    /day [1-7] – посмотреть другой день
                    /deadlines – проверить дедлайны
                    """, weekTypeEmoji, weekTypeDisplay));
        }

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String dayName = today.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);

        String scheduleText = formatScheduleList(filteredScheduleList, dayName, "сегодня", currentWeekType);
        int totalPairs = filteredScheduleList.size();
        int onlinePairs = countOnlinePairs(filteredScheduleList);
        int offlinePairs = totalPairs - onlinePairs;

        LocalTime firstTime = getFirstPairTime(filteredScheduleList);
        LocalTime lastTime = getLastPairTime(filteredScheduleList);

        String response = String.format("""
                📋 *РАСПИСАНИЕ НА СЕГОДНЯ* 📋
                *%s* | %s %s
                
                %s
                
                📊 *Статистика дня:*
                📝 Всего пар: %d
                🏫 Очных: %d
                💻 Онлайн: %d
                
                ⏰ *Временные границы:*
                🕐 Начало: %s
                🕔 Конец: %s
                
                💡 *Другие команды:*
                /day [1-7] – другой день недели
                /deadlines – дедлайны работ
                """,
                dayName.substring(0, 1).toUpperCase() + dayName.substring(1),
                weekTypeEmoji, weekTypeDisplay,
                scheduleText,
                totalPairs,
                offlinePairs,
                onlinePairs,
                firstTime != null ? firstTime.format(TIME_FORMATTER) : "—",
                lastTime != null ? lastTime.format(TIME_FORMATTER) : "—");

        return reply(ctx, response);
    }

    private SendMessage day(CommandContext ctx) {
        if (ctx.getArgs().length < 2) {
            return reply(ctx, """
                    📝 *Использование команды /day*
                    
                    🔢 *Формат:* `/day [номер дня]`
                    • 1 – Понедельник
                    • 2 – Вторник
                    • 3 – Среда
                    • 4 – Четверг
                    • 5 – Пятница
                    • 6 – Суббота
                    • 7 – Воскресенье
                    
                    💡 *Пример:* `/day 3` – расписание на среду
                    
                    ⚠️ *Примечание:* Бот автоматически определяет тип текущей недели
                    и показывает расписание для соответствующего типа (четная/нечетная)
                    """);
        }

        try {
            int dayNumber = Integer.parseInt(ctx.arg(1));
            if (dayNumber < 1 || dayNumber > 7) {
                return reply(ctx, "❌ *Некорректный номер дня*\n\nВведите число от 1 до 7");
            }

            // Автоматически определяем текущую неделю
            String currentWeekType = getCurrentWeekType();
            String weekTypeDisplay = getWeekTypeDisplayName(currentWeekType);
            String weekTypeEmoji = getWeekTypeEmoji(currentWeekType);

            DayOfWeek dayOfWeek = DayOfWeek.of(dayNumber);
            String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);
            String dayNameCapitalized = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            // Получаем ВСЕ расписания для этого дня недели
            List<Schedule> allScheduleList = scheduleService.findEntitiesByDay(dayNumber);

            // Фильтруем: показываем только расписание для текущего типа недели + all
            List<Schedule> filteredScheduleList = allScheduleList.stream()
                    .filter(s -> {
                        String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                        return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                    })
                    .sorted(Comparator.comparing(Schedule::getTimeStart))
                    .toList();

            if (filteredScheduleList.isEmpty()) {
                return reply(ctx, String.format("""
                        📭 *В %s занятий нет* 📭
                        🗓️ *Тип недели:* %s %s
                        
                        🎉 *Это день для:*
                        • Самостоятельной подготовки
                        • Отдыха и восстановления
                        • Работы над проектами
                        
                        💡 *Проверьте другие дни:*
                        /today – сегодня
                        /deadlines – дедлайны
                        /week %s – вся неделя
                        """,
                        dayNameCapitalized,
                        weekTypeEmoji, weekTypeDisplay,
                        currentWeekType.equals("odd") ? "even" : "odd"));
            }

            // Форматируем расписание с указанием типа недели
            String scheduleText = formatScheduleList(filteredScheduleList, dayNameCapitalized, "этот день", currentWeekType);
            int totalPairs = filteredScheduleList.size();
            int onlinePairs = countOnlinePairs(filteredScheduleList);
            int offlinePairs = totalPairs - onlinePairs;

            String response = String.format("""
                    📅 *РАСПИСАНИЕ: %s* 📅
                    🗓️ *Тип недели:* %s %s
                    
                    %s
                    
                    📊 *Статистика дня:*
                    📝 Всего пар: %d
                    🏫 Очных: %d
                    💻 Онлайн: %d
                    
                    💡 *Быстрые команды:*
                    /today – сегодняшнее расписание
                    /week %s – вся неделя
                    /help – все команды
                    """,
                    dayNameCapitalized.toUpperCase(),
                    weekTypeEmoji, weekTypeDisplay,
                    scheduleText,
                    totalPairs,
                    offlinePairs,
                    onlinePairs,
                    currentWeekType.equals("odd") ? "even" : "odd");

            return reply(ctx, response);
        } catch (NumberFormatException e) {
            return reply(ctx, """
                    ❌ *Ошибка ввода*
                    
                    🔢 *Правильный формат:* `/day [номер дня]`
                    
                    📆 *Номера дней недели:*
                    ├ 1 – Понедельник
                    ├ 2 – Вторник
                    ├ 3 – Среда
                    ├ 4 – Четверг
                    ├ 5 – Пятница
                    ├ 6 – Суббота
                    └ 7 – Воскресенье
                    
                    💡 *Пример:* `/day 3` для среды
                    
                    ⚠️ *Примечание:* Бот автоматически определяет тип текущей недели
                    и показывает расписание для соответствующего типа (четная/нечетная)
                    """);
        }
    }

    private SendMessage week(CommandContext ctx) {
        // Если нет аргументов - показываем текущую неделю
        if (ctx.getArgs().length < 2) {
            // Автоматически определяем текущую неделю
            String currentWeekType = getCurrentWeekType();
            String weekTypeDisplay = getWeekTypeDisplayName(currentWeekType);
            String weekTypeEmoji = getWeekTypeEmoji(currentWeekType);

            // Получаем все расписание
            List<Schedule> allSchedules = scheduleService.findAllEntities();

            // Фильтруем: показываем пары для текущего типа недели + пары с week_type = 'all'
            List<Schedule> filteredSchedules = allSchedules.stream()
                    .filter(s -> {
                        String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                        // Показываем если:
                        // 1. Тип недели совпадает с текущим (odd/even)
                        // 2. Или тип недели = "all" (показывается всегда)
                        return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                    })
                    .sorted(Comparator.comparing(Schedule::getDayOfWeek)
                            .thenComparing(Schedule::getTimeStart))
                    .toList();

            return formatWeekSchedule(ctx, filteredSchedules, currentWeekType);
        }

        // Если есть аргументы - используем указанный тип недели
        String weekType = ctx.arg(1).toLowerCase();

        // Проверяем корректность типа недели
        if (!weekType.equals("odd") && !weekType.equals("even")) {
            return reply(ctx, """
                ❌ *Некорректный тип недели*
                
                📊 *Доступные типы:*
                • *odd* – неделя числителя
                • *even* – неделя знаменателя
                
                💡 *Пример:* `/week odd` или просто `/week` для текущей недели
                """);
        }

        // Получаем все расписание
        List<Schedule> allSchedules = scheduleService.findAllEntities();

        // Фильтруем: показываем пары для запрошенного типа недели + пары с week_type = 'all'
        List<Schedule> filteredSchedules = allSchedules.stream()
                .filter(s -> {
                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    // Показываем если:
                    // 1. Тип недели совпадает с запрошенным (odd/even)
                    // 2. Или тип недели = "all" (показывается всегда)
                    return scheduleWeekType.equals(weekType) || scheduleWeekType.equals("all");
                })
                .sorted(Comparator.comparing(Schedule::getDayOfWeek)
                        .thenComparing(Schedule::getTimeStart))
                .toList();

        return formatWeekSchedule(ctx, filteredSchedules, weekType);
    }

    private SendMessage formatWeekSchedule(CommandContext ctx, List<Schedule> schedules, String weekType) {
        if (schedules.isEmpty()) {
            String weekTypeName = getWeekTypeDisplayName(weekType);
            return reply(ctx, String.format("""
                📭 *На %s неделю пар нет*
                
                🎉 *Можно заняться:*
                • Самостоятельной подготовкой
                • Работой над проектами
                • Отдыхом и восстановлением
                
                💡 *Проверьте другую неделю:*
                /week %s
                """,
                    weekTypeName,
                    weekType.equals("odd") ? "even" : "odd"));
        }

        // Группируем расписание по дням недели
        Map<Integer, List<Schedule>> scheduleByDay = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getDayOfWeek));

        // Формируем вывод
        StringBuilder response = new StringBuilder();
        String weekTypeName = getWeekTypeDisplayName(weekType);
        String weekTypeEmoji = getWeekTypeEmoji(weekType);

        response.append(String.format("%s *НЕДЕЛЯ %s* %s\n\n",
                weekTypeEmoji,
                weekTypeName.toUpperCase(),
                weekTypeEmoji));

        // Русские названия дней недели
        String[] dayNames = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};

        // Сортируем дни недели
        List<Integer> sortedDays = scheduleByDay.keySet().stream()
                .sorted()
                .toList();

        int totalPairs = 0;
        int onlinePairs = 0;

        for (Integer day : sortedDays) {
            List<Schedule> daySchedules = scheduleByDay.get(day);
            if (daySchedules != null && !daySchedules.isEmpty()) {
                String dayName = dayNames[day - 1];
                response.append(String.format("📅 *%s*\n", dayName));

                // Сортируем пары по времени
                daySchedules.sort(Comparator.comparing(Schedule::getTimeStart));

                for (Schedule s : daySchedules) {
                    totalPairs++;

                    if (s.getIsOnline() != null && s.getIsOnline()) {
                        onlinePairs++;
                    }

                    String timeRange = String.format("%s-%s",
                            s.getTimeStart().format(TIME_FORMATTER),
                            s.getTimeEnd().format(TIME_FORMATTER));

                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    String typeEmoji = getWeekTypeEmoji(scheduleWeekType);
                    String onlineEmoji = (s.getIsOnline() != null && s.getIsOnline()) ? "💻" : "🏫";

                    response.append(String.format("%s %s\n", typeEmoji, onlineEmoji))
                            .append(String.format("   ⏰ *%s*\n", timeRange))
                            .append(String.format("   📖 %s\n", s.getSubject()));

                    if (s.getTeacher() != null && !s.getTeacher().isBlank()) {
                        response.append(String.format("   👨‍🏫 %s\n", s.getTeacher()));
                    }

                    if (s.getLocation() != null && !s.getLocation().isBlank()) {
                        response.append(String.format("   📍 %s\n", s.getLocation()));
                    }

                    response.append("\n");
                }

                // Добавляем разделитель между днями
                response.append("──────────\n\n");
            }
        }

        // Убираем последний лишний разделитель
        if (response.length() > 0) {
            int lastIndex = response.lastIndexOf("──────────\n\n");
            if (lastIndex == response.length() - "──────────\n\n".length()) {
                response.delete(lastIndex, response.length());
                response.append("\n");
            }
        }

        // Добавляем упрощенную статистику
        int offlinePairs = totalPairs - onlinePairs;

        response.append(String.format("""
            📊 *СТАТИСТИКА:*
            
            📝 Всего пар: %d
            🏫 Очных: %d
            💻 Онлайн: %d
            
            💡 *Другие команды:*
            /today – сегодня
            /day [1-7] – по дням недели
            /week %s – другая неделя
            """,
                totalPairs,
                offlinePairs,
                onlinePairs,
                weekType.equals("odd") ? "even" : "odd"));

        return reply(ctx, response.toString());
    }

    private SendMessage deadlines(CommandContext ctx) {
        // Получаем ВСЕ дедлайны
        var allDeadlines = deadlineService.findAllDeadlinesSorted();

        // Фильтруем дедлайны:
        // 1. Показываем все будущие дедлайны (deadlineAt > now)
        // 2. Показываем просроченные, но не более чем на 7 дней
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        var filteredDeadlines = allDeadlines.stream()
                .filter(d -> {
                    LocalDateTime deadlineTime = d.getDeadlineAt();
                    // Показываем если:
                    // 1. Дедлайн в будущем
                    // 2. ИЛИ дедлайн просрочен, но не более чем на 7 дней
                    return deadlineTime.isAfter(now) ||
                            (deadlineTime.isBefore(now) && deadlineTime.isAfter(sevenDaysAgo));
                })
                .toList();

        if (filteredDeadlines.isEmpty()) {
            return reply(ctx, """
                ✅ *Все дедлайны выполнены!* ✅
                
                🎉 *Отличная работа!* 🎉
                Все задания сданы вовремя.
                
                📚 *Что можно сделать дальше:*
                • Заняться дополнительными материалами
                • Подготовиться к следующей неделе
                • Отдохнуть и восстановить силы
                
                💡 *Другие команды:*
                /links – полезные ресурсы
                /today – расписание
                """);
        }

        // Сортируем по дате дедлайна
        List<Deadline> sortedDeadlines = new ArrayList<>(filteredDeadlines);
        sortedDeadlines.sort((d1, d2) -> {
            boolean d1Overdue = d1.getDeadlineAt().isBefore(now);
            boolean d2Overdue = d2.getDeadlineAt().isBefore(now);

            if (d1Overdue && d2Overdue) {
                return d2.getDeadlineAt().compareTo(d1.getDeadlineAt());
            } else if (d1Overdue) {
                return -1;
            } else if (d2Overdue) {
                return 1;
            } else {
                return d1.getDeadlineAt().compareTo(d2.getDeadlineAt());
            }
        });

        StringBuilder formatted = new StringBuilder();
        int urgentCount = 0;
        int nearCount = 0;
        int futureCount = 0;
        int overdueCount = 0;

        int deadlineCount = sortedDeadlines.size();

        for (int i = 0; i < deadlineCount; i++) {
            var deadline = sortedDeadlines.get(i);
            LocalDateTime deadlineTime = deadline.getDeadlineAt();

            // Рассчитываем разницу в днях
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    now.toLocalDate(),
                    deadlineTime.toLocalDate()
            );

            String emoji;
            String daysText;

            // Проверяем, просрочен ли дедлайн
            if (deadlineTime.isBefore(now)) {
                emoji = "🔴 (ПРОСРОЧЕНО)";
                overdueCount++;

                long daysOverdue = Math.abs(daysBetween);
                if (daysOverdue == 0) {
                    long hoursOverdue = java.time.temporal.ChronoUnit.HOURS.between(deadlineTime, now);
                    if (hoursOverdue < 24) {
                        daysText = String.format("⚠️ Просрочено на %d ч.", hoursOverdue);
                    } else {
                        daysText = "⚠️ Просрочено сегодня";
                    }
                } else if (daysOverdue == 1) {
                    daysText = "⚠️ Просрочено на 1 день";
                } else {
                    daysText = String.format("⚠️ Просрочено на %d д.", daysOverdue);
                }
            }
            // Дедлайн сегодня
            else if (daysBetween == 0) {
                long hoursLeft = java.time.temporal.ChronoUnit.HOURS.between(now, deadlineTime);
                if (hoursLeft <= 12) {
                    emoji = "🔴 (СЕГОДНЯ)";
                    urgentCount++;
                    daysText = String.format("⏰ Осталось %d ч.", hoursLeft);
                } else {
                    emoji = "🟡 (СЕГОДНЯ)";
                    nearCount++;
                    daysText = "⏰ Сдать сегодня";
                }
            }
            // Срочные (менее 3 дней)
            else if (daysBetween <= 2) {
                emoji = "🔴";
                urgentCount++;
                if (daysBetween == 1) {
                    daysText = "⏳ Остался 1 день";
                } else {
                    daysText = String.format("⏳ Осталось %d д.", daysBetween);
                }
            }
            // Ближайшие (менее 7 дней)
            else if (daysBetween <= 7) {
                emoji = "🟡";
                nearCount++;
                daysText = String.format("⏳ Осталось %d д.", daysBetween);
            }
            // Будущие (более 7 дней)
            else {
                emoji = "🟢";
                futureCount++;
                daysText = String.format("⏳ Осталось %d д.", daysBetween);
            }

            formatted.append(String.format("%s *%s*\n", emoji, deadline.getTitle()))
                    .append(String.format("   📅 %s\n", deadlineTime.format(DATETIME_FORMATTER)))
                    .append(String.format("   📝 %s\n",
                            deadline.getDescription() != null && !deadline.getDescription().isBlank() ?
                                    deadline.getDescription() : "Описание отсутствует"));

            // Добавляем ссылку, если она есть
            if (deadline.getLinkUrl() != null && !deadline.getLinkUrl().isBlank()) {
                String linkText = deadline.getLinkText() != null && !deadline.getLinkText().isBlank()
                        ? deadline.getLinkText()
                        : "Ссылка на задание";
                formatted.append(String.format("   🔗 [%s](%s)\n", linkText, deadline.getLinkUrl()));
            }

            formatted.append(String.format("   %s\n", daysText));

            // Добавляем разделитель между дедлайнами, но не после последнего
            if (i < deadlineCount - 1) {
                formatted.append("\n──────────\n\n");
            }
        }

        // Обновляем статистику
        String response = String.format("""
            ⏰ *АКТУАЛЬНЫЕ ДЕДЛАЙНЫ* ⏰
            
            %s
            📈 *Статистика:*
            🔴 Просрочено (< 7 д.): %d
            🔴 Срочных (< 3 дней): %d
            🟡 Ближайших (< 7 дней): %d
            🟢 Будущих (> 7 дней): %d
            📊 Всего: %d
            
            💡 *Не забывайте:*
            • Начинайте работу заранее
            • Распределяйте нагрузку равномерно
            • Делайте перерывы для эффективности
            
            🚀 *У вас всё получится!*
            """,
                formatted.toString(),
                overdueCount,
                urgentCount,
                nearCount,
                futureCount,
                sortedDeadlines.size());

        return reply(ctx, response);
    }

    private SendMessage links(CommandContext ctx) {
        var links = linkService.findAll();

        if (links.isEmpty()) {
            return reply(ctx, """
                    🔗 *Ссылки временно отсутствуют*
                    
                    📚 *Полезные ресурсы для учёбы:*
                    • Онлайн-курсы (Coursera, Stepik)
                    • Документация по технологиям
                    • Официальные сайты университета
                    
                    💡 *Администратор скоро добавит актуальные ссылки*
                    
                    ⚡ *Другие команды:*
                    /today – расписание
                    /deadlines – дедлайны
                    """);
        }

        StringBuilder formatted = new StringBuilder();
        for (var link : links) {
            formatted.append(String.format("• [%s](%s)%n",
                    link.getTitle(),
                    link.getUrl()));
        }

        String response = String.format("""
                🔗 *ПОЛЕЗНЫЕ РЕСУРСЫ* 🔗
                
                %s
                📌 *Рекомендации по использованию:*
                • Сохраните важные ссылки в закладки
                • Регулярно проверяйте обновления
                • Используйте для подготовки к занятиям
                """, formatted.toString());

        return reply(ctx, response);
    }

    private SendMessage tag(CommandContext ctx) {
        if (ctx.getArgs().length < 2) {
            return reply(ctx, """
                👥 *УПОМИНАНИЕ ГРУППЫ* 👥
                
                🔧 *Формат:* `/tag [название_группы]`
                
                📋 *Доступные группы:*
                • all – все пользователи
                • starosta – староста
                
                💡 *Пример:* `/tag all` – упомянуть всех
                """);
        }

        String groupName = ctx.getArgs()[1].toLowerCase();

        return groupService.findByName(groupName)
                .map(group -> {
                    if (group.getUsers().isEmpty()) {
                        return reply(ctx, String.format("""
                            👤 *Группа "%s" пуста*
                            
                            📭 *В этой группе пока нет участников*
                            
                            💡 *Что можно сделать:*
                            • Пригласить участников в группу
                            • Проверить другие группы
                            • Обратиться к администратору
                            """, groupName));
                    }

                    StringBuilder users = new StringBuilder();
                    for (var user : group.getUsers()) {
                        if (user.getUsername() != null && !user.getUsername().isBlank() && user.getTelegramId() != null) {
                            // Формируем кликабельную ссылку на пользователя
                            String username = user.getUsername();
                            Long telegramId = user.getTelegramId();

                            // Формат: [@username](tg://user?id=telegram_id)
                            String mentionLink = String.format("[@%s](tg://user?id=%d)", username, telegramId);

                            // Добавляем имя пользователя, если есть
                            if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
                                users.append(String.format("👤 %s %s\n", user.getFirstName(), mentionLink));
                            } else {
                                users.append(String.format("👤 %s\n", mentionLink));
                            }
                        } else if (user.getFirstName() != null && !user.getFirstName().isBlank() && user.getTelegramId() != null) {
                            // Если нет username, но есть telegramId, делаем ссылку на имя
                            String mentionLink = String.format("[%s](tg://user?id=%d)",
                                    user.getFirstName(), user.getTelegramId());
                            users.append(String.format("👤 %s\n", mentionLink));
                        } else {
                            // Если ничего нет, просто показываем информацию
                            String displayName = user.getFirstName() != null ? user.getFirstName() :
                                    (user.getUsername() != null ? "@" + user.getUsername() :
                                            "Пользователь #" + user.getId());
                            users.append(String.format("👤 %s\n", displayName));
                        }
                    }

                    return reply(ctx, String.format("""
                        📢 *УПОМИНАНИЕ ГРУППЫ: %s* 📢
                        
                        %s
                        👥 *Участников всего:* %d
                        
                        ⚠️ *Пожалуйста, не злоупотребляйте упоминаниями*
                        """,
                            groupName.toUpperCase(),
                            users.toString(),
                            group.getUsers().size()));
                })
                .orElse(reply(ctx, String.format("""
                    ❌ *Группа не найдена* ❌
                    
                    Группа *"%s"* не существует или была удалена.
                    
                    🔍 *Проверьте правильность названия*
                    /help – список доступных команд
                    
                    📋 *Возможные группы:*
                    • all – все пользователи
                    • starosta – староста
                    
                    💡 *Обратитесь к администратору для уточнения*
                    """, groupName)));
    }

    private SendMessage help(CommandContext ctx) {
        return reply(ctx, """
            🤖 *КОМАНДЫ УЧЕБНОГО ПОМОЩНИКА* 🤖
            
            ┏━━━━━━━━━━━┓
            ┃     📚 РАСПИСАНИЕ     ┃
            ┗━━━━━━━━━━━┛
            
            📅 /today – Расписание на сегодня
            📆 /day [1-7] – Расписание по дню недели
            🗓️ /week – Расписание на текущую неделю (автоопределение)
            🗓️ /week [odd/even] – Расписание на указанную неделю
            
            ┏━━━━━━━━━━━┓
            ┃  ⏰ УЧЕБНЫЙ ПЛАН   ┃
            ┗━━━━━━━━━━━┛
            
            ⏳ /deadlines – Дедлайны работ
            🔗 /links – Полезные ресурсы
            
            ┏━━━━━━━━━━━┓
            ┃ 👥 КОММУНИКАЦИЯ  ┃
            ┗━━━━━━━━━━━┛
            
            📢 /tag [группа] – Упомянуть группу
            
            ┏━━━━━━━━━━━┓
            ┃        ⚙️  СИСТЕМА          ┃
            ┗━━━━━━━━━━━┛
            
            🚀 /start – Перезапуск бота
            ❓ /help – Эта справка
            
            ━━━━━━━━━━━━━━━━
            
            📝 *Примеры использования:*
            • `/day 3` – расписание на среду (автоопределение недели)
            • `/week` – текущая неделя (автоопределение)
            • `/week odd` – неделя числитель
            • `/week even` – неделя знаменатель
            • `/tag all` – упомянуть всех
            • `/deadlines` – посмотреть дедлайны
            
            💡 *Совет:* Используйте встроенные кнопки для быстрого доступа!
            
            🎓 *Успешной учёбы!*
            """);
    }

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ФОРМАТИРОВАНИЯ ======

    private String formatScheduleList(List<Schedule> scheduleList, String dayName, String context, String weekType) {
        if (scheduleList.isEmpty()) {
            return String.format("На %s занятий нет.", context);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📚 *Пары на ").append(context).append(":*\n\n");

        for (int i = 0; i < scheduleList.size(); i++) {
            Schedule s = scheduleList.get(i);
            String timeRange = String.format("%s-%s",
                    s.getTimeStart().format(TIME_FORMATTER),
                    s.getTimeEnd().format(TIME_FORMATTER));

            String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
            String weekTypeEmoji = getWeekTypeEmoji(scheduleWeekType);
            Boolean isOnline = s.getIsOnline();
            String onlineEmoji = (isOnline != null && isOnline) ? "💻" : "🏫";
            String locationInfo = (isOnline != null && isOnline) ?
                    "Онлайн" : (s.getLocation() != null ? s.getLocation() : "Ауд. не указана");

            // Добавляем информацию о типе недели для пары
            String weekTypeText = "";
            if (!scheduleWeekType.equals("all")) {
                weekTypeText = String.format(" (%s)",
                        scheduleWeekType.equals("odd") ? "числитель" : "знаменатель");
            }

            sb.append(String.format("%d. %s %s\n", i + 1, weekTypeEmoji, onlineEmoji))
                    .append(String.format("   ⏰ *%s*\n", timeRange))
                    .append(String.format("   📖 %s%s\n", s.getSubject(), weekTypeText))
                    .append(String.format("   👨‍🏫 %s\n",
                            s.getTeacher() != null ? s.getTeacher() : "Преподаватель не указан"))
                    .append(String.format("   📍 %s\n", locationInfo))
                    .append("\n");
        }

        return sb.toString();
    }

    private String getWeekTypeEmoji(String weekType) {
        if (weekType == null) {
            return "🔄"; // Для null показываем как "all"
        }
        return switch (weekType.toLowerCase()) {
            case "odd" -> "1️⃣";
            case "even" -> "2️⃣";
            case "all" -> "🔄";
            default -> "🔄"; // По умолчанию
        };
    }

    private String getWeekTypeDisplayName(String weekType) {
        return switch (weekType.toLowerCase()) {
            case "odd" -> "ЧИСЛИТЕЛЬ";
            case "even" -> "ЗНАМЕНАТЕЛЬ";
            default -> weekType.toUpperCase();
        };
    }

    private LocalTime getFirstPairTime(List<Schedule> scheduleList) {
        return scheduleList.stream()
                .map(Schedule::getTimeStart)
                .min(LocalTime::compareTo)
                .orElse(null);
    }

    private LocalTime getLastPairTime(List<Schedule> scheduleList) {
        return scheduleList.stream()
                .map(Schedule::getTimeEnd)
                .max(LocalTime::compareTo)
                .orElse(null);
    }

    private int countOnlinePairs(List<Schedule> scheduleList) {
        return (int) scheduleList.stream()
                .filter(s -> s.getIsOnline() != null && s.getIsOnline())
                .count();
    }

    /**
     * Определяет текущий тип недели (четная/нечетная)
     * на основе установленной даты отсчета
     *
     * @return "odd" - нечетная, "even" - четная
     */
    private String getCurrentWeekType() {
        LocalDate today = LocalDate.now();

        // Определяем разницу в неделях между сегодняшней датой и эталонной
        long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(
                REFERENCE_DATE_EVEN_WEEK.with(DayOfWeek.MONDAY),
                today.with(DayOfWeek.MONDAY)
        );

        // Если разница четная - текущая неделя четная (even)
        // Если разница нечетная - текущая неделя нечетная (odd)
        return weeksBetween % 2 == 0 ? "even" : "odd";
    }
}