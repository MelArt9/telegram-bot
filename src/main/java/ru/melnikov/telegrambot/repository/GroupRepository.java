package ru.melnikov.telegrambot.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.melnikov.telegrambot.model.Group;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByName(String name);

    @EntityGraph(attributePaths = "users")
    Optional<Group> findWithUsersById(Long id);

    // Новый метод для загрузки группы с пользователями по имени
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.users WHERE g.name = :name")
    Optional<Group> findByNameWithUsers(@Param("name") String name);
}