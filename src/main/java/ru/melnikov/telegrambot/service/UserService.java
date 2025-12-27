package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.exception.NotFoundException;
import ru.melnikov.telegrambot.model.Role;
import ru.melnikov.telegrambot.model.User;
import ru.melnikov.telegrambot.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public boolean existsByTelegramId(Long telegramId) {
        return userRepository.existsByTelegramId(telegramId);
    }

    public User getByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ru.melnikov.telegrambot.exception.NotFoundException("User not found: " + id));
    }

    public void registerIfNotExists(Long tgId, String username, String firstName, String lastName) {
        if (!userRepository.existsByTelegramId(tgId)) {
            userRepository.save(User.builder()
                    .telegramId(tgId)
                    .username(username)
                    .firstName(firstName)
                    .lastName(lastName)
                    .role(Role.STUDENT)
                    .build());
        }
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
