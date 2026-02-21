package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.statistics.*;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.TesterStatisticsService;
import com.frist.assesspro.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/creator/testers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Получение статистики",description = "API для создателей")
public class TesterOverallStatisticsController {

    private final TesterStatisticsService testerStatisticsService;
    private final UserService userService;
    private final TestService testService;

    @Operation(summary = "Получение получение статистики тестировщика")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/{testerId}/statistics")
    public String getTesterOverallStatistics(
            @PathVariable Long testerId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            // Получаем информацию о тестировщике
            User tester = userService.getUserById(testerId)
                    .orElseThrow(() -> new RuntimeException("Тестировщик не найден"));
            // Проверяем, что это действительно тестировщик
            if (!"ROLE_TESTER".equals(tester.getRole())) {
                throw new RuntimeException("Пользователь не является тестировщиком");
            }
            // Получаем все попытки тестировщика
            List<TesterAttemptDTO> allAttempts = testerStatisticsService.getAllAttemptsByTester(tester.getUsername());
            // Агрегируем статистику по тестам
            Map<Long, List<TesterAttemptDTO>> attemptsByTest = allAttempts.stream()
                    .collect(Collectors.groupingBy(TesterAttemptDTO::getTestId));

            // Создаем список статистики по каждому тесту
            List<TestTesterStatisticsDTO> testStatistics = new ArrayList<>();
            for (Map.Entry<Long, List<TesterAttemptDTO>> entry : attemptsByTest.entrySet()) {
                Long testId = entry.getKey();
                List<TesterAttemptDTO> testAttempts = entry.getValue();

                // Получаем информацию о тесте
                Test test = testService.getTestBasicById(testId, userDetails.getUsername())
                        .orElse(null);

                if (test != null) {
                    TestTesterStatisticsDTO stat = new TestTesterStatisticsDTO();
                    stat.setTestId(testId);
                    stat.setTestTitle(test.getTitle());
                    stat.setAttempts(testAttempts);
                    stat.setTotalAttempts((long) testAttempts.size());

                    // Вычисляем статистику
                    double avgPercentage = testAttempts.stream()
                            .mapToDouble(TesterAttemptDTO::getPercentage)
                            .average()
                            .orElse(0.0);
                    stat.setAveragePercentage(avgPercentage);

                    double bestPercentage = testAttempts.stream()
                            .mapToDouble(TesterAttemptDTO::getPercentage)
                            .max()
                            .orElse(0.0);
                    stat.setBestPercentage(bestPercentage);

                    testStatistics.add(stat);
                }
            }

            // Общая статистика по всем тестам
            long totalAttempts = allAttempts.size();
            long completedAttempts = allAttempts.stream()
                    .filter(a -> a.getEndTime() != null)
                    .count();
            double overallAverage = allAttempts.stream()
                    .mapToDouble(TesterAttemptDTO::getPercentage)
                    .average()
                    .orElse(0.0);

            model.addAttribute("tester", tester);
            model.addAttribute("testStatistics", testStatistics);
            model.addAttribute("totalAttempts", totalAttempts);
            model.addAttribute("completedAttempts", completedAttempts);
            model.addAttribute("overallAverage", overallAverage);

            return "creator/tester-statistics";

        } catch (Exception e) {
            log.error("Ошибка при загрузке статистики тестировщика", e);
            model.addAttribute("errorMessage", "Ошибка: " + e.getMessage());
            return "redirect:/creator/testers";
        }
    }
}