package ru.melnikov.telegrambot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.ScheduleDto;
import ru.melnikov.telegrambot.service.ScheduleService;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/day/{day}")
    public List<ScheduleDto> getByDay(@PathVariable Integer day) {
        return scheduleService.findByDay(day);
    }

    @GetMapping("/day/{day}/week/{type}")
    public List<ScheduleDto> getByDayAndWeek(
            @PathVariable Integer day,
            @PathVariable String type
    ) {
        return scheduleService.findByDayAndWeekType(day, type);
    }

    @PostMapping
    public ScheduleDto create(@Valid @RequestBody ScheduleDto scheduleDto) {
        return scheduleService.save(scheduleDto);
    }
}
