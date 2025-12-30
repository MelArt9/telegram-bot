package ru.melnikov.telegrambot.util;

import ru.melnikov.telegrambot.model.Deadline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DeadlineFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static String formatDeadlines(List<Deadline> deadlines) {
        if (deadlines == null || deadlines.isEmpty()) {
            return "📭 *Дедлайнов нет*\n\nВсе задания выполнены вовремя! 🎉";
        }

        StringBuilder sb = new StringBuilder();
        LocalDate today = LocalDate.now();

        sb.append("⏰ *АКТУАЛЬНЫЕ ДЕДЛАЙНЫ*\n\n");

        for (int i = 0; i < deadlines.size(); i++) {
            Deadline d = deadlines.get(i);
            long daysLeft = ChronoUnit.DAYS.between(today, d.getDeadlineAt().toLocalDate());

            String urgency = getUrgencyEmoji(daysLeft);
            String daysText = getDaysText(daysLeft);

            sb.append(urgency).append(" *").append(d.getTitle()).append("*\n");
            sb.append("   📅 ").append(d.getDeadlineAt().format(DATETIME_FORMATTER)).append("\n");

            if (d.getDescription() != null && !d.getDescription().trim().isEmpty()) {
                sb.append("   📝 ").append(d.getDescription()).append("\n");
            }

            sb.append("   ⏳ ").append(daysText).append("\n");

            if (i < deadlines.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static String getUrgencyEmoji(long daysLeft) {
        if (daysLeft < 0) return "🔴 (ПРОСРОЧЕНО)";
        if (daysLeft == 0) return "🔴 (СЕГОДНЯ)";
        if (daysLeft <= 2) return "🔴";
        if (daysLeft <= 7) return "🟡";
        return "🟢";
    }

    private static String getDaysText(long daysLeft) {
        if (daysLeft < 0) return "Просрочено на " + Math.abs(daysLeft) + " д.";
        if (daysLeft == 0) return "Сдать сегодня!";
        if (daysLeft == 1) return "Остался 1 день";
        return "Осталось " + daysLeft + " д.";
    }
}