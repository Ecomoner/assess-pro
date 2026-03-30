package com.frist.assesspro.service;


import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.entity.Event;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.EventRepository;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    /**
     * Создание события.
     */
    @Transactional
    public Event createEvent(EventDTO eventDTO, String creatorUsername) {

        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + creatorUsername));

        log.info("Пользователь {} создает новое событие", creatorUsername);

        if (eventRepository.existsByName(eventDTO.getName())) {
            throw new RuntimeException("Событие с названием '" + eventDTO.getName() + "' уже существует");
        }

        if (eventDTO.getDescription() == null) {
            throw new IllegalArgumentException("Описание событие не может быть пустым");
        }
        Event event = new Event();
        event.setName(eventDTO.getName());
        event.setCreatedByEvent(creator);
        event.setDescription(eventDTO.getDescription());

        Event savedEvent = eventRepository.save(event);

        return savedEvent;
    }

    /**
     * Редактирование события.
     */
    @Transactional
    public Event editEvent(Long id,EventDTO eventDTO){

        Event event = eventRepository.findById(id)
                .orElseThrow(()->new RuntimeException("Событии с таким ID нет"));

        if (eventDTO.getId()!=null && eventDTO.getDescription()!=null){
            event.setDescription(eventDTO.getDescription());
        }

        Event editedEvent = eventRepository.save(event);

        return editedEvent;
    }

    /**
     * Удаление события.
     */
    @Transactional
    public void deleteEvent(Long id){
        Event event = eventRepository.findById(id)
                .orElseThrow(()->new RuntimeException("Событии с таким ID нет"));

        eventRepository.delete(event);
    }

    @Transactional(readOnly=true )
    public Page<EventDTO> getAllEvents(Pageable pageable) {
        return eventRepository.findAllEventDTOs(pageable);
    }

    public List<EventDTO> getLastEvents(int limit) {
        return eventRepository.findTop5ByOrderByIdDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public EventDTO convertToDTO(Event e) {
        EventDTO dto = new EventDTO();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        return dto;
    }

    @Transactional(readOnly= true )
    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }
}
