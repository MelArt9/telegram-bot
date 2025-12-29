package ru.melnikov.telegrambot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.exception.NotFoundException;
import ru.melnikov.telegrambot.model.Group;
import ru.melnikov.telegrambot.model.User;
import ru.melnikov.telegrambot.repository.GroupRepository;
import ru.melnikov.telegrambot.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    public Optional<Group> findByName(String name) {
        return groupRepository.findByName(name);
    }

    public Group save(Group incoming) {
        if (incoming.getId() == null) {
            // новая группа
            return groupRepository.save(incoming);
        }

        // существующая группа — обновляем ТОЛЬКО поля
        Group existing = groupRepository.findById(incoming.getId())
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());

        // ⚠️ users НЕ трогаем!
        return groupRepository.save(existing);
    }

    public Group findById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
    }

    public Group findByIdWithUsers(Long id) {
        return groupRepository.findWithUsersById(id)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
    }

    public Group update(Long id, Group updatedGroup) {
        Group existing = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        existing.setName(updatedGroup.getName());
        existing.setDescription(updatedGroup.getDescription());

        return groupRepository.save(existing);
    }

    public void delete(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new NotFoundException("Группа с id=" + id + " не найдена");
        }
        groupRepository.deleteById(id);
    }

    public void addUserToGroup(Long groupId, Long userId) {
        Group group = findByIdWithUsers(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        group.getUsers().add(user);
        groupRepository.save(group);
    }

    public void removeUserFromGroup(Long groupId, Long userId) {
        Group group = findByIdWithUsers(groupId);
        group.getUsers().removeIf(u -> u.getId().equals(userId));
        groupRepository.save(group);
    }
}