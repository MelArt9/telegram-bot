package ru.melnikov.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Entity
@Table(name = "reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "reminder_type", nullable = false)
    private String reminderType; // SCHEDULE_TODAY, SCHEDULE_BEFORE, DEADLINE_WEEKLY

    @Column(name = "schedule_time", nullable = false)
    private LocalTime scheduleTime;

    @Column(name = "days_of_week", length = 7)
    private String daysOfWeek;

    @Column(name = "is_active")
    private Boolean isActive;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (daysOfWeek == null) {
            // Устанавливаем дни по умолчанию в зависимости от типа
            daysOfWeek = switch (reminderType) {
                case "DEADLINE_WEEKLY" -> "01011"; // Вт, Чт, Пт
                default -> "1111111"; // Все дни
            };
        }
    }
}