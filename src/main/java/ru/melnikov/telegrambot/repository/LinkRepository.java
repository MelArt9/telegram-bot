package ru.melnikov.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.melnikov.telegrambot.model.Link;

import java.util.List;

public interface LinkRepository extends JpaRepository<Link, Long> {

    List<Link> findByCreatedBy_Id(Long userId);
}