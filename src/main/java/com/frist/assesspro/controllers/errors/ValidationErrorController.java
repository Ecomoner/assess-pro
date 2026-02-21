package com.frist.assesspro.controllers.errors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/error")
@Slf4j
@Tag(name = "Ошибки",description = "API для ошибок")
public class ValidationErrorController {

    @Operation(summary = "Валидация")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/validation")
    public String showValidationErrors(Model model) {
        // Сообщение по умолчанию
        if (!model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", "Обнаружены ошибки в форме");
        }
        return "error/validation";
    }
}
