package com.frist.assesspro.util;


import com.frist.assesspro.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String getCurrentUsername() {
        Authentication auth = getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    public static User getCurrentUser() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    public static boolean isCurrentUserCreator() {
        User user = getCurrentUser();
        return user != null && user.getRole().equals(User.Roles.CREATOR);
    }

    public static boolean isCurrentUserTester() {
        User user = getCurrentUser();
        return user != null && user.getRole().equals(User.Roles.TESTER);
    }
}

