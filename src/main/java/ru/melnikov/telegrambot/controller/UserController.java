package ru.melnikov.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.model.User;
import ru.melnikov.telegrambot.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{telegramId}")
    public User getByTelegramId(@PathVariable Long telegramId) {
        return userService.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userService.save(user);
    }
}