package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.melnikov.telegrambot.bot.context.CommandContext;
import ru.melnikov.telegrambot.config.BotSettingsConfig;
import ru.melnikov.telegrambot.model.BotChat;
import ru.melnikov.telegrambot.model.Deadline;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.service.*;

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
@Slf4j
public class CommandService {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final DeadlineService deadlineService;
    private final LinkService linkService;
    private final GroupService groupService;
    private final KeyboardFactory keyboardFactory;
    private final BotChatService botChatService;
    private final PerformanceMonitor performanceMonitor;
    private final AdminCheckService adminCheckService;
    private final BotSettingsConfig settingsConfig;
    private final WeekTypeService weekTypeService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Locale RUSSIAN_LOCALE = new Locale("ru");

    public SendMessage handle(CommandType type, CommandContext ctx) {
        // Автоматическое логирование через аспект
        performanceMonitor.incrementCommand(type.name());

        return switch (type) {
            case START -> start(ctx);
            case TODAY -> today(ctx);
            case DAY -> day(ctx);
            case WEEK -> week(ctx);
            case DEADLINES -> deadlines(ctx);
            case LINKS -> links(ctx);
            case TAG -> tag(ctx);
            case HELP -> help(ctx);
            case REMINDERS -> reminders(ctx);
            case SETTINGS -> settings(ctx);
            case ADMIN -> admin(ctx);
            case SETTOPIC -> setTopic(ctx);
            default -> unknown(ctx);
        };
    }

    private SendMessage unknown(CommandContext ctx) {
        return buildReply(ctx, "❌ *Неизвестная команда*\n\nВведите /help для списка команд");
    }

    private SendMessage reply(CommandContext ctx, String text) {
        return buildReply(ctx, text);
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

        return buildReply(ctx, welcomeMessage);
    }

    private SendMessage admin(CommandContext ctx) {
        var user = ctx.getUser();
        String username = user.getUserName();
        Long userId = user.getId();

        boolean isAdmin = adminCheckService.isAdmin(username, userId);

        if (isAdmin) {
            return reply(ctx, String.format("""
            👑 *СТАТУС АДМИНИСТРАТОРА*
            
            ✅ *Вы являетесь администратором бота!*
            
            📋 *Данные:*
            • Username: @%s
            • User ID: %d
            • Статус: ✅ АДМИНИСТРАТОР
            
            🔧 *Доступные команды:*
            • /reminders – управление напоминаниями
            • /settings – настройки группы
            • /tag all – упомянуть всех участников
            
            ⚠️ *Будьте осторожны с настройками!*
            """,
                    username != null ? username : "unknown",
                    userId));
        } else {
            return reply(ctx, String.format("""
            👑 *СТАТУС АДМИНИСТРАТОРА*
            
            ❌ *Вы НЕ являетесь администратором бота*
            
            📋 *Данные:*
            • Username: @%s
            • User ID: %d
            • Статус: ❌ НЕ АДМИНИСТРАТОР
            
            💡 *Только администраторы могут:*
            • Настраивать напоминания (/reminders)
            • Изменять настройки группы (/settings)
            • Упоминать всех участников (/tag all)
            
            🔒 *Обратитесь к владельцу бота для получения прав*
            """,
                    username != null ? username : "unknown",
                    userId));
        }
    }

    private SendMessage today(CommandContext ctx) {
        // Используем WeekTypeService для определения типа недели
        String currentWeekType = weekTypeService.getCurrentWeekType();
        String weekTypeDisplay = weekTypeService.getWeekTypeDisplayName(currentWeekType);
        String weekTypeEmoji = weekTypeService.getWeekTypeEmoji(currentWeekType);

        // Получаем номер дня недели (1-7)
        DayOfWeek todayDayOfWeek = LocalDate.now().getDayOfWeek();
        int dayNumber = todayDayOfWeek.getValue();

        // Получаем ВСЕ расписания для этого дня недели
        List<Schedule> allScheduleList = scheduleService.findEntitiesByDay(dayNumber);

        // ✅ ИСПРАВЛЕНИЕ: Фильтруем как в методе day()
        List<Schedule> filteredScheduleList = allScheduleList.stream()
                .filter(s -> {
                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                    return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                })
                .sorted(Comparator.comparing(Schedule::getTimeStart))
                .toList();

        // Логируем для отладки
        log.debug("Сегодня день: {} (номер: {}), тип недели: {} ({}), всего пар в БД: {}, после фильтрации: {}",
                todayDayOfWeek.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE),
                dayNumber,
                currentWeekType, weekTypeDisplay,
                allScheduleList.size(),
                filteredScheduleList.size());

