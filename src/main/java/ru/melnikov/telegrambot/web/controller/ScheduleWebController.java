package ru.melnikov.telegrambot.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.ScheduleDto;
import ru.melnikov.telegrambot.web.service.ScheduleWebService;

@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ScheduleWebController {

    private final ScheduleWebService scheduleWebService;

    @GetMapping
    public String list(Model model) {
        // Теперь getAll() возвращает List<Schedule>
        model.addAttribute("schedule", scheduleWebService.getAll());
        return "schedule/list";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        // Теперь getById() возвращает Schedule
        model.addAttribute("schedule", scheduleWebService.getById(id));
        return "schedule/edit";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("schedule", new ScheduleDto());
        return "schedule/edit";
    }

    @PostMapping
    public String save(@ModelAttribute ScheduleDto dto) {
        scheduleWebService.save(dto);
        return "redirect:/schedule";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        scheduleWebService.delete(id);
        return "redirect:/schedule";
    }
}