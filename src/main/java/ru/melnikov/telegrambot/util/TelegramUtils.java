package ru.melnikov.telegrambot.util;

import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.dto.LinkDto;
import ru.melnikov.telegrambot.dto.ScheduleDto;
import ru.melnikov.telegrambot.model.User;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class TelegramUtils {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private TelegramUtils() {
    }

    public static String formatSchedule(DayOfWeek dayOfWeek, List<ScheduleDto> schedule) {
        String header = "Расписание на " + dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("ru"));
        String body = schedule.stream()
                .map(item -> String.format("%s-%s | %s (%s) %s [%s]",
                        item.getTimeStart().format(TIME_FORMATTER),
                        item.getTimeEnd().format(TIME_FORMATTER),
                        item.getSubject(),
                        item.getTeacher() == null ? "Преподаватель не указан" : item.getTeacher(),
                        item.getLocation() == null ? "" : "Аудитория: " + item.getLocation(),
                        item.getWeekType() == null ? "all" : item.getWeekType()))
                .collect(Collectors.joining("\n"));
        return header + "\n" + body;
    }

    public static String formatDeadlines(List<DeadlineDto> deadlines) {
        return deadlines.stream()
                .map(deadline -> String.format("• %s — %s", deadline.getTitle(), deadline.getDeadlineAt().format(DATE_TIME_FORMATTER)))
                .collect(Collectors.joining("\n"));
    }

    public static String formatLinks(List<LinkDto> links) {
        return links.stream()
                .map(link -> String.format("• %s: %s", link.getTitle(), link.getUrl()))
                .collect(Collectors.joining("\n"));
    }

    public static String formatMentions(Collection<User> users) {
        if (users == null || users.isEmpty()) {
            return "";
        }
        return users.stream()
                .map(TelegramUtils::formatMention)
                .collect(Collectors.joining(" "));
    }

    private static String formatMention(User user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return "@" + user.getUsername();
        }
        return user.getFirstName() == null ? "" : user.getFirstName();
    }
}
