package ru.melnikov.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.model.Schedule;
import ru.melnikov.telegrambot.service.ScheduleService;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/day/{day}")
    public List<Schedule> getByDay(@PathVariable Integer day) {
        return scheduleService.findByDay(day);
    }

    @GetMapping("/day/{day}/week/{type}")
    public List<Schedule> getByDayAndWeek(
            @PathVariable Integer day,
            @PathVariable String type
    ) {
        return scheduleService.findByDayAndWeekType(day, type);
    }

    @PostMapping
    public Schedule create(@RequestBody Schedule schedule) {
        return scheduleService.save(schedule);
    }
}