package ru.melnikov.telegrambot.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.DeadlineDto;
import ru.melnikov.telegrambot.web.service.DeadlineWebService;

@Controller
@RequestMapping("/deadlines")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DeadlineWebController {

    private final DeadlineWebService deadlineWebService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("deadlines", deadlineWebService.findAll());
        return "deadlines/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("deadline", new DeadlineDto());
        return "deadlines/edit";
    }

    @PostMapping
    public String save(@ModelAttribute DeadlineDto dto) {
        deadlineWebService.save(dto);
        return "redirect:/deadlines";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("deadline", deadlineWebService.findById(id));
        return "deadlines/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        deadlineWebService.delete(id);
        return "redirect:/deadlines";
    }
}