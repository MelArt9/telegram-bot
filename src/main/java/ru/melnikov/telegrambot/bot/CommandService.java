package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.melnikov.telegrambot.bot.context.CommandContext;
import ru.melnikov.telegrambot.service.*;

@Service
@RequiredArgsConstructor
public class CommandService {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final DeadlineService deadlineService;
    private final LinkService linkService;
    private final GroupService groupService;
    private final KeyboardFactory keyboardFactory;

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
        return reply(ctx, "Неизвестная команда. Введите /help");
    }

    private SendMessage reply(CommandContext ctx, String text) {
        return SendMessage.builder()
                .chatId(ctx.getChatId())
                .text(text)
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
        return reply(ctx, "Привет! 👋 Я готов к работе.");
    }

    private SendMessage today(CommandContext ctx) {
        var list = scheduleService.findToday();
        return list.isEmpty()
                ? reply(ctx, "Сегодня занятий нет")
                : reply(ctx, "📅 Сегодня:\n" + list);
    }

    private SendMessage day(CommandContext ctx) {
        try {
            int day = Integer.parseInt(ctx.arg(1));
            return reply(ctx, scheduleService.findByDay(day).toString());
        } catch (Exception e) {
            return reply(ctx, "Используй: /day 1");
        }
    }

    private SendMessage deadlines(CommandContext ctx) {
        return reply(ctx, deadlineService.formatDeadlines());
    }

    private SendMessage links(CommandContext ctx) {
        return reply(ctx, linkService.formatLinks());
    }

    private SendMessage tag(CommandContext ctx) {
        if (ctx.getArgs().length < 2) {
            return reply(ctx, "Использование: /tag <название_группы>");
        }

        return groupService.findByName(ctx.getArgs()[1])
                .map(group -> {
                    if (group.getUsers().isEmpty()) {
                        return reply(ctx, "В группе нет участников");
                    }
                    String users = group.getUsers().stream()
                            .map(u -> "@" + u.getUsername())
                            .reduce("", (a, b) -> a + "\n" + b);
                    return reply(ctx, "Участники группы:\n" + users);
                })
                .orElse(reply(ctx, "Группа не найдена"));
    }

    private SendMessage help(CommandContext ctx) {
        return reply(ctx, """
                📘 Команды:
                /start – старт
                /today – расписание
                /day N – день недели
                /deadlines – дедлайны
                /links – ссылки
                /tag группа – упомянуть группу
                """);
    }
}