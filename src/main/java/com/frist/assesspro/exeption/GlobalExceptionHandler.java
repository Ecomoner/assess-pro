package com.frist.assesspro.exeption;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Обработка ошибок валидации форм
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationExceptions(
            MethodArgumentNotValidException ex,
            RedirectAttributes redirectAttributes) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Ошибки валидации: {}", errors);

        // Сохраняем ошибки для отображения на форме
        redirectAttributes.addFlashAttribute("validationErrors", errors);
        redirectAttributes.addFlashAttribute("errorMessage", "Пожалуйста, исправьте ошибки в форме");

        return "redirect:/error/validation";
    }

    // Обработка ConstraintViolationException (валидация в сервисах)
    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolation(
            ConstraintViolationException ex,
            RedirectAttributes redirectAttributes) {

        String errors = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("Нарушение ограничений: {}", errors);

        redirectAttributes.addFlashAttribute("errorMessage", errors);
        return "redirect:/error/constraint";
    }

    // Обработка IllegalArgumentException (бизнес-логика)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(
            IllegalArgumentException ex,
            RedirectAttributes redirectAttributes) {

        log.warn("Некорректный аргумент: {}", ex.getMessage());

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/error/business";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(
            Exception ex,
            Model model,
            HttpServletRequest request) {

        log.error("Неожиданная ошибка при запросе: {}", request.getRequestURI(), ex);

        // Безопасное сообщение для пользователя
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorMessage", "Внутренняя ошибка сервера. Пожалуйста, попробуйте позже.");

        // Дополнительная информация только для админов
        if (request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_CREATOR")) {
            model.addAttribute("errorDetails", ex.getMessage());
            model.addAttribute("stackTrace", Arrays.toString(ex.getStackTrace()));
        }

        return "error/general";
    }

    private boolean isAdmin(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN");
    }

}