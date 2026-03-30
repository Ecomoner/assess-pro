package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.entity.Event;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "CREATOR")
@ActiveProfiles("test")
@DisplayName("Тесты контроллера событий (EventController)")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private final Long EVENT_ID = 1L;
    private final String EVENT_NAME = "Test Event";
    private final String EVENT_DESC = "Test Description";

    private Event event;
    private EventDTO eventDTO;

    @BeforeEach
    void setUp() {
        User creator = new User();
        creator.setUsername("user");

        event = new Event();
        event.setId(EVENT_ID);
        event.setName(EVENT_NAME);
        event.setDescription(EVENT_DESC);
        event.setCreatedByEvent(creator);

        eventDTO = new EventDTO();
        eventDTO.setId(EVENT_ID);
        eventDTO.setName(EVENT_NAME);
        eventDTO.setDescription(EVENT_DESC);
    }

    // ---------- GET /creator/events ----------

    @Test
    @DisplayName("GET /creator/events: должен вернуть view со списком событий")
    void getAllEvents_ShouldReturnView() throws Exception {
        int page = 0, size = 10;
        String sort = "name";
        Page<EventDTO> pageResult = new PageImpl<>(List.of(eventDTO), PageRequest.of(page, size), 1);

        when(eventService.getAllEvents(any(PageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get("/creator/events")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/event-list"))
                .andExpect(model().attribute("events", pageResult.getContent()))
                .andExpect(model().attribute("sort", sort))
                .andExpect(model().attribute("pageSize", size))
                .andExpect(model().attributeExists("startPage", "endPage", "baseUrl", "queryString"));
    }

    // ---------- GET /creator/events/new ----------

    @Test
    @DisplayName("GET /creator/events/new: должен вернуть форму создания")
    void showCreateEventForm_ShouldReturnView() throws Exception {
        mockMvc.perform(get("/creator/events/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/event-form"))
                .andExpect(model().attributeExists("eventDTO"))
                .andExpect(model().attribute("action", "create"));
    }

    // ---------- POST /creator/events/new ----------

    @Test
    @DisplayName("POST /creator/events/new: успешное создание, редирект на список")
    void createEvent_Success_ShouldRedirect() throws Exception {
        when(eventService.createEvent(any(EventDTO.class), anyString())).thenReturn(event);

        mockMvc.perform(post("/creator/events/new")
                        .param("name", EVENT_NAME)
                        .param("description", EVENT_DESC))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/events"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(eventService).createEvent(any(EventDTO.class), eq("user"));
    }

    @Test
    @DisplayName("POST /creator/events/new: ошибка валидации, редирект с ошибкой")
    void createEvent_ValidationError_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/creator/events/new")
                        .param("name", "")
                        .param("description", EVENT_DESC))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/events/new"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("eventDTO"));

        verify(eventService, never()).createEvent(any(), any());
    }

    @Test
    @DisplayName("POST /creator/events/new: исключение сервиса, редирект с ошибкой")
    void createEvent_ServiceException_ShouldRedirectWithError() throws Exception {
        when(eventService.createEvent(any(EventDTO.class), anyString()))
                .thenThrow(new RuntimeException("Creation failed"));

        mockMvc.perform(post("/creator/events/new")
                        .param("name", EVENT_NAME)
                        .param("description", EVENT_DESC))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/events/new"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("eventDTO"));
    }

    // ---------- GET /creator/events/edit/{id} ----------

    @Test
    @DisplayName("GET /creator/events/edit/{id}: успешный просмотр формы редактирования")
    void showEventEditForm_Success_ShouldReturnView() throws Exception {
        when(eventService.getEventById(EVENT_ID)).thenReturn(Optional.of(event));

        mockMvc.perform(get("/creator/events/edit/{id}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/event-form"))
                .andExpect(model().attributeExists("eventDTO"))
                .andExpect(model().attribute("eventId", EVENT_ID))
                .andExpect(model().attribute("formAction", "/creator/events/update/" + EVENT_ID))
                .andExpect(model().attribute("action", "edit"));
    }

    @Test
    @DisplayName("GET /creator/events/edit/{id}: событие не найдено, редирект")
    void showEventEditForm_NotFound_ShouldRedirect() throws Exception {
        when(eventService.getEventById(EVENT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/creator/events/edit/{id}", EVENT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/events?error=not_found"));
    }

    // ---------- POST /creator/events/update/{id} ----------

    @Test
    @DisplayName("POST /creator/events/update/{id}: успешное обновление, редирект на список")
    void updateEvent_Success_ShouldRedirect() throws Exception {
        when(eventService.editEvent(eq(EVENT_ID), any(EventDTO.class))).thenReturn(event);

        mockMvc.perform(post("/creator/events/update/{id}", EVENT_ID)
                        .param("name", "Updated Name")
                        .param("description", "Updated Desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/events"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /creator/events/update/{id}: ошибка валидации, редирект с ошибкой")
    void updateEvent_ValidationError_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/creator/events/update/{id}", EVENT_ID)
                        .param("name", "")
                        .param("description", "Desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/events/edit/" + EVENT_ID))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("eventDTO"));
    }

    @Test
    @DisplayName("POST /creator/events/update/{id}: исключение сервиса, редирект с ошибкой")
    void updateEvent_ServiceException_ShouldRedirectWithError() throws Exception {
        when(eventService.editEvent(eq(EVENT_ID), any(EventDTO.class)))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(post("/creator/events/update/{id}", EVENT_ID)
                        .param("name", "Updated Name")
                        .param("description", "Updated Desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/events/edit/" + EVENT_ID))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("eventDTO"));
    }

    // ---------- POST /creator/events/delete/{id} ----------

    @Test
    @DisplayName("POST /creator/events/delete/{id}: успешное удаление, редирект с сообщением")
    void deleteEvent_Success_ShouldRedirect() throws Exception {
        doNothing().when(eventService).deleteEvent(EVENT_ID);

        mockMvc.perform(post("/creator/events/delete/{id}", EVENT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/events"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /creator/events/delete/{id}: исключение сервиса, редирект с ошибкой")
    void deleteEvent_ServiceException_ShouldRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Delete failed")).when(eventService).deleteEvent(EVENT_ID);

        mockMvc.perform(post("/creator/events/delete/{id}", EVENT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/events"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}