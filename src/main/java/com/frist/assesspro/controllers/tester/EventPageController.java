package com.frist.assesspro.controllers.tester;


import com.frist.assesspro.dto.EventDTO;
import com.frist.assesspro.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tester/events")
@PreAuthorize("hasAnyRole('TESTER','MANAGER')")
@RequiredArgsConstructor
public class EventPageController {

    private final EventService eventService;

    @GetMapping("/{id}")
    public String viewEvent(@PathVariable Long id, Model model) {
        EventDTO event = eventService.getEvent(id);
        model.addAttribute("event", event);
        return "tester/event-details";
    }
}
