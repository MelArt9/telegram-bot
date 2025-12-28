package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.LinkDto;
import ru.melnikov.telegrambot.mapper.LinkMapper;
import ru.melnikov.telegrambot.model.Link;
import ru.melnikov.telegrambot.repository.LinkRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final LinkMapper linkMapper;
    private final UserService userService;

    public List<LinkDto> findAll() {
        return linkRepository.findAll()
                .stream()
                .map(linkMapper::toDto)
                .toList();
    }

    public LinkDto save(LinkDto dto) {
        Link entity = linkMapper.toEntity(dto);
        entity.setCreatedBy(userService.getByIdOrThrow(dto.getCreatedBy()));
        return linkMapper.toDto(linkRepository.save(entity));
    }

    public LinkDto create(LinkDto dto) {
        Link entity = linkMapper.toEntity(dto);
        entity.setCreatedBy(userService.getByIdOrThrow(dto.getCreatedBy()));
        return linkMapper.toDto(linkRepository.save(entity));
    }

    public LinkDto update(Long id, LinkDto dto) {
        Link existing = linkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ссылка не найдена"));

        existing.setTitle(dto.getTitle());
        existing.setUrl(dto.getUrl());
        // createdBy НЕ меняем!

        return linkMapper.toDto(linkRepository.save(existing));
    }

    public List<LinkDto> findByUser(Long userId) {
        return linkRepository.findByCreatedBy_Id(userId)
                .stream()
                .map(linkMapper::toDto)
                .toList();
    }

    public LinkDto findById(Long id) {
        return linkRepository.findById(id)
                .map(linkMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Ссылка не найдена"));
    }

    public void delete(Long id) {
        linkRepository.deleteById(id);
    }
}
