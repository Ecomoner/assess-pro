package com.frist.assesspro.controllers;

import com.frist.assesspro.dto.DashboardStatsDTO;
import com.frist.assesspro.dto.statistics.TestSummaryDTO;
import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.service.DashboardService;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
import com.frist.assesspro.service.metrics.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Дашборд",description = "API для дашборда")
public class DashboardController {

    private final MetricsService metricsService;

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @Operation(summary = "Дашборд в зависимости от роли")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }

        String username = auth.getName();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        model.addAttribute("username", username);
        model.addAttribute("role", role);

        if (role.equals("ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        } else if (role.equals("ROLE_CREATOR")) {
            return "redirect:/creator/dashboard";
        } else if (role.equals("ROLE_TESTER")) {
            return "redirect:/tester/dashboard";
        }

        return "redirect:/home";
    }
}

