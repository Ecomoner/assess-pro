package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.entity.RetryCooldownException;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.CooldownService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/creator/tests/{testId}/exceptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ограничение повторного прохождения",description = "API для создателей")
public class RetryCooldownExceptionController {

    private final CooldownService cooldownService;
    private final TestService testService;
    private final TesterStatisticsService testerStatisticsService;
    private final UserService userService;

    @Operation(summary = "Управление ограничением")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping
    public String manageExceptions(
            @PathVariable Long testId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Test test = testService.getTestByIdWithoutOwnershipCheck(testId);

        // 🔥 НОВОЕ: Получаем список всех тестировщиков, проходивших тест
        List<User> testers = testerStatisticsService.getDistinctTestersByTest(testId, userDetails.getUsername());

        model.addAttribute("test", test);
        model.addAttribute("testers", testers);
        return "creator/retry-exceptions";
    }

    @Operation(summary = "Снятие ограничений")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/remove/{testerUsername}")
    public String removeException(
            @PathVariable Long testId,
            @PathVariable String testerUsername,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            Test test = testService.getTestByIdWithoutOwnershipCheck(testId);

            // 🔥 ИСПРАВЛЕНО: Используем UserService
            User tester = userService.getUserByUsername(testerUsername);

            cooldownService.removeException(test, tester);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Ограничения для пользователя " + tester.getFullName() + " сняты");

        } catch (Exception e) {
            log.error("Ошибка при снятии ограничений", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка: " + e.getMessage());
        }

        return "redirect:/creator/tests/" + testId + "/statistics/testers";
    }

    @Operation(summary = "Снятие ограничений с таймером")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/create")
    public String createException(
            @PathVariable Long testId,
            @RequestParam String testerUsername,
            @RequestParam(required = false) Integer hours,
            @RequestParam(defaultValue = "false") boolean permanent,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            Test test = testService.getTestByIdWithoutOwnershipCheck(testId);

            // 🔥 ИСПРАВЛЕНО: Используем UserService
            User tester = userService.getUserByUsername(testerUsername);
            User creator = userService.getUserByUsername(userDetails.getUsername());

            cooldownService.createException(test, tester, creator, hours, permanent, reason);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Исключение создано для пользователя " + tester.getFullName());

        } catch (Exception e) {
            log.error("Ошибка при создании исключения", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка: " + e.getMessage());
        }

        return "redirect:/creator/tests/" + testId + "/statistics/testers";
    }

    @Operation(summary = "Получение статуса ограничения")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/status/{testerUsername}")
    @ResponseBody
    public Map<String, Object> getCooldownStatus(
            @PathVariable Long testId,
            @PathVariable String testerUsername,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        try {
            Test test = testService.getTestByIdWithoutOwnershipCheck(testId);

            User tester = userService.getUserByUsername(testerUsername);

            String status = cooldownService.getCooldownStatus(test, tester);
            LocalDateTime nextAvailable = cooldownService.getNextAvailableTime(test, tester);

            response.put("success", true);
            response.put("status", status);
            response.put("nextAvailable", nextAvailable != null ? nextAvailable.toString() : null);
            response.put("hasCooldown", test.hasRetryCooldown());
            response.put("testerName", tester.getFullName());

        } catch (Exception e) {
            log.error("Ошибка при получении статуса ограничений", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }
}