package com.frist.assesspro.service;

import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("active");
        activeUser.setPassword("pass");
        activeUser.setRole("ROLE_USER");
        activeUser.setIsActive(true);

        inactiveUser = new User();
        inactiveUser.setId(2L);
        inactiveUser.setUsername("inactive");
        inactiveUser.setPassword("pass");
        inactiveUser.setRole("ROLE_USER");
        inactiveUser.setIsActive(false);
    }

    @Test
    @DisplayName("loadUserByUsername: успешная загрузка активного пользователя")
    void loadUserByUsername_Success() {
        when(userRepository.findByUsername("active")).thenReturn(Optional.of(activeUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("active");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("active");
        assertThat(userDetails.getPassword()).isEqualTo("pass");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername: пользователь не найден -> исключение")
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Пользователь с именем 'unknown' не найден");
    }

    @Test
    @DisplayName("loadUserByUsername: неактивный пользователь -> исключение")
    void loadUserByUsername_InactiveUser_ThrowsException() {
        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("inactive"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Учетная запись отключена");
    }
}