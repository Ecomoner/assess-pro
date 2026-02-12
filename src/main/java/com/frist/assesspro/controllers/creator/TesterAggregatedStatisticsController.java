package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.statistics.TesterAggregatedStatsDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/creator/tests/{testId}/statistics/aggregated")
@RequiredArgsConstructor
@Slf4j
public class TesterAggregatedStatisticsController {

    private final TesterStatisticsService testerStatisticsService;
    private final TestService testService;

    /**
     * Главная страница агрегированной статистики (группировка по тестировщикам)
     */
    @GetMapping("/testers")
    public String getAggregatedTesters(
            @PathVariable Long testId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Test test = testService.getTestById(testId, userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            Pageable pageable = PageRequest.of(page, size);
            Page<TesterAggregatedStatsDTO> aggregatedTestersPage = testerStatisticsService
                    .getAggregatedTestersByTest(testId, userDetails.getUsername(), pageable, dateFrom, dateTo);

            model.addAttribute("test", test);
            model.addAttribute("aggregatedTesters", aggregatedTestersPage.getContent());
            model.addAttribute("currentPage", aggregatedTestersPage.getNumber());
            model.addAttribute("totalPages", aggregatedTestersPage.getTotalPages());
            model.addAttribute("totalItems", aggregatedTestersPage.getTotalElements());
            model.addAttribute("dateFrom", dateFrom);
            model.addAttribute("dateTo", dateTo);

            return "creator/aggregated-testers-statistics";

        } catch (Exception e) {
            log.error("Ошибка при загрузке агрегированной статистики", e);
            return "redirect:/creator/tests/" + testId + "/statistics?error=" + e.getMessage();
        }
    }

    /**
     * Детальная агрегированная статистика конкретного тестировщика
     */
    @GetMapping("/tester/{testerUsername}")
    public String getTesterAggregatedStats(
            @PathVariable Long testId,
            @PathVariable String testerUsername,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Test test = testService.getTestById(testId, userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Тест не найден"));

            TesterAggregatedStatsDTO aggregatedStats = testerStatisticsService
                    .getTesterAggregatedStats(testId, testerUsername, userDetails.getUsername());

            model.addAttribute("test", test);
            model.addAttribute("aggregatedStats", aggregatedStats);
            model.addAttribute("testerUsername", testerUsername);
            model.addAttribute("dateFrom", dateFrom);
            model.addAttribute("dateTo", dateTo);

            return "creator/tester-aggregated-stats";

        } catch (Exception e) {
            log.error("Ошибка при загрузке агрегированной статистики тестировщика", e);
            return "redirect:/creator/tests/" + testId + "/statistics/aggregated/testers?error=" + e.getMessage();
        }
    }

    /**
     * Быстрый просмотр агрегированной статистики (AJAX)
     */
    @GetMapping("/tester/{testerUsername}/quick-view")
    @ResponseBody
    public TesterAggregatedStatsDTO getTesterAggregatedQuickView(
            @PathVariable Long testId,
            @PathVariable String testerUsername,
            @AuthenticationPrincipal UserDetails userDetails) {

        return testerStatisticsService.getTesterAggregatedStats(
                testId, testerUsername, userDetails.getUsername());
    }
}