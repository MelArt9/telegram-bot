package ru.melnikov.telegrambot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.service.DeadlineService;

@Controller
@RequestMapping("/deadlines")
public class DeadlineController {

    private final DeadlineService deadlineService;

    public DeadlineController(DeadlineService deadlineService) {
        this.deadlineService = deadlineService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("deadlines", deadlineService.findAll());
        model.addAttribute("content", "deadlines/list");
        return "layout";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("deadline", new DeadlineDto());
        model.addAttribute("content", "deadlines/create");
        return "layout";
    }
}
