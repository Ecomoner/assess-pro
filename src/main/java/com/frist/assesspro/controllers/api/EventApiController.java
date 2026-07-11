package com.frist.assesspro.controllers.api;


import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.EventService;
import com.frist.assesspro.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "События",description = "API для создателей")
public class EventApiController {

    private final EventService eventService;
    private final UserService userService;

    // Получить события для календаря
    @GetMapping
    public ResponseEntity<List<EventDTO>> getEvents(
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr) {
        // Извлекаем только дату (первые 10 символов)
        LocalDate start = LocalDate.parse(startStr.substring(0, 10));
        LocalDate end = LocalDate.parse(endStr.substring(0, 10));
        return ResponseEntity.ok(eventService.getEventsForCalendar(start, end));
    }

    // Создать (только CREATOR)
    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<EventDTO> createEvent(@RequestBody EventDTO dto,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User creator = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        EventDTO created = eventService.createEvent(dto, creator);
        return ResponseEntity.ok(created);
    }

    // Обновить (только CREATOR)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<EventDTO> updateEvent(@PathVariable Long id, @RequestBody EventDTO dto) {
        return ResponseEntity.ok(eventService.updateEvent(id, dto));
    }

    // Удалить (только CREATOR)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

}
