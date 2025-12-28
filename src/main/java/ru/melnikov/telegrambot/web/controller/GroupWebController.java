package ru.melnikov.telegrambot.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.model.Group;
import ru.melnikov.telegrambot.web.service.GroupWebService;

@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GroupWebController {

    private final GroupWebService groupWebService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", groupWebService.getAllGroups());
        return "groups/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("group", new Group());
        return "groups/edit";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("group", groupWebService.getById(id));
        return "groups/edit";
    }

    @PostMapping
    public String save(@ModelAttribute Group group) {
        groupWebService.save(group);
        return "redirect:/groups";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        groupWebService.delete(id);
        return "redirect:/groups";
    }
}