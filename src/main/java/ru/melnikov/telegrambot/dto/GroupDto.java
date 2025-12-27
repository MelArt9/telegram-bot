package ru.melnikov.telegrambot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupDto {
    private Long id;

    @NotBlank
    private String name;

    private String description;
}
