package com.frist.assesspro.controllers;

import com.frist.assesspro.dto.DashboardStatsDTO;
import com.frist.assesspro.dto.statistics.TestSummaryDTO;
import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.service.DashboardService;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final TesterStatisticsService testerStatisticsService;
    private final TestService testService;

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

        if (role.equals("ROLE_CREATOR")) {
            return "redirect:/creator/dashboard";
        } else if (role.equals("ROLE_TESTER")) {
            return "redirect:/tester/dashboard";
        }

        return "dashboard";
    }

    @GetMapping("/creator/dashboard")
    @PreAuthorize("hasRole('CREATOR')")
    public String creatorDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        DashboardStatsDTO stats = dashboardService.getCreatorStats(username);

        // Получаем последние 5 попыток тестировщиков
        List<TesterAttemptDTO> recentAttempts = testerStatisticsService
                .getRecentTestAttemptsForCreator(username, 5);

        // Получаем общее количество уникальных тестировщиков
        long totalTesters = testerStatisticsService.getTotalTestersForCreator(username);

        model.addAttribute("username", username);
        model.addAttribute("role", auth.getAuthorities().iterator().next().getAuthority());
        model.addAttribute("message", "Добро пожаловать в панель создателя тестов!");
        model.addAttribute("stats", stats);
        model.addAttribute("recentAttempts", recentAttempts);
        model.addAttribute("totalTesters", totalTesters);

        return "creator/dashboard";
    }

    @GetMapping("/creator/tests/{testId}/statistics")
    @PreAuthorize("hasRole('CREATOR')")
    public String testStatisticsMain(
            @PathVariable Long testId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Использовать метод без загрузки вопросов и ответов
            Test test = testService.getTestBasicById(testId, userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            model.addAttribute("test", test);

            // Быстрая статистика по тесту
            TestSummaryDTO testSummary = testerStatisticsService.getTestSummary(testId, userDetails.getUsername());
            model.addAttribute("testSummary", testSummary);

            return "creator/testers-statistics-main";

        } catch (Exception e) {
            log.error("Ошибка при загрузке статистики теста", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при загрузке статистики: " + e.getMessage());
            return "redirect:/creator/tests";
        }
    }
}

