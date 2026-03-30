package com.frist.assesspro.controllers.creator;


import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.dto.category.CategoryCreateDTO;
import com.frist.assesspro.dto.category.CategoryDTO;
import com.frist.assesspro.dto.category.CategoryUpdateDTO;
import com.frist.assesspro.entity.Category;
import com.frist.assesspro.entity.Event;
import com.frist.assesspro.service.EventService;
import com.frist.assesspro.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/creator/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "События",description = "API для создателей")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "Получение списка всех событиий")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping()
    public String getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        Page<EventDTO> eventsPage = eventService.getAllEvents(pageable);

        Map<String, String> params = new HashMap<>();
        params.put("size", String.valueOf(size));
        params.put("sort", sort);

        PaginationUtils.addPaginationAttributes(model, eventsPage, "/creator/events", params);

        model.addAttribute("events", eventsPage.getContent());
        model.addAttribute("sort", sort);
        model.addAttribute("pageSize", size);

        return "creator/event-list";
    }

    @Operation(summary = "Форма создания события")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/new")
    public String showCreateEventForm(Model model) {
        model.addAttribute("eventDTO", new EventDTO());
        model.addAttribute("action", "create");
        return "creator/event-form";
    }

    @Operation(summary = "Создание нового события")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/new")
    public String createEvent(
            @Valid @ModelAttribute EventDTO eventDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибки валидации: " + bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("eventDTO", eventDTO);
            return "redirect:/creator/events/new";
        }

        try {
            Event event = eventService.createEvent(eventDTO, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Событие '" + event.getName() + "' успешно создано!");
            return "redirect:/creator/events";
        } catch (Exception e) {
            log.error("Ошибка при создании события", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при создании события: " + e.getMessage());
            redirectAttributes.addFlashAttribute("eventDTO", eventDTO);
            return "redirect:/creator/events/new";
        }
    }

    @Operation(summary = "Форма редактирования события")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/edit/{id}")
    public String showEventEditForm(
            @PathVariable Long id,
            Model model){
        return eventService.getEventById(id)
                .map(event -> {
                    EventDTO updateEventDTO = new EventDTO();
                    updateEventDTO.setName(event.getName());
                    updateEventDTO.setDescription(event.getDescription());

                    model.addAttribute("eventDTO", updateEventDTO);
                    model.addAttribute("eventId", id);
                    model.addAttribute("formAction", "/creator/events/update/" + id);
                    model.addAttribute("action", "edit");
                    return "creator/event-form";
                })
                .orElse("redirect:/creator/events?error=not_found");
    }

    @Operation(summary = "Обновление события")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/update/{id}")
    public String updateEvent(
            @PathVariable Long id,
            @Valid @ModelAttribute EventDTO updateDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибки валидации: " + bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("eventDTO", updateDTO);
            return "redirect:/creator/events/edit/" + id;
        }

        try {
            Event category = eventService.editEvent(id,updateDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Событие '" + category.getName() + "' успешно обновлено!");
            return "redirect:/creator/events";
        } catch (Exception e) {
            log.error("Ошибка при обновлении события", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при обновлении категории: " + e.getMessage());
            redirectAttributes.addFlashAttribute("eventDTO", updateDTO);
            return "redirect:/creator/events/edit/" + id;
        }
    }

    @Operation(summary = "Удаление события")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/delete/{id}")
    public String deleteEvent(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            eventService.deleteEvent(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Событие успешно удалено!");
        } catch (Exception e) {
            log.error("Ошибка при удалении события", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при удалении события: " + e.getMessage());
        }
        return "redirect:/creator/events";
    }

}
