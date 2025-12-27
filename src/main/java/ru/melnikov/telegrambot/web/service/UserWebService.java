package ru.melnikov.telegrambot.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.model.User;
import ru.melnikov.telegrambot.service.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserWebService {

    private final UserService userService;

    public List<User> getAllUsers() {
        return userService.findAll();
    }

    public User getById(Long id) {
        return userService.findById(id);
    }

    public void save(User user) {
        userService.save(user);
    }

    public void delete(Long id) {
        userService.delete(id);
    }
}
