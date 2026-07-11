package com.frist.assesspro.controllers.creator;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/creator/events")
@PreAuthorize("hasRole('CREATOR')")
public class CreatorEventController {

    @GetMapping
    public String manageEvents() {
        return "creator/events-management";
    }
}