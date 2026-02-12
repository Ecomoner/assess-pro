package com.frist.assesspro.controllers.errors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/error")
@Slf4j
public class ValidationErrorController {

    @GetMapping("/validation")
    public String showValidationErrors(Model model) {
        // Сообщение по умолчанию
        if (!model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", "Обнаружены ошибки в форме");
        }
        return "error/validation";
    }

    @GetMapping("/constraint")
    public String showConstraintErrors(Model model) {
        return "error/constraint";
    }

    @GetMapping("/business")
    public String showBusinessErrors(Model model) {
        return "error/business";
    }

    @GetMapping("/data-integrity")
    public String showDataIntegrityErrors(Model model) {
        return "error/data-integrity";
    }
}
