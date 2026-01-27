package com.frist.assesspro.util;


import com.frist.assesspro.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {


    public static Authentication getAuthentication(){
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String getCurrenName(){
        Authentication auth = getAuthentication();
        return auth != null? auth.getName():null;
    }

    public static boolean isCurrentUserCreator(){
        Authentication auth = getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch( a -> a.getAuthority().equals(User.Roles.CREATOR));
    }

    public static boolean isCurrentThisUser(){
        Authentication auth = getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(User.Roles.USER));
    }

    public static boolean isCurrentUserAdmin(){
        Authentication auth = getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(User.Roles.ADMIN));
    }

    public static User getCurrentUser(){
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User){
            return (User) auth.getPrincipal();
        }
        return null;
    }

}
