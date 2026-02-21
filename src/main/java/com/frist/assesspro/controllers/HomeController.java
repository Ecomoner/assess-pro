package com.frist.assesspro.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Tag(name = "Стартовая страница",description = "API для стартовой старницы")
public class HomeController {

    @Operation(summary = "Стартовая страница")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/")
    public String home() {
        return "redirect:/home";
    }

    @Operation(summary = "Стартовая страница")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/home")
    public String homePage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null &&
                auth.isAuthenticated() &&
                !auth.getPrincipal().equals("anonymousUser");

        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            model.addAttribute("username", auth.getName());
            model.addAttribute("role", auth.getAuthorities().iterator().next().getAuthority());
        }

        return "home";
    }

    @Operation(summary = "Переход на страницу ошибок")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}
