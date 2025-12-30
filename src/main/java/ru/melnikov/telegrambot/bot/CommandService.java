package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.melnikov.telegrambot.bot.context.CommandContext;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.service.*;
import ru.melnikov.telegrambot.util.TelegramUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    public SendMessage handle(CommandType type, CommandContext ctx) {
        return switch (type) {
            case START -> start(ctx);
            case TODAY -> today(ctx);
            case DAY -> day(ctx);
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
        // Используем метод, возвращающий сущности
        List<Schedule> scheduleList = scheduleService.findEntitiesToday();

        if (scheduleList.isEmpty()) {
            return reply(ctx, """
                    📭 *Сегодня занятий нет!* 📭

                    🎉 *Можно отдохнуть или заняться саморазвитием:*
                    • Повторите пройденный материал
                    • Подготовьтесь к будущим занятиям
                    • Отдохните и наберитесь сил

                    💡 *Что дальше?*
                    /day [1-7] – посмотреть другой день
                    /deadlines – проверить дедлайны
                    """);
        }

        // Получаем русское название дня недели
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String dayName = today.getDisplayName(TextStyle.FULL, RUSSIAN_LOCALE);

        String scheduleText = formatScheduleList(scheduleList, dayName, "сегодня");
        int totalPairs = scheduleList.size();
        int onlinePairs = countOnlinePairs(scheduleList);
        int offlinePairs = totalPairs - onlinePairs;

        LocalTime firstTime = getFirstPairTime(scheduleList);
        LocalTime lastTime = getLastPairTime(scheduleList);

        String response = String.format("""
                📋 *РАСПИСАНИЕ НА СЕГОДНЯ* 📋
                *%s*

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

                    💡 *Пример:* `/day 3` – среда
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

            // Используем метод, возвращающий сущности
            List<Schedule> scheduleList = scheduleService.findEntitiesByDay(dayNumber);

            if (scheduleList.isEmpty()) {
                return reply(ctx, String.format("""
                        📭 *В %s занятий нет*

                        🎉 *Это день для:*
                        • Самостоятельной подготовки
                        • Отдыха и восстановления
                        • Работы над проектами

                        💡 *Проверьте другие дни:*
                        /today – сегодня
                        /deadlines – дедлайны
                        """, dayNameCapitalized));
            }

            String scheduleText = formatScheduleList(scheduleList, dayNameCapitalized, "этот день");
            int totalPairs = scheduleList.size();
            int onlinePairs = countOnlinePairs(scheduleList);
            int offlinePairs = totalPairs - onlinePairs;

            String response = String.format("""
                    📅 *РАСПИСАНИЕ: %s* 📅

                    %s

                    📊 *Статистика дня:*
                    📝 Всего пар: %d
                    🏫 Очных: %d
                    💻 Онлайн: %d

                    💡 *Быстрые команды:*
                    /today – сегодняшнее расписание
                    /help – все команды
                    """,
                    dayNameCapitalized.toUpperCase(),
                    scheduleText,
                    totalPairs,
                    offlinePairs,
                    onlinePairs);

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

    private SendMessage deadlines(CommandContext ctx) {
        var deadlines = deadlineService.findUpcoming();

        if (deadlines.isEmpty()) {
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
        deadlines.sort(Comparator.comparing(deadline -> deadline.getDeadlineAt()));

        StringBuilder formatted = new StringBuilder();
        LocalDate today = LocalDate.now();
        int urgentCount = 0;
        int nearCount = 0;
        int futureCount = 0;

        for (var deadline : deadlines) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, deadline.getDeadlineAt().toLocalDate());
            String emoji;

            if (daysLeft < 3) {
                emoji = "🔴";
                urgentCount++;
            } else if (daysLeft < 7) {
                emoji = "🟡";
                nearCount++;
            } else {
                emoji = "🟢";
                futureCount++;
            }

            formatted.append(String.format("%s *%s*%n", emoji, deadline.getTitle()))
                    .append(String.format("   📅 %s%n", deadline.getDeadlineAt().format(DATETIME_FORMATTER)))
                    .append(String.format("   📝 %s%n", deadline.getDescription() != null ? deadline.getDescription() : "Описание отсутствует"))
                    .append(String.format("   ⏳ Осталось: %d д.%n%n", Math.max(0, daysLeft)));
        }

        String response = String.format("""
                ⏰ *АКТУАЛЬНЫЕ ДЕДЛАЙНЫ* ⏰

                %s
                📈 *Статистика:*
                🔴 Срочных (< 3 дней): %d
                🟡 Ближайших (< 7 дней): %d
                🟢 Будущих (> 7 дней): %d

                💡 *Не забывайте:*
                • Начинайте работу заранее
                • Распределяйте нагрузку равномерно
                • Делайте перерывы для эффективности

                🚀 *У вас всё получится!*
                """,
                formatted.toString(),
                urgentCount,
                nearCount,
                futureCount);

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
                                👤 *Группа \"%s\" пуста*

                                📭 *В этой группе пока нет участников*

                                💡 *Что можно сделать:*
                                • Пригласить участников в группу
                                • Проверить другие группы
                                • Обратиться к администратору

                                🔧 *Для админов:* `/admin` – управление группами
                                """, groupName));
                    }

                    StringBuilder users = new StringBuilder();
                    for (var user : group.getUsers()) {
                        String mention = user.getUsername() != null && !user.getUsername().isBlank()
                                ? "@" + user.getUsername()
                                : (user.getFirstName() != null ? user.getFirstName() : "");

                        if (!mention.isEmpty()) {
                            users.append("👤 ").append(mention).append("\n");
                        }
                    }

                    return reply(ctx, String.format("""
                            📢 *УПОМИНАНИЕ ГРУППЫ: %s* 📢

                            %s
                            👥 *Участников всего:* %d

                            💬 *Используйте для:*
                            • Важных объявлений
                            • Напоминаний о дедлайнах
                            • Совместных обсуждений

                            ⚠️ *Пожалуйста, не злоупотребляйте упоминаниями*
                            """,
                            groupName.toUpperCase(),
                            users.toString(),
                            group.getUsers().size()));
                })
                .orElse(reply(ctx, String.format("""
                        ❌ *Группа не найдена* ❌

                        Группа *\"%s\"* не существует или была удалена.

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

                📅 /today – На сегодня
                📆 /day [1-7] – По дням недели

                ┏━━━━━━━━━━━┓
                ┃  ⏰ УЧЕБНЫЙ ПЛАН   ┃
                ┗━━━━━━━━━━━┛

                ⏳ /deadlines – Дедлайны работ
                🔗 /links – Полезные ресурсы

                ┏━━━━━━━━━━━┓
                ┃ 👥 КОММУНИКАЦИЯ  ┃
                ┗━━━━━━━━━━━┛

                📢 /tag [группа] – Упоминание

                ┏━━━━━━━━━━━┓
                ┃        ⚙️  СИСТЕМА          ┃
                ┗━━━━━━━━━━━┛

                🚀 /start – Перезапуск бота
                ❓ /help – Эта справка

                ━━━━━━━━━━━━━━━━━━━━━

                📝 *Примеры использования:*
                • `/day 3` – расписание на среду
                • `/tag all` – упомянуть всех
                • `/deadlines` – посмотреть дедлайны

                💡 *Совет:* Используйте встроенную клавиатурные кнопки для быстрого доступа!

                🎓 *Успешной учёбы!*
                """);
    }

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ФОРМАТИРОВАНИЯ ======

    private String formatScheduleList(List<Schedule> scheduleList, String dayName, String context) {
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

            String weekTypeEmoji = getWeekTypeEmoji(s.getWeekType());
            String onlineEmoji = s.getIsOnline() ? "💻" : "🏫";
            String locationInfo = s.getIsOnline() ? "Онлайн" :
                    (s.getLocation() != null ? s.getLocation() : "Ауд. не указана");

            sb.append(String.format("%d. %s %s\n", i + 1, weekTypeEmoji, onlineEmoji))
                    .append(String.format("   ⏰ *%s*\n", timeRange))
                    .append(String.format("   📖 %s\n", s.getSubject()))
                    .append(String.format("   👨‍🏫 %s\n",
                            s.getTeacher() != null ? s.getTeacher() : "Преподаватель не указан"))
                    .append(String.format("   📍 %s\n", locationInfo))
                    .append("\n");
        }

        return sb.toString();
    }

    private String getWeekTypeEmoji(String weekType) {
        return switch (weekType) {
            case "odd" -> "1️⃣";
            case "even" -> "2️⃣";
            default -> "🔄";
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
                .filter(Schedule::getIsOnline)
                .count();
    }
}