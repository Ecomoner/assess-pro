package com.frist.assesspro.config;

import com.frist.assesspro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig  {

//    private final UserDetailsServiceImpl userDetailsService;


    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


//    @Bean
//    public AuthenticationManager authenticationManager(
//            AuthenticationConfiguration authConfig) throws Exception {
//        return authConfig.getAuthenticationManager();
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) {
//        http.csrf(AbstractHttpConfigurer::disable) // Для упрощения, можно включить позже
//                .authorizeHttpRequests(auth -> auth
//                        // Публичные эндпоинты
//                        .requestMatchers("/", "/home", "/login","/password-check","/register").permitAll()
//                        // Только для создателей тестов
//                        .requestMatchers("/creator/**").hasRole("CREATOR")
//                        .requestMatchers("/admin/**").hasRole("ADMIN")
//                        // Только для тестирующих
//                        .requestMatchers("/user/**").hasRole("USER")
//                        // Все остальные запросы требуют аутентификации
//                        .anyRequest().authenticated())
//                .formLogin(form -> form
//                        .loginPage("/login")
//                        .loginProcessingUrl("/login")
//                        .defaultSuccessUrl("/dashboard", true)
//                        .failureUrl("/login?error=true")
//                        .usernameParameter("username")
//                        .passwordParameter("password")
//                        .permitAll()
//                )
//                .exceptionHandling(exceptions -> exceptions
//                        .accessDeniedPage("/access-denied")
//                );
//
//        return http.build();
//    }
}
