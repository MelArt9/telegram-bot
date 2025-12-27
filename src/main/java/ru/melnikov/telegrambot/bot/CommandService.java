package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.jvnet.hk2.annotations.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.melnikov.telegrambot.service.*;
import ru.melnikov.telegrambot.util.DateUtils;
import ru.melnikov.telegrambot.util.TelegramUtils;

import java.time.DayOfWeek;
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

    public SendMessage start(String chatId, User tgUser) {
        userService.registerIfNotExists(
                tgUser.getId(),
                tgUser.getUserName(),
                tgUser.getFirstName(),
                tgUser.getLastName()
        );
        return reply(chatId, "Добро пожаловать! Используйте меню ниже.");
    }

    public SendMessage today(String chatId) {
        var today = LocalDate.now();
        var schedule = scheduleService.findByDayAndWeekType(
                today.getDayOfWeek().getValue(),
                DateUtils.weekTypeForDate(today)
        );

        return schedule.isEmpty()
                ? reply(chatId, "Сегодня занятий нет.")
                : reply(chatId, TelegramUtils.formatSchedule(today.getDayOfWeek(), schedule));
    }

    public SendMessage day(String chatId, String raw) {
        int day;
        try {
            day = Integer.parseInt(raw);
        } catch (Exception e) {
            return reply(chatId, "Неверный формат. Используй /day 1..7");
        }

        var schedule = scheduleService.findByDay(day);
        return schedule.isEmpty()
                ? reply(chatId, "На выбранный день занятий нет.")
                : reply(chatId, TelegramUtils.formatSchedule(DayOfWeek.of(day), schedule));
    }

    public SendMessage deadlines(String chatId) {
        var list = deadlineService.findUpcoming();
        return list.isEmpty()
                ? reply(chatId, "Ближайших дедлайнов нет.")
                : reply(chatId, TelegramUtils.formatDeadlines(list));
    }

    public SendMessage links(String chatId) {
        return reply(chatId, TelegramUtils.formatLinks(linkService.findAll()));
    }

    public SendMessage tag(String chatId, String groupName) {
        return groupService.findByName(groupName)
                .map(g -> reply(chatId, TelegramUtils.formatMentions(g.getUsers())))
                .orElse(reply(chatId, "Группа не найдена"));
    }

    public SendMessage unknown(String chatId) {
        return reply(chatId, "Неизвестная команда. Используйте /help");
    }

    private SendMessage reply(String chatId, String text) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboardFactory.defaultKeyboard())
                .build();
    }
}