package com.frist.assesspro.service;

import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Загрузка пользователя по имени: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: {}", username);
                    return new UsernameNotFoundException(
                            "Пользователь с именем '" + username + "' не найден"
                    );
                });
        if (!user.isEnabled()) {
            log.error("Учетная запись отключена: {}", username);
            throw new UsernameNotFoundException("Учетная запись отключена");
        }

        log.debug("Пользователь найден: {} с ролью: {}",
                user.getUsername(), user.getRole());

        return user;
    }
}
