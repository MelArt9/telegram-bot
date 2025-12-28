package ru.melnikov.telegrambot.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.LinkDto;
import ru.melnikov.telegrambot.service.LinkService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkWebService {

    private final LinkService linkService;

    public List<LinkDto> getAll() {
        return linkService.findAll();
    }

    public LinkDto getById(Long id) {
        return linkService.findById(id);
    }

    public void save(LinkDto dto) {
        linkService.save(dto);
    }

    public void delete(Long id) {
        linkService.delete(id);
    }
}