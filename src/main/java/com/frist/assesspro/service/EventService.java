package com.frist.assesspro.service;


import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.entity.Event;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.mapper.EventMapper;
import com.frist.assesspro.repository.EventRepository;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    // Получить события для календаря (за период)
    @Transactional(readOnly = true)
    public List<EventDTO> getEventsForCalendar(LocalDate start, LocalDate end) {
        return eventRepository.findByEventDateBetween(start, end)
                .stream()
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }

    // Создать событие
    @Transactional
    public EventDTO createEvent(EventDTO dto, User creator) {
        Event event = Event.builder()
                .name(dto.getTitle())
                .description(dto.getDescription())
                .eventDate(dto.getStart())
                .createdByEvent(creator)
                .build();
        event = eventRepository.save(event);
        return eventMapper.toDto(event);
    }

    // Обновить событие
    @Transactional
    public EventDTO updateEvent(Long id, EventDTO dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"));
        event.setName(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getStart());
        event = eventRepository.save(event);
        return eventMapper.toDto(event);
    }

    // Удалить событие
    @Transactional
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    // Получить одно событие
    @Transactional(readOnly = true)
    public EventDTO getEvent(Long id) {
        return eventRepository.findById(id)
                .map(eventMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Событие не найдено"));
    }
}
