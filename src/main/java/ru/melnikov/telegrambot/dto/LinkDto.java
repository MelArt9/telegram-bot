package ru.melnikov.telegrambot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LinkDto {

    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    private String url;

    @NotNull
    private Long createdBy;
}
