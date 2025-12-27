package ru.melnikov.telegrambot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.melnikov.telegrambot.dto.UserDto;
import ru.melnikov.telegrambot.exception.NotFoundException;
import ru.melnikov.telegrambot.mapper.UserMapper;
import ru.melnikov.telegrambot.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public List<UserDto> getAll() {
        return userService.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @GetMapping("/{telegramId}")
    public UserDto getByTelegramId(@PathVariable Long telegramId) {
        return userService.findByTelegramId(telegramId)
                .map(userMapper::toDto)
                .orElseThrow(() -> new NotFoundException("User not found by telegramId: " + telegramId));
    }

    @PostMapping
    public UserDto create(@Valid @RequestBody UserDto userDto) {
        return userMapper.toDto(userService.save(userMapper.toEntity(userDto)));
    }
}
