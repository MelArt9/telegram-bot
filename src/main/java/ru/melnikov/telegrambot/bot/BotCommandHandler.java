package ru.melnikov.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.dto.ScheduleDto;
import ru.melnikov.telegrambot.model.User;
import ru.melnikov.telegrambot.service.DeadlineService;
import ru.melnikov.telegrambot.service.GroupService;
import ru.melnikov.telegrambot.service.LinkService;
import ru.melnikov.telegrambot.service.ScheduleService;
import ru.melnikov.telegrambot.service.UserService;
import ru.melnikov.telegrambot.util.DateUtils;
import ru.melnikov.telegrambot.util.TelegramUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BotCommandHandler {

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final DeadlineService deadlineService;
    private final LinkService linkService;
    private final GroupService groupService;
    private final KeyboardFactory keyboardFactory;

    public Optional<SendMessage> handle(Update update) {
        if (update.getMessage() == null || update.getMessage().getText() == null) {
            return Optional.empty();
        }

        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText().trim();

        if (text.startsWith("/start")) {
            return Optional.of(registerUser(chatId, update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName(),
                    update.getMessage().getFrom().getFirstName(), update.getMessage().getFrom().getLastName()));
        }

        if (text.equals("/today")) {
            return Optional.of(scheduleForToday(chatId));
        }

        if (text.startsWith("/day")) {
            return Optional.of(scheduleForDay(chatId, text));
        }

        if (text.equals("/deadlines")) {
            return Optional.of(deadlines(chatId));
        }

        if (text.equals("/links")) {
            return Optional.of(links(chatId));
        }

        if (text.startsWith("/tag")) {
            return Optional.of(tagGroup(chatId, text));
        }

        return Optional.of(SendMessage.builder()
                .chatId(chatId)
                .text("Неизвестная команда. Используйте /today, /deadlines, /links или /day <1-7>.")
                .replyMarkup(keyboardFactory.defaultKeyboard())
                .build());
    }

    private SendMessage registerUser(String chatId, Long telegramId, String username, String firstName, String lastName) {
        if (userService.existsByTelegramId(telegramId)) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("Вы уже зарегистрированы.")
                    .replyMarkup(keyboardFactory.defaultKeyboard())
                    .build();
        }
        User user = User.builder()
                .telegramId(telegramId)
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .isActive(true)
                .build();
        userService.save(user);
        return SendMessage.builder()
                .chatId(chatId)
                .text("Добро пожаловать! Используйте клавиатуру для навигации.")
                .replyMarkup(keyboardFactory.defaultKeyboard())
                .build();
    }

    private SendMessage scheduleForToday(String chatId) {
        LocalDate today = LocalDate.now();
        String weekType = DateUtils.weekTypeForDate(today);
        List<ScheduleDto> schedule = scheduleService.findByDayAndWeekType(today.getDayOfWeek().getValue(), weekType);
        if (schedule.isEmpty()) {
            return SendMessage.builder().chatId(chatId).text("Сегодня пар нет.").build();
        }
        String body = TelegramUtils.formatSchedule(today.getDayOfWeek(), schedule);
        return SendMessage.builder()
                .chatId(chatId)
                .text(body)
                .replyMarkup(keyboardFactory.defaultKeyboard())
                .build();
    }

    private SendMessage scheduleForDay(String chatId, String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            return SendMessage.builder().chatId(chatId).text("Укажите номер дня /day <1-7>.").build();
        }
        int day = Integer.parseInt(parts[1]);
        DayOfWeek dayOfWeek = DateUtils.toDayOfWeek(day);
        if (dayOfWeek == null) {
            return SendMessage.builder().chatId(chatId).text("Неверный номер дня.").build();
        }
        List<ScheduleDto> schedule = scheduleService.findByDay(day);
        if (schedule.isEmpty()) {
            return SendMessage.builder().chatId(chatId).text("На выбранный день пар нет.").build();
        }
        String body = TelegramUtils.formatSchedule(dayOfWeek, schedule);
        return SendMessage.builder().chatId(chatId).text(body).build();
    }

    private SendMessage deadlines(String chatId) {
        List<DeadlineDto> deadlines = deadlineService.findUpcoming();
        String body = deadlines.isEmpty()
                ? "Ближайших дедлайнов нет."
                : TelegramUtils.formatDeadlines(deadlines);
        return SendMessage.builder().chatId(chatId).text(body).replyMarkup(keyboardFactory.defaultKeyboard()).build();
    }

    private SendMessage links(String chatId) {
        var links = linkService.findAll();
        String body = links.isEmpty()
                ? "Полезные ссылки отсутствуют."
                : TelegramUtils.formatLinks(links);
        return SendMessage.builder().chatId(chatId).text(body).replyMarkup(keyboardFactory.defaultKeyboard()).build();
    }

    private SendMessage tagGroup(String chatId, String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            return SendMessage.builder().chatId(chatId).text("Укажите группу: /tag <group>").build();
        }
        String groupName = parts[1];
        return groupService.findByName(groupName)
                .map(group -> {
                    String mentions = TelegramUtils.formatMentions(group.getUsers());
                    String result = mentions.isBlank()
                            ? "В группе нет пользователей."
                            : "Участники " + groupName + ": " + mentions;
                    return SendMessage.builder().chatId(chatId).text(result).build();
                })
                .orElseGet(() -> SendMessage.builder().chatId(chatId).text("Группа не найдена: " + groupName).build());
    }
}
