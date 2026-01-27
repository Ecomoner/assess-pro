package com.frist.assesspro.controllers;

import com.frist.assesspro.dto.RegistrationDTO;
import com.frist.assesspro.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(){
        return "auth/login";
    }


    @GetMapping("/register")
    public String registerPage(Model model){
        if (!model.containsAttribute("registrationDTO")){
            model.addAttribute("registrationDTO",new RegistrationDTO());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute RegistrationDTO registrationDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes){

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.registrationDTO",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("registrationDTO", registrationDTO);
            return "redirect:/register";
        }

        try {
            authService.registerUser(registrationDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Регистрация прошла успешно! Теперь вы можете войти.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("registrationDTO", registrationDTO);
            return "redirect:/register";
        }
    }
    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }


}
