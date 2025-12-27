package ru.melnikov.telegrambot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ScheduleDto {
    private Long id;

    @NotNull
    @Min(1)
    @Max(7)
    private Integer dayOfWeek;

    @NotNull
    private LocalTime timeStart;

    @NotNull
    private LocalTime timeEnd;

    @NotBlank
    private String subject;

    private String teacher;

    private String location;

    private Boolean isOnline;

    @Pattern(regexp = "odd|even|all", message = "Week type must be odd, even or all")
    private String weekType;
}
