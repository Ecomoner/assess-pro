package com.frist.assesspro.service;

import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.entity.Event;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.EventRepository;
import com.frist.assesspro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса событий")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private final String CREATOR_USERNAME = "creator";
    private User creator;
    private Event event;
    private EventDTO eventDTO;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setUsername(CREATOR_USERNAME);

        event = new Event();
        event.setId(1L);
        event.setName("Test Event");
        event.setDescription("Test Description");
        event.setCreatedByEvent(creator);

        eventDTO = new EventDTO();
        eventDTO.setId(1L);
        eventDTO.setName("Test Event");
        eventDTO.setDescription("Test Description");
    }

    // ---------- createEvent ----------

    @Test
    @DisplayName("createEvent: успешное создание")
    void createEvent_Success_ShouldReturnEvent() {
        when(userRepository.findByUsername(CREATOR_USERNAME)).thenReturn(Optional.of(creator));
        when(eventRepository.existsByName(eventDTO.getName())).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        Event result = eventService.createEvent(eventDTO, CREATOR_USERNAME);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Event");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("createEvent: пользователь не найден -> исключение")
    void createEvent_UserNotFound_ShouldThrowException() {
        when(userRepository.findByUsername(CREATOR_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(eventDTO, CREATOR_USERNAME))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    @Test
    @DisplayName("createEvent: событие с таким именем уже существует -> исключение")
    void createEvent_DuplicateName_ShouldThrowException() {
        when(userRepository.findByUsername(CREATOR_USERNAME)).thenReturn(Optional.of(creator));
        when(eventRepository.existsByName(eventDTO.getName())).thenReturn(true);

        assertThatThrownBy(() -> eventService.createEvent(eventDTO, CREATOR_USERNAME))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    @DisplayName("createEvent: описание null -> исключение")
    void createEvent_NullDescription_ShouldThrowException() {
        eventDTO.setDescription(null);
        when(userRepository.findByUsername(CREATOR_USERNAME)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> eventService.createEvent(eventDTO, CREATOR_USERNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Описание событие не может быть пустым");
    }

    // ---------- editEvent ----------

    @Test
    @DisplayName("editEvent: успешное обновление описания")
    void editEvent_Success_ShouldUpdateDescription() {
        EventDTO updateDTO = new EventDTO();
        updateDTO.setId(1L);
        updateDTO.setDescription("Updated description");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);

        Event result = eventService.editEvent(1L, updateDTO);

        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("editEvent: событие не найдено -> исключение")
    void editEvent_NotFound_ShouldThrowException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.editEvent(999L, eventDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Событии с таким ID нет");
    }

    // ---------- deleteEvent ----------

    @Test
    @DisplayName("deleteEvent: успешное удаление")
    void deleteEvent_Success_ShouldDelete() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(eventRepository).delete(event);

        eventService.deleteEvent(1L);

        verify(eventRepository).delete(event);
    }

    @Test
    @DisplayName("deleteEvent: событие не найдено -> исключение")
    void deleteEvent_NotFound_ShouldThrowException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.deleteEvent(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Событии с таким ID нет");
    }

    // ---------- getAllEvents ----------

    @Test
    @DisplayName("getAllEvents: должен вернуть страницу DTO")
    void getAllEvents_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<EventDTO> expectedPage = new PageImpl<>(List.of(eventDTO), pageable, 1);

        when(eventRepository.findAllEventDTOs(pageable)).thenReturn(expectedPage);

        Page<EventDTO> result = eventService.getAllEvents(pageable);

        assertThat(result).isEqualTo(expectedPage);
    }

    // ---------- getLastEvents ----------

    @Test
    @DisplayName("getLastEvents: должен вернуть последние события")
    void getLastEvents_ShouldReturnList() {
        when(eventRepository.findTop5ByOrderByIdDesc()).thenReturn(List.of(event));

        List<EventDTO> result = eventService.getLastEvents(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Event");
    }

    // ---------- getEventById ----------

    @Test
    @DisplayName("getEventById: должен вернуть Optional")
    void getEventById_ShouldReturnOptional() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        Optional<Event> result = eventService.getEventById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Event");
    }

    // ---------- convertToDTO ----------

    @Test
    @DisplayName("convertToDTO: корректное преобразование")
    void convertToDTO_ShouldConvert() {
        EventDTO dto = eventService.convertToDTO(event);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Test Event");
        assertThat(dto.getDescription()).isEqualTo("Test Description");
    }
}