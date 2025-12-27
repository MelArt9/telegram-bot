package ru.melnikov.telegrambot.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.LinkDto;
import ru.melnikov.telegrambot.model.Link;
import ru.melnikov.telegrambot.service.LinkService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkWebService {

    private final LinkService linkService;

    public List<LinkDto> getAll() {
        return linkService.findAll();
    }

    public Link getById(Long id) {
        return linkService.findById(id);
    }
}
