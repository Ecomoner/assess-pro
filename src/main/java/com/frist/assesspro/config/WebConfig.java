package com.frist.assesspro.config;

import com.frist.assesspro.interceptor.ProfileInterceptor;
import com.frist.assesspro.service.ProfileService;
import com.frist.assesspro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ProfileService profileService;
    private final UserService userService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ProfileInterceptor(profileService, userService))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/register", "/logout",
                        "/profile/complete",
                        "/css/**", "/js/**", "/images/**",
                        "/webjars/**", "/error", "/access-denied"
                );
    }
}