        if (filteredScheduleList.isEmpty()) {
            return reply(ctx, String.format("""
                📭 *Сегодня занятий нет!* 📭
                📅 *День:* %s
                🗓️ *Тип недели:* %s %s
                
                🎉 *Можно отдохнуть или заняться саморазвитием:*
                • Повторите пройденный материал
                • Подготовьтесь к будущим занятиям
                • Отдохните и наберитесь сил
                
                💡 *Что дальше?*
                /day [1-7] – посмотреть другой день
                /deadlines – проверить дедлайны
                """,
                    todayDayOfWeek.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE),
                    weekTypeEmoji, weekTypeDisplay));
        }

        String dayName = todayDayOfWeek.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);

        // Форматируем расписание
        StringBuilder scheduleText = new StringBuilder();

        for (int i = 0; i < filteredScheduleList.size(); i++) {
            Schedule s = filteredScheduleList.get(i);
            String timeRange = String.format("%s-%s",
                    s.getTimeStart().format(TIME_FORMATTER),
                    s.getTimeEnd().format(TIME_FORMATTER));

            String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";

            // Эмодзи для каждой пары
            String pairWeekTypeEmoji;
            String pairWeekTypeText;

            if ("odd".equals(scheduleWeekType)) {
                pairWeekTypeEmoji = "1️⃣";
                pairWeekTypeText = "числитель";
            } else if ("even".equals(scheduleWeekType)) {
                pairWeekTypeEmoji = "2️⃣";
                pairWeekTypeText = "знаменатель";
            } else {
                pairWeekTypeEmoji = "🔄";
                pairWeekTypeText = "обе недели";
            }

            String onlineEmoji = (s.getIsOnline() != null && s.getIsOnline()) ? "💻" : "🏫";

            scheduleText.append(String.format("%d. %s %s\n", i + 1, pairWeekTypeEmoji, onlineEmoji))
                    .append(String.format("   ⏰ *%s*\n", timeRange))
                    .append(String.format("   📖 %s (%s)\n", s.getSubject(), pairWeekTypeText));

            if (s.getTeacher() != null && !s.getTeacher().isBlank()) {
                scheduleText.append(String.format("   👨‍🏫 %s\n", s.getTeacher()));
            }

            if (s.getLocation() != null && !s.getLocation().isBlank()) {
                String location = (s.getIsOnline() != null && s.getIsOnline()) ?
                        "Онлайн" : s.getLocation();
                scheduleText.append(String.format("   📍 %s\n", location));
            }

            scheduleText.append("\n");
        }

        int totalPairs = filteredScheduleList.size();
        int onlinePairs = (int) filteredScheduleList.stream()
                .filter(s -> s.getIsOnline() != null && s.getIsOnline())
                .count();
        int offlinePairs = totalPairs - onlinePairs;

        LocalTime firstTime = getFirstPairTime(filteredScheduleList);
        LocalTime lastTime = getLastPairTime(filteredScheduleList);

        String response = String.format("""
            📋 *РАСПИСАНИЕ НА СЕГОДНЯ* 📋
            📅 *День:* %s
            🗓️ *Тип недели:* %s %s
            
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
                scheduleText.toString(),
                totalPairs,
                offlinePairs,
                onlinePairs,
                firstTime != null ? firstTime.format(TIME_FORMATTER) : "—",
                lastTime != null ? lastTime.format(TIME_FORMATTER) : "—");

        log.info("========== DEBUG /today ==========");
        log.info("Сегодня: {} (день {}), weekType: {}",
                LocalDate.now(), dayNumber, currentWeekType);
        log.info("Всего пар в БД для дня {}: {}", dayNumber, allScheduleList.size());
        for (Schedule s : allScheduleList) {
            log.info("Пара в БД: {} (week_type: {})",
                    s.getSubject(), s.getWeekType() != null ? s.getWeekType() : "all");
        }
        log.info("После фильтрации (weekType={}): {} пар",
                currentWeekType, filteredScheduleList.size());
        log.info("========== END DEBUG ==========");
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
                """);
        }

        try {
            int dayNumber = Integer.parseInt(ctx.arg(1));
            if (dayNumber < 1 || dayNumber > 7) {
                return reply(ctx, "❌ *Некорректный номер дня*\n\nВведите число от 1 до 7");
            }

            DayOfWeek dayOfWeek = DayOfWeek.of(dayNumber);
            String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);
            String dayNameCapitalized = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            // Используем ТЕКУЩИЙ тип недели
            String currentWeekType = weekTypeService.getCurrentWeekType();

            // ✅ ПРАВИЛЬНЫЕ ЭМОДЗИ И НАЗВАНИЯ
            String weekTypeDisplay;
            String weekTypeEmoji;

            if ("odd".equals(currentWeekType)) {
                weekTypeDisplay = "ЧИСЛИТЕЛЬ";
                weekTypeEmoji = "1️⃣";
            } else if ("even".equals(currentWeekType)) {
                weekTypeDisplay = "ЗНАМЕНАТЕЛЬ";
                weekTypeEmoji = "2️⃣";
            } else {
                weekTypeDisplay = "ВСЕ";
                weekTypeEmoji = "🔄";
            }

            // Получаем все расписания для этого дня
            List<Schedule> allScheduleList = scheduleService.findEntitiesByDay(dayNumber);

            // Фильтруем: показываем пары для текущего типа недели + пары с week_type = 'all'
            List<Schedule> filteredScheduleList = allScheduleList.stream()
                    .filter(s -> {
                        String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
                        return scheduleWeekType.equals(currentWeekType) || scheduleWeekType.equals("all");
                    })
                    .sorted(Comparator.comparing(Schedule::getTimeStart))
                    .toList();

            // Отладочная информация
            log.debug("День {}: тип недели {} ({}), всего пар {}",
                    dayNumber, currentWeekType, weekTypeDisplay, filteredScheduleList.size());

            if (filteredScheduleList.isEmpty()) {
                return reply(ctx, String.format("""
                    📭 *В %s занятий нет* 📭
                    🗓️ *Тип недели:* %s %s
                    
                    🎉 *Это день для:*
                    • Самостоятельной подготовки
                    • Отдыха и восстановления
                    • Работы над проектами
                    
                    💡 *Проверьте другую неделю:*
                    /week %s
                    """,
                        dayNameCapitalized,
                        weekTypeEmoji, weekTypeDisplay,
                        currentWeekType.equals("odd") ? "even" : "odd"));
            }

            // Форматируем расписание с правильными эмодзи
            StringBuilder scheduleText = new StringBuilder();

            for (int i = 0; i < filteredScheduleList.size(); i++) {
                Schedule s = filteredScheduleList.get(i);
                String timeRange = String.format("%s-%s",
                        s.getTimeStart().format(TIME_FORMATTER),
                        s.getTimeEnd().format(TIME_FORMATTER));

                String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";

                // ✅ Эмодзи для каждой пары
                String pairWeekTypeEmoji;
                String pairWeekTypeText;

                if ("odd".equals(scheduleWeekType)) {
                    pairWeekTypeEmoji = "1️⃣";
                    pairWeekTypeText = "числитель";
                } else if ("even".equals(scheduleWeekType)) {
                    pairWeekTypeEmoji = "2️⃣";
                    pairWeekTypeText = "знаменатель";
                } else {
                    pairWeekTypeEmoji = "🔄";
                    pairWeekTypeText = "обе недели";
                }

                String onlineEmoji = (s.getIsOnline() != null && s.getIsOnline()) ? "💻" : "🏫";

                scheduleText.append(String.format("%d. %s %s\n", i + 1, pairWeekTypeEmoji, onlineEmoji))
                        .append(String.format("   ⏰ *%s*\n", timeRange))
                        .append(String.format("   📖 %s (%s)\n", s.getSubject(), pairWeekTypeText));

                if (s.getTeacher() != null && !s.getTeacher().isBlank()) {
                    scheduleText.append(String.format("   👨‍🏫 %s\n", s.getTeacher()));
                }

                if (s.getLocation() != null && !s.getLocation().isBlank()) {
                    String location = (s.getIsOnline() != null && s.getIsOnline()) ?
                            "Онлайн" : s.getLocation();
                    scheduleText.append(String.format("   📍 %s\n", location));
                }

                scheduleText.append("\n");
            }

            int totalPairs = filteredScheduleList.size();
            int onlinePairs = (int) filteredScheduleList.stream()
                    .filter(s -> s.getIsOnline() != null && s.getIsOnline())
                    .count();
            int offlinePairs = totalPairs - onlinePairs;

            String response = String.format("""
                📅 *РАСПИСАНИЕ: %s* 📅
                🗓️ *Текущая неделя:* %s %s
                
                %s
                
                📊 *Статистика дня:*
                📝 Всего пар: %d
                🏫 Очных: %d
                💻 Онлайн: %d
                
                💡 *Другие команды:*
                /today – сегодняшнее расписание
                /week %s – другая неделя
                /help – все команды
                """,
                    dayNameCapitalized.toUpperCase(),
                    weekTypeEmoji, weekTypeDisplay,
                    scheduleText.toString(),
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
                """);
        }
    }

    private SendMessage week(CommandContext ctx) {
        // Если нет аргументов - показываем текущую неделю
        if (ctx.getArgs().length < 2) {
            // Используем WeekTypeService
            String currentWeekType = weekTypeService.getCurrentWeekType();
            String weekTypeDisplay = weekTypeService.getWeekTypeDisplayName(currentWeekType);
            String weekTypeEmoji = weekTypeService.getWeekTypeEmoji(currentWeekType);

            // Получаем все расписание
            List<Schedule> allSchedules = scheduleService.findAllEntities();

            // Фильтруем через WeekTypeService
            List<Schedule> filteredSchedules = allSchedules.stream()
                    .filter(s -> {
                        String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";
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
                    return scheduleWeekType.equals(weekType) || scheduleWeekType.equals("all");
                })
                .sorted(Comparator.comparing(Schedule::getDayOfWeek)
                        .thenComparing(Schedule::getTimeStart))
                .toList();

        return formatWeekSchedule(ctx, filteredSchedules, weekType);
    }

    private SendMessage formatWeekSchedule(CommandContext ctx, List<Schedule> schedules, String weekType) {
        if (schedules.isEmpty()) {
            String weekTypeName = weekTypeService.getWeekTypeDisplayName(weekType);
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

        // ✅ ПРАВИЛЬНЫЕ ЭМОДЗИ ДЛЯ ТИПА НЕДЕЛИ
        String weekTypeEmoji;
        String weekTypeName;

        if ("odd".equals(weekType)) {
            weekTypeEmoji = "1️⃣";
            weekTypeName = "ЧИСЛИТЕЛЬ";
        } else if ("even".equals(weekType)) {
            weekTypeEmoji = "2️⃣";
            weekTypeName = "ЗНАМЕНАТЕЛЬ";
        } else {
            weekTypeEmoji = "🔄";
            weekTypeName = "ВСЕ";
        }

        response.append(String.format("%s *НЕДЕЛЯ %s* %s\n\n",
                weekTypeEmoji,
                weekTypeName,
                weekTypeEmoji));

        // Русские названия дней недели
        String[] dayNames = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};

        // Сортируем дни недели
        List<Integer> sortedDays = scheduleByDay.keySet().stream()
                .sorted()
                .toList();

        for (Integer day : sortedDays) {
            List<Schedule> daySchedules = scheduleByDay.get(day);
            if (daySchedules != null && !daySchedules.isEmpty()) {
                String dayName = dayNames[day - 1];
                response.append(String.format("📅 *%s*\n", dayName));

                // Сортируем пары по времени
                daySchedules.sort(Comparator.comparing(Schedule::getTimeStart));

                for (Schedule s : daySchedules) {
                    String timeRange = String.format("%s-%s",
                            s.getTimeStart().format(TIME_FORMATTER),
                            s.getTimeEnd().format(TIME_FORMATTER));

                    String scheduleWeekType = s.getWeekType() != null ? s.getWeekType() : "all";

                    // ✅ ЭМОДЗИ ДЛЯ КАЖДОЙ ПАРЫ
                    String typeEmoji;
                    if ("odd".equals(scheduleWeekType)) {
                        typeEmoji = "1️⃣";
                    } else if ("even".equals(scheduleWeekType)) {
                        typeEmoji = "2️⃣";
                    } else {
                        typeEmoji = "🔄";
                    }

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

        String groupName = ctx.arg(1).toLowerCase();

        // Если это упоминание "all", проверяем права администратора
        if (groupName.equals("all")) {
            if (!isAdmin(ctx)) {
                String username = ctx.getUser().getUserName();
                return reply(ctx, String.format("""
                ⚠️ *Доступ запрещён*
                
                ❌ *Упоминание всех участников могут использовать только администраторы бота*
                
                👑 *Текущий пользователь:* @%s
                *Статус:* ❌ НЕ АДМИНИСТРАТОР
                
                💡 *Обратитесь к администратору бота для упоминания участников*
                """,
                        username != null ? username : "unknown"));
            }
        }

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

    private SendMessage reminders(CommandContext ctx) {
        // 1. Сначала проверяем права администратора
        if (!isAdmin(ctx)) {
            String username = ctx.getUser().getUserName();
            log.warn("Попытка использования /reminders не-админом: @{}", username);

            return reply(ctx, String.format("""
                ⚠️ *Доступ запрещён*
                
                ❌ *Команда `/reminders` доступна только администраторам бота*
                
                👑 *Текущий пользователь:* @%s
                *Статус:* ❌ НЕ АДМИНИСТРАТОР
                
                💡 *Обратитесь к администратору бота для настройки напоминаний*
                
                ✅ *Вы можете использовать:*
                • `/today` – расписание на сегодня
                • `/day [1-7]` – расписание по дням
                • `/deadlines` – дедлайны заданий
                • `/help` – все команды
                """,
                    username != null ? username : "unknown"));
        }

        // 2. Проверяем, что это группа (если нужно)
        if (!ctx.getUpdate().getMessage().isGroupMessage() &&
                !ctx.getUpdate().getMessage().isSuperGroupMessage()) {
            return reply(ctx, """
                ❌ *Эта команда доступна только в группах*
                
                💡 *Для личного использования бота пишите команды в личные сообщения*
                """);
        }

        // 3. Продолжаем обычную логику (только для администраторов)
        Long chatId = ctx.getChatId();

        if (ctx.getArgs().length < 2) {
            return showRemindersStatus(ctx, chatId);
        }

        String subCommand = ctx.arg(1).toLowerCase();

        return switch (subCommand) {
            case "schedule" -> handleScheduleReminders(ctx, chatId);
            case "deadlines" -> handleDeadlineReminders(ctx, chatId);
            case "before" -> handleBeforeClassReminders(ctx, chatId);
            case "list" -> listReminders(ctx, chatId);
            default -> showRemindersHelp(ctx);
        };
    }

    private SendMessage showRemindersStatus(CommandContext ctx, Long chatId) {
        Optional<BotChat> chatOpt = botChatService.findByChatId(chatId);

        if (chatOpt.isEmpty()) {
            return reply(ctx, """
            ❌ *Чат не зарегистрирован*
            
            💡 *Бот должен быть добавлен в группу как администратор*
            """);
        }

        BotChat chat = chatOpt.get();
        Map<String, Object> settings = chat.getSettings();

        // Настройки из YML
        int minutesBefore = settingsConfig.getReminders().getBeforeClass().getMinutes();
        boolean beforeClassEnabled = settingsConfig.getReminders().getBeforeClass().getEnabled();

        // Проверяем, включены ли напоминания перед парой для этого чата
        boolean chatBeforeClassEnabled = (boolean) settings.getOrDefault("before_class_enabled", beforeClassEnabled);

        // Настройки из YML
        String scheduleTime = settingsConfig.getReminders().getSchedule().getTime();
        String deadlineTime = settingsConfig.getReminders().getDeadlines().getTime();
        String deadlineDays = formatDaysOfWeek(settingsConfig.getReminders().getDeadlines().getDays());

        // Настройки из БД чата
        boolean scheduleEnabled = (boolean) settings.getOrDefault("schedule_notifications", true);
        boolean deadlineEnabled = (boolean) settings.getOrDefault("deadline_notifications", true);

        StringBuilder response = new StringBuilder();
        response.append("🔔 *ТЕКУЩИЕ НАСТРОЙКИ НАПОМИНАНИЙ*\n\n");

        response.append(String.format("""
        📅 *Расписание:*
        • Ежедневно в %s – %s
        • За %d мин. до пары – %s
        
        ⏰ *Дедлайны:*
        • %s в %s – %s
        """,
                scheduleTime,
                scheduleEnabled ? "✅ ВКЛ" : "❌ ВЫКЛ",
                minutesBefore,
                chatBeforeClassEnabled ? "✅ ВКЛ" : "❌ ВЫКЛ",
                deadlineDays,
                deadlineTime,
                deadlineEnabled ? "✅ ВКЛ" : "❌ ВЫКЛ"));

        response.append("""
        
        🔧 *Команды управления:*
        • /reminders schedule on/off – уведомления о расписании
        • /reminders deadlines on/off – уведомления о дедлайнах
        
        ⚠️ *Только для администраторов группы*
        """);

        return reply(ctx, response.toString());
    }

    private SendMessage handleScheduleReminders(CommandContext ctx, Long chatId) {
        if (ctx.getArgs().length < 3) {
            return reply(ctx, """
            ❌ *Недостаточно аргументов*
            
            💡 *Использование:*
            • `/reminders schedule on` – включить
            • `/reminders schedule off` – выключить
            """);
        }

        String action = ctx.arg(2).toLowerCase();
        boolean enable = action.equals("on");

        botChatService.toggleScheduleNotifications(chatId, enable);

        // Получаем время из конфига
        String scheduleTime = settingsConfig.getReminders().getSchedule().getTime();

        return reply(ctx, String.format("""
        %s *Уведомления о расписании %s*
        
        📅 *Бот будет:*
        • Ежедневно в %s отправлять расписание
        • За N минут до каждой пары напоминать о начале
        
        💡 *Используйте `/reminders before [минуты]` для изменения времени*
        """,
                enable ? "✅" : "⏸️",
                enable ? "ВКЛЮЧЕНЫ" : "ВЫКЛЮЧЕНЫ",
                scheduleTime));
    }

    private SendMessage handleDeadlineReminders(CommandContext ctx, Long chatId) {
        if (ctx.getArgs().length < 3) {
            return reply(ctx, """
            ❌ *Недостаточно аргументов*
            
            💡 *Использование:*
            • `/reminders deadlines on` – включить
            • `/reminders deadlines off` – выключить
            """);
        }

        String action = ctx.arg(2).toLowerCase();
        boolean enable = action.equals("on");

        botChatService.toggleDeadlineNotifications(chatId, enable);

        // Получаем настройки из конфига
        String deadlineTime = settingsConfig.getReminders().getDeadlines().getTime();
        String deadlineDays = formatDaysOfWeek(settingsConfig.getReminders().getDeadlines().getDays());

        return reply(ctx, String.format("""
        %s *Уведомления о дедлайнах %s*
        
        ⏰ *Бот будет:*
        • По %s в %s отправлять дедлайны на неделю
        • Показывать актуальные задания и сроки
        
        💡 *Дедлайны берутся из общей базы данных*
        """,
                enable ? "✅" : "⏸️",
                enable ? "ВКЛЮЧЕНЫ" : "ВЫКЛЮЧЕНЫ",
                deadlineDays,
                deadlineTime));
    }

    private SendMessage setTopic(CommandContext ctx) {
        // Проверяем права администратора
        if (!isAdmin(ctx)) {
            return reply(ctx, "❌ *Только администраторы могут настраивать тему бота*");
        }

        // Получаем ID темы из сообщения
        Integer topicId = ctx.getUpdate().getMessage().getMessageThreadId();

        if (topicId == null) {
            return reply(ctx, """
            ❌ *Сообщение не в теме!*
            
            💡 *Как установить тему для бота:*
            1. Перейдите в тему, куда должен писать бот
            2. Отправьте команду `/settopic` ИМЕННО В ЭТОЙ ТЕМЕ
            3. Бот запомнит эту тему и будет писать только туда
            
            ⚠️ *Все автоматические уведомления будут приходить в эту тему*
            """);
        }

        // Получаем название темы
        String topicName = "Тема бота";
        if (ctx.getUpdate().getMessage().getForumTopicCreated() != null) {
            topicName = ctx.getUpdate().getMessage().getForumTopicCreated().getName();
        }

        // Сохраняем ID темы
        botChatService.setBotTopicId(ctx.getChatId(), topicId, topicName);

        // Проверяем, что тема сохранилась
        Optional<Integer> savedTopicId = botChatService.getBotTopicId(ctx.getChatId());

        String response;
        if (savedTopicId.isPresent()) {
            response = String.format("""
            ✅ *Тема установлена!*
            
            🤖 Бот теперь будет отправлять все автоматические уведомления в эту тему:
            *%s*
            
            📌 *ID темы:* `%d`
            
            📋 *Что будет приходить в тему:*
            • 📅 Ежедневное расписание (8:00)
            • ⏰ Недельные дедлайны (понедельник 9:00)
            • 🔔 Напоминания о начале пар (за 15 мин)
            • 📢 Важные объявления
            
            ⚠️ *Важно:* 
            • Бот по-прежнему будет отвечать на команды там, где их отправляют
            • Все автоматические уведомления будут приходить только в эту тему
            
            🔧 *Для проверки используйте команду:* `/testtopic`
            """, topicName, topicId);
        } else {
            response = """
            ⚠️ *Ошибка сохранения темы!*
            
            Тема не сохранилась в базу данных.
            Попробуйте еще раз или обратитесь к разработчику.
            """;
        }

        // Отправляем ответ В ТОЙ ЖЕ теме
        return SendMessage.builder()
                .chatId(ctx.getChatId())
                .messageThreadId(topicId)
                .text(response)
                .parseMode(ParseMode.MARKDOWN)
                .build();
    }

    private SendMessage handleBeforeClassReminders(CommandContext ctx, Long chatId) {
        if (ctx.getArgs().length < 3) {
            int currentMinutes = settingsConfig.getReminders().getBeforeClass().getMinutes();
            boolean enabled = settingsConfig.getReminders().getBeforeClass().getEnabled();

            return reply(ctx, String.format("""
            ⚙️ *НАСТРОЙКА НАПОМИНАНИЙ ПЕРЕД ПАРОЙ*
            
            📋 *Текущие настройки (из YML):*
            • Включено: %s
            • Минут до пары: %d
            
            💡 *Настройки минут изменяются в YML файле:*
            telegram.reminders.before-class.minutes
            
            🔧 *Доступные команды:*
            • /reminders before on – включить напоминания
            • /reminders before off – выключить напоминания
            • /reminders before info – эта информация
            
            ⚠️ *Для изменения минут обратитесь к администратору сервера*
            """,
                    enabled ? "✅" : "❌",
                    currentMinutes));
        }

        String subCommand = ctx.arg(2).toLowerCase();

        if ("on".equals(subCommand) || "off".equals(subCommand)) {
            boolean enable = "on".equals(subCommand);
            botChatService.toggleBeforeClassEnabled(chatId, enable);

            int minutes = settingsConfig.getReminders().getBeforeClass().getMinutes();

            return reply(ctx, String.format("""
            %s *Напоминания перед парой %s*
            
            ⚙️ *Настройки из YML:*
            • Минут до пары: %d
            • Состояние: %s
            
            💡 *Бот будет напоминать за %d минут до начала пары*
            """,
                    enable ? "✅" : "⏸️",
                    enable ? "ВКЛЮЧЕНЫ" : "ВЫКЛЮЧЕНЫ",
                    minutes,
                    enable ? "ВКЛЮЧЕНО" : "ВЫКЛЮЧЕНО",
                    minutes));
        }
        else if ("info".equals(subCommand)) {
            int minutes = settingsConfig.getReminders().getBeforeClass().getMinutes();
            boolean enabled = settingsConfig.getReminders().getBeforeClass().getEnabled();

            return reply(ctx, String.format("""
            ℹ️ *ИНФОРМАЦИЯ О НАПОМИНАНИЯХ ПЕРЕД ПАРОЙ*
            
            📋 *Текущие настройки (YML):*
            • Минут до пары: %d
            • По умолчанию включено: %s
            
            ⚙️ *Как изменить:*
            1. Откройте файл application.yml
            2. Найдите telegram.reminders.before-class
            3. Измените значение minutes
            4. Перезапустите приложение
            
            ⚠️ *Требуется доступ к серверу*
            """,
                    minutes,
                    enabled ? "✅ ДА" : "❌ НЕТ"));
        }
        else {
            try {
                int requestedMinutes = Integer.parseInt(subCommand);
                int currentMinutes = settingsConfig.getReminders().getBeforeClass().getMinutes();

                return reply(ctx, String.format("""
                ℹ️ *ИНФОРМАЦИЯ О ЗНАЧЕНИИ МИНУТ*
                
                📊 *Текущее значение в YML:* %d минут
                📊 *Запрошенное значение:* %d минут
                
                ⚠️ *Значение минут изменяется ТОЛЬКО в YML файле*
                
                💡 *Для изменения:*
                1. Отредактируйте application.yml
                2. Установите: telegram.reminders.before-class.minutes: %d
                3. Перезапустите бота
                
                🔧 *Текущая команда не меняет значение, только показывает информацию*
                """,
                        currentMinutes,
                        requestedMinutes,
                        requestedMinutes));
            } catch (NumberFormatException e) {
                return reply(ctx, "❌ *Неизвестная команда*\n\nИспользуйте:\n• /reminders before on/off\n• /reminders before info\n• /reminders before [число] - информация о значении");
            }
        }
    }

    private SendMessage listReminders(CommandContext ctx, Long chatId) {
        // Упрощаем - возвращаем сообщение, что список недоступен
        return reply(ctx, """
            📋 *СПИСОК НАПОМИНАНИЙ*
            
            ⚠️ *Функция временно недоступна*
            
            💡 *Используйте команды:*
            • `/reminders schedule on/off` – управление расписанием
            • `/reminders deadlines on/off` – управление дедлайнами
            • `/reminders before [минуты]` – настройка времени
            
            🔧 *Напоминания работают автоматически*
            """);
    }

    private SendMessage settings(CommandContext ctx) {
        // 1. Проверяем права администратора
        if (!isAdmin(ctx)) {
            String username = ctx.getUser().getUserName();
            return reply(ctx, String.format("""
                ⚠️ *Доступ запрещён*
                
                ❌ *Команда `/settings` доступна только администраторам бота*
                
                👑 *Текущий пользователь:* @%s
                *Статус:* ❌ НЕ АДМИНИСТРАТОР
                """,
                    username != null ? username : "unknown"));
        }

        // 2. Проверяем, что это группа
        if (!ctx.getUpdate().getMessage().isGroupMessage() &&
                !ctx.getUpdate().getMessage().isSuperGroupMessage()) {
            return reply(ctx, """
                ❌ *Эта команда доступна только в группах*
                """);
        }

        // 3. Продолжаем обычную логику
        Long chatId = ctx.getChatId();
        Optional<BotChat> chatOpt = botChatService.findByChatId(chatId);

        if (chatOpt.isEmpty()) {
            return reply(ctx, """
                ❌ *Чат не зарегистрирован*
                
                💡 *Бот должен быть добавлен в группу как администратор*
                """);
        }

        BotChat chat = chatOpt.get();
        Map<String, Object> settings = chat.getSettings();

        boolean welcomeEnabled = (boolean) settings.getOrDefault("welcome_message", true);
        boolean mentionsEnabled = (boolean) settings.getOrDefault("mention_all_enabled", true);

        return reply(ctx, String.format("""
            ⚙️ *НАСТРОЙКИ ГРУППЫ «%s»*
            
            👋 *Приветственное сообщение:* %s
            👥 *Упоминание всех:* %s
            
            🔧 *Команды:*
            • /reminders – управление напоминаниями
            • /tag all – упомянуть всех участников
            
            💡 *Напоминания работают автоматически на основе расписания и дедлайнов*
            """,
                chat.getTitle() != null ? chat.getTitle() : "Группа",
                welcomeEnabled ? "✅ ВКЛ" : "❌ ВЫКЛ",
                mentionsEnabled ? "✅ ВКЛ" : "❌ ВЫКЛ"));
    }

    // Вспомогательный метод для показа справки по reminders
    private SendMessage showRemindersHelp(CommandContext ctx) {
        // Получаем настройки из конфигурации
        String scheduleTime = settingsConfig.getReminders().getSchedule().getTime();
        String deadlineTime = settingsConfig.getReminders().getDeadlines().getTime();
        int beforeClassMinutes = settingsConfig.getReminders().getBeforeClass().getMinutes();

        // Получаем дни недели в читаемом формате
        String deadlineDays = formatDaysOfWeek(settingsConfig.getReminders().getDeadlines().getDays());

        return reply(ctx, String.format("""
        🔔 *СПРАВКА ПО КОМАНДАМ НАПОМИНАНИЙ*
        
        📋 *Основные команды:*
        • `/reminders` – текущие настройки
        • `/reminders schedule on/off` – уведомления о расписании
        • `/reminders deadlines on/off` – уведомления о дедлайнах
        • `/reminders before [минуты]` – напоминание перед парой
        • `/reminders list` – список всех напоминаний
        
        ⏰ *Что делает бот:*
        • *Ежедневно в %s* – отправляет расписание на день
        • *За %d минут до пары* – напоминает о начале занятия
        • *По %s в %s* – отправляет дедлайны на неделю
        
        ⚠️ *Только для администраторов группы*
        💡 *Все данные берутся из учебной базы*
        """,
                scheduleTime,
                beforeClassMinutes,
                deadlineDays,
                deadlineTime));
    }

    /**
     * Проверяет, является ли пользователь администратором
     */
    private boolean isAdmin(CommandContext ctx) {
        try {
            var user = ctx.getUser();
            if (user == null) {
                return false;
            }

            String username = user.getUserName();
            // Используем новую конфигурацию через settingsConfig
            return settingsConfig.getAdmins().isAdminByUsername(username);

        } catch (Exception e) {
            log.error("Ошибка проверки администратора: {}", e.getMessage());
            return false;
        }
    }

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ФОРМАТИРОВАНИЯ ======

    private String formatScheduleList(List<Schedule> scheduleList, String dayName, String context, String targetWeekType) {
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

            // ✅ ПРАВИЛЬНЫЕ ЭМОДЗИ ДЛЯ ТИПОВ НЕДЕЛЬ
            String weekTypeEmoji;
            if ("odd".equals(scheduleWeekType)) {
                weekTypeEmoji = "1️⃣"; // числитель
            } else if ("even".equals(scheduleWeekType)) {
                weekTypeEmoji = "2️⃣"; // знаменатель
            } else {
                weekTypeEmoji = "🔄"; // all - другой
            }

            Boolean isOnline = s.getIsOnline();
            String onlineEmoji = (isOnline != null && isOnline) ? "💻" : "🏫";
            String locationInfo = (isOnline != null && isOnline) ?
                    "Онлайн" : (s.getLocation() != null ? s.getLocation() : "Ауд. не указана");

            // Текст типа недели
            String weekTypeText = "";
            if ("odd".equals(scheduleWeekType)) {
                weekTypeText = " (числитель)";
            } else if ("even".equals(scheduleWeekType)) {
                weekTypeText = " (знаменатель)";
            } else {
                weekTypeText = " (обе недели)";
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

    // Вспомогательный метод для форматирования дней недели
    private String formatDaysOfWeek(String days) {
        if (days == null || days.length() != 7) return "Все дни";

        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 7; i++) {
            if (days.charAt(i) == '1') {
                result.append(dayNames[i]).append(", ");
            }
        }

        if (result.length() > 0) {
            result.setLength(result.length() - 2); // Убираем последнюю запятую
        } else {
            result.append("Никогда");
        }

        return result.toString();
    }

    /**
     * Определяет текущий тип недели (четная/нечетная)
     * на основе установленной даты отсчета
     *
     * @return "odd" - нечетная, "even" - четная
     */
    public String getCurrentWeekType() {
        return weekTypeService.getCurrentWeekType();
    }

    private SendMessage buildReply(CommandContext ctx, String text) {
        return buildReply(ctx, text, getKeyboardForChat(ctx));
    }

    private SendMessage buildReply(CommandContext ctx, String text, ReplyKeyboard markup) {
        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(ctx.getChatId())
                .text(text)
                .parseMode(ParseMode.MARKDOWN);

        // ВАЖНО: Устанавливаем тот же messageThreadId, откуда пришла команда
        if (ctx.getMessageThreadId() != null) {
            builder.messageThreadId(ctx.getMessageThreadId());
            log.debug("Отправляем ответ в тему ID: {}", ctx.getMessageThreadId());
        }

        // Добавляем клавиатуру только если она есть
        if (markup != null) {
            builder.replyMarkup(markup);
        }

        return builder.build();
    }

    // Метод для определения клавиатуры в зависимости от типа чата
    private ReplyKeyboard getKeyboardForChat(CommandContext ctx) {
        Chat chat = ctx.getUpdate().getMessage().getChat();

        if (isGroupChat(chat)) {
            // В чатах показываем минимальную клавиатуру или убираем её совсем
            return keyboardFactory.minimalKeyboard(); // или null
        } else {
            // В личных сообщениях - полная клавиатура
            return keyboardFactory.defaultKeyboard();
        }
    }

    // Метод для определения типа чата (дублируем из CommandRouter или выносим в утилиты)
    private boolean isGroupChat(Chat chat) {
        if (chat == null) return false;
        String type = chat.getType();
        return "group".equals(type) || "supergroup".equals(type) ||
                "GROUP".equals(type) || "SUPERGROUP".equals(type);
    }
}