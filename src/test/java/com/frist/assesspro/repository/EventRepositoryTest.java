package com.frist.assesspro.repository;

import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.entity.Event;
import com.frist.assesspro.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EventRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private User creator;
    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        creator = new User();
        creator.setUsername("event_creator");
        creator.setPassword("pass");
        creator.setRole(User.Roles.CREATOR);
        creator = userRepository.save(creator);

        event1 = new Event();
        event1.setName("Event 1");
        event1.setDescription("Description 1");
        event1.setCreatedByEvent(creator);
        event1 = eventRepository.save(event1);

        event2 = new Event();
        event2.setName("Event 2");
        event2.setDescription("Description 2");
        event2.setCreatedByEvent(creator);
        event2 = eventRepository.save(event2);
    }

    @Test
    @DisplayName("findAllEventDTOs: должен вернуть страницу DTO")
    void findAllEventDTOs_ShouldReturnPageOfDTOs() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Page<EventDTO> page = eventRepository.findAllEventDTOs(pageable);

        assertThat(page.getContent()).hasSize(2);
        EventDTO dto = page.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(event1.getId());
        assertThat(dto.getName()).isEqualTo("Event 1");
        assertThat(dto.getDescription()).isEqualTo("Description 1");
    }

    @Test
    @DisplayName("getTotalEvent: должен вернуть общее количество событий")
    void getTotalEvent_ShouldReturnCount() {
        long total = eventRepository.getTotalEvent();
        assertThat(total).isEqualTo(2);
    }

    @Test
    @DisplayName("existsByName: должен проверить существование по имени")
    void existsByName_ShouldWork() {
        assertThat(eventRepository.existsByName("Event 1")).isTrue();
        assertThat(eventRepository.existsByName("Non existent")).isFalse();
    }

    @Test
    @DisplayName("findTop5ByOrderByIdDesc: должен вернуть последние 5 событий")
    void findTop5ByOrderByIdDesc_ShouldReturnLastEvents() {
        List<Event> lastEvents = eventRepository.findTop5ByOrderByIdDesc();
        assertThat(lastEvents).hasSize(2);
        // последнее добавленное должно быть первым
        assertThat(lastEvents.get(0).getId()).isEqualTo(event2.getId());
        assertThat(lastEvents.get(1).getId()).isEqualTo(event1.getId());
    }
}