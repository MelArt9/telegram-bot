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

    // 🔹 Список групп
    @GetMapping
    public String list(Model model) {
        model.addAttribute("groups", groupWebService.getAllGroups());
        return "groups/list";
    }

    // 🔹 Страница создания группы
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("group", new Group());
        return "groups/edit";
    }

    // 🔹 Редактирование группы
    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Group group = groupWebService.getByIdWithUsers(id);

        model.addAttribute("group", group);
        model.addAttribute("users", group.getUsers());
        model.addAttribute("allUsers", groupWebService.getAllUsers());

        return "groups/edit";
    }

    // 🔹 Сохранение группы
    @PostMapping
    public String save(@ModelAttribute Group group) {
        groupWebService.save(group);
        return "redirect:/groups";
    }

    // 🔹 Удаление группы
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        groupWebService.delete(id);
        return "redirect:/groups";
    }

    // 🔹 Добавление пользователя в группу
    @PostMapping("/{groupId}/users/add")
    public String addUserToGroup(
            @PathVariable Long groupId,
            @RequestParam Long userId
    ) {
        groupWebService.addUserToGroup(groupId, userId);
        return "redirect:/groups/" + groupId;
    }

    // 🔹 Удаление пользователя из группы
    @PostMapping("/{groupId}/users/{userId}/delete")
    public String removeUserFromGroup(
            @PathVariable Long groupId,
            @PathVariable Long userId
    ) {
        groupWebService.removeUserFromGroup(groupId, userId);
        return "redirect:/groups/" + groupId;
    }
}