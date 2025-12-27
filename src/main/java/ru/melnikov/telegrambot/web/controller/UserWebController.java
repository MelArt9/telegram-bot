package ru.melnikov.telegrambot.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.UserDto;
import ru.melnikov.telegrambot.model.User;
import ru.melnikov.telegrambot.web.service.UserWebService;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserWebController {

    private final UserWebService userWebService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userWebService.getAllUsers());
        return "users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("user", new User());
        return "users/edit";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("user", userWebService.getById(id));
        return "users/edit";
    }

    @PostMapping
    public String save(@ModelAttribute User userDto) {
        userWebService.save(userDto);
        return "redirect:/users";
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userWebService.delete(id);
        return ResponseEntity.noContent().build();
    }
}