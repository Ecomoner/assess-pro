package com.frist.assesspro.interceptor;

import com.frist.assesspro.service.ProfileService;
import com.frist.assesspro.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ProfileInterceptor implements HandlerInterceptor {

    private final ProfileService profileService;
    private final UserService userService;

    // URL-ы, которые не требуют проверки профиля
    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/login", "/register", "/logout", "/profile/complete",
            "/css/", "/js/", "/images/", "/webjars/", "/error", "/access-denied"
    );

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String requestURI = request.getRequestURI();

        // Пропускаем публичные URL
        for (String url : PUBLIC_URLS) {
            if (requestURI.startsWith(url)) {
                return true;
            }
        }

        // Проверяем аутентификацию
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return true;
        }

        // Получаем username
        String username;
        if (auth.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) auth.getPrincipal()).getUsername();
        } else {
            username = auth.getPrincipal().toString();
        }

        // 🔥 ИСПРАВЛЕНО: Проверяем существование пользователя
        if (!userService.existsByUsername(username)) {
            log.warn("Пользователь {} не найден в БД", username);
            return true;
        }

        // Проверяем заполненность профиля
        if (!profileService.isProfileComplete(username)) {
            log.debug("Пользователь {} не заполнил профиль, перенаправление на /profile/complete", username);
            response.sendRedirect("/profile/complete");
            return false;
        }

        return true;
    }
}