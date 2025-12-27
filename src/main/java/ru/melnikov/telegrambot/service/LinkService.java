package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.model.Link;
import ru.melnikov.telegrambot.repository.LinkRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;

    public List<Link> findAll() {
        return linkRepository.findAll();
    }

    public Link save(Link link) {
        return linkRepository.save(link);
    }

    public List<Link> findByUser(Long userId) {
        return linkRepository.findByCreatedBy_Id(userId);
    }
}