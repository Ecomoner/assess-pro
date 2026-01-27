package com.frist.assesspro.controllers;

import com.frist.assesspro.model.User;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthTestController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/password-check")
    public String passwordCheckPage() {
        return "auth/password-check";
    }

    @PostMapping("/password-check")
    public String checkPassword(@RequestParam String username,
                                @RequestParam String password,
                                Model model) {

        var userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            model.addAttribute("result", "❌ Пользователь не найден: " + username);
            return "auth/password-check";
        }

        User user = userOpt.get();
        String storedHash = user.getPassword();
        boolean matches = passwordEncoder.matches(password, storedHash);

        String result = String.format("""
            <h3>Результат проверки:</h3>
            <p><strong>Пользователь:</strong> %s</p>
            <p><strong>Роль:</strong> %s</p>
            <p><strong>Активен:</strong> %s</p>
            <p><strong>Введенный пароль:</strong> %s</p>
            <p><strong>Хеш в базе:</strong> %s</p>
            <p><strong>Совпадение паролей:</strong> %s</p>
            <p><strong>isEnabled():</strong> %s</p>
            """,
                username,
                user.getRole(),
                user.getIsActive(),
                password,
                storedHash.substring(0, 30) + "...",
                matches ? "✅ СОВПАДАЕТ" : "❌ НЕ СОВПАДАЕТ",
                user.isEnabled()
        );

        model.addAttribute("result", result);
        return "auth/password-check";
    }
}
