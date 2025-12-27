package ru.melnikov.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.melnikov.telegrambot.dto.LinkDto;
import ru.melnikov.telegrambot.exception.NotFoundException;
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
        Link link = linkMapper.toEntity(dto);
        link.setCreatedBy(userService.getByIdOrThrow(dto.getCreatedBy()));
        return linkMapper.toDto(linkRepository.save(link));
    }

    public List<LinkDto> findByUser(Long userId) {
        return linkRepository.findByCreatedBy_Id(userId)
                .stream()
                .map(linkMapper::toDto)
                .toList();
    }

    public Link findById(Long id) {
        return linkRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Ссылка с id=" + id + " не найдена"));
    }
}
