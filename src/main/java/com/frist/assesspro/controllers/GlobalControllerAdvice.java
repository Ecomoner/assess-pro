package com.frist.assesspro.controllers;

import com.frist.assesspro.service.TestPassingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired(required = false)
    private TestPassingService testPassingService;

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("hasActiveAttempt")
    public boolean hasActiveAttempt(Principal principal) {
        if (principal != null && testPassingService != null) {
            return testPassingService.hasActiveAttempt(principal.getName());
        }
        return false;
    }
}