package ru.melnikov.telegrambot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Future;

@Data
public class DeadlineDto {

    private Long id;

    @NotBlank
    private String title;

    @NotNull
    @Future
    private LocalDateTime deadlineAt;
    private String description;

    private String linkUrl;
    private String linkText;

    private Long createdBy;
}