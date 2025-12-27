package ru.melnikov.telegrambot.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.web.service.ScheduleWebService;

@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ScheduleWebController {

    private final ScheduleWebService scheduleWebService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("schedule", scheduleWebService.getAll());
        return "schedule/list";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("schedule", scheduleWebService.getById(id));
        return "schedule/edit";
    }
}