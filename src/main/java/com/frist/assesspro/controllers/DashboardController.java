package com.frist.assesspro.controllers;

import com.frist.assesspro.entity.User;
import com.frist.assesspro.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)  // ← ДОБАВЬТЕ ЭТО!
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }

        // Используем только имя пользователя из Authentication
        String username = auth.getName();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        model.addAttribute("username", username);
        model.addAttribute("role", role);

        // Перенаправляем в зависимости от роли
        if (role.equals("ROLE_CREATOR")) {
            return "redirect:/creator/dashboard";
        } else if (role.equals("ROLE_TESTER")) {
            return "redirect:/tester/dashboard";
        }

        return "dashboard";
    }

    @GetMapping("/creator/dashboard")
    @PreAuthorize("hasRole('CREATOR')")
    @Transactional(readOnly = true)
    public String creatorDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        model.addAttribute("role", auth.getAuthorities().iterator().next().getAuthority());
        model.addAttribute("message", "Добро пожаловать в панель создателя тестов!");
        return "creator/dashboard";
    }

    @GetMapping("/tester/dashboard")
    @PreAuthorize("hasRole('TESTER')")
    @Transactional(readOnly = true)
    public String testerDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        model.addAttribute("role", auth.getAuthorities().iterator().next().getAuthority());
        model.addAttribute("message", "Добро пожаловать в каталог тестов!");
        return "tester/dashboard";
    }
}