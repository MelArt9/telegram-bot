package ru.melnikov.telegrambot.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.web.service.LinkWebService;

@Controller
@RequestMapping("/links")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LinkWebController {

    private final LinkWebService linkWebService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("links", linkWebService.getAll());
        return "links/list";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("link", linkWebService.getById(id));
        return "links/edit";
    }
}