package ru.melnikov.telegrambot.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeadlineDto {

    private Long id;
    private String title;
    private LocalDateTime deadlineAt;
    private String description;
    private Long createdBy;
}