package ru.melnikov.telegrambot.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.LinkDto;
import ru.melnikov.telegrambot.service.LinkService;
import ru.melnikov.telegrambot.web.service.LinkWebService;

@Controller
@RequestMapping("/links")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LinkWebController {

    private final LinkWebService linkWebService;
    private final LinkService linkService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("links", linkService.findAll());
        return "links/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("link", new LinkDto());
        return "links/edit";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("link", linkService.findById(id));
        return "links/edit";
    }

    @PostMapping
    public String save(@ModelAttribute LinkDto link) {
        if (link.getId() == null) {
            linkService.create(link);
        } else {
            linkService.update(link.getId(), link);
        }
        return "redirect:/links";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        linkService.delete(id);
        return "redirect:/links";
    }
}