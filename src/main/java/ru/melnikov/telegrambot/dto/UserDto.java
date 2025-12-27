package ru.melnikov.telegrambot.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    @jakarta.validation.constraints.NotNull
    private Long telegramId;
    private String username;
    private String firstName;
    private String lastName;
    private Boolean isActive = true;
}
