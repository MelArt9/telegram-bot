package ru.melnikov.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule extends BaseEntity {

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "time_start", nullable = false)
    private LocalTime timeStart;

    @Column(name = "time_end", nullable = false)
    private LocalTime timeEnd;

    @Column(nullable = false)
    private String subject;

    private String teacher;

    private String location;

    @Column(name = "is_online")
    private Boolean isOnline;

    @Column(name = "week_type")
    @Builder.Default
    private String weekType = "all"; // Значение по умолчанию
}