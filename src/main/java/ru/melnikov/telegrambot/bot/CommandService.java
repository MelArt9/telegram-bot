package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.melnikov.telegrambot.bot.context.CommandContext;
import ru.melnikov.telegrambot.service.*;
import ru.melnikov.telegrambot.util.DateUtils;
import ru.melnikov.telegrambot.util.TelegramUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CommandService {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final DeadlineService deadlineService;
    private final LinkService linkService;
    private final GroupService groupService;
    private final KeyboardFactory keyboardFactory;
    private final CommandLogService commandLogService;

    public SendMessage handle(CommandType type, CommandContext ctx) {
        commandLogService.log(
                ctx.getUser().getId(),
                ctx.getUser().getUserName(),
                type.name(),
                String.join(" ", ctx.getArgs())
        );

        return switch (type) {
            case START -> start(ctx);
            case TODAY -> today(ctx);
            case DAY -> day(ctx);
            case DEADLINES -> deadlines(ctx);
            case LINKS -> links(ctx);
            case TAG -> tag(ctx);
            default -> unknown(ctx);
        };
    }

    private SendMessage start(CommandContext ctx) {
        userService.registerIfNotExists(
                ctx.getUser().getId(),
                ctx.getUser().getUserName(),
                ctx.getUser().getFirstName(),
                ctx.getUser().getLastName()
        );

        return reply(ctx, "Добро пожаловать! Используйте меню ниже.");
    }

    private SendMessage today(CommandContext ctx) {
        LocalDate today = LocalDate.now();
        var list = scheduleService.findByDayAndWeekType(
                today.getDayOfWeek().getValue(),
                DateUtils.weekTypeForDate(today)
        );

        return list.isEmpty()
                ? reply(ctx, "Сегодня занятий нет.")
                : reply(ctx, TelegramUtils.formatSchedule(today.getDayOfWeek(), list));
    }

    private SendMessage day(CommandContext ctx) {
        try {
            int day = Integer.parseInt(ctx.arg(0));
            var list = scheduleService.findByDay(day);

            return list.isEmpty()
                    ? reply(ctx, "На выбранный день занятий нет.")
                    : reply(ctx, TelegramUtils.formatSchedule(
                    java.time.DayOfWeek.of(day),
                    list
            ));
        } catch (Exception e) {
            return reply(ctx, "Используйте формат: /day 1");
        }
    }

    private SendMessage deadlines(CommandContext ctx) {
        var list = deadlineService.findUpcoming();
        return list.isEmpty()
                ? reply(ctx, "Ближайших дедлайнов нет.")
                : reply(ctx, TelegramUtils.formatDeadlines(list));
    }

    private SendMessage links(CommandContext ctx) {
        return reply(ctx, TelegramUtils.formatLinks(linkService.findAll()));
    }

    private SendMessage tag(CommandContext ctx) {
        if (ctx.getArgs().length < 1) {
            return reply(ctx, "Укажите группу: /tag group");
        }

        return groupService.findByName(ctx.getArgs()[0])
                .map(g -> reply(ctx, TelegramUtils.formatMentions(g.getUsers())))
                .orElse(reply(ctx, "Группа не найдена"));
    }

    private SendMessage unknown(CommandContext ctx) {
        return reply(ctx, "Неизвестная команда. Используйте /help");
    }

    private SendMessage reply(CommandContext ctx, String text) {
        return SendMessage.builder()
                .chatId(ctx.getChatId())
                .text(text)
                .replyMarkup(keyboardFactory.defaultKeyboard())
                .build();
    }
}