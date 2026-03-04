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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User admin;
    private User creator;
    private User tester;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(User.Roles.ADMIN);
        admin.setFirstName("Admin");
        admin.setLastName("Adminov");

        creator = new User();
        creator.setId(2L);
        creator.setUsername("creator");
        creator.setRole(User.Roles.CREATOR);
        creator.setFirstName("Создатель");
        creator.setLastName("Тестов");

        tester = new User();
        tester.setId(3L);
        tester.setUsername("tester");
        tester.setRole(User.Roles.TESTER);
        tester.setFirstName("Тестер");
        tester.setLastName("Петров");
    }

    @Test
    @DisplayName("findByUsername: пользователь найден")
    void findByUsername_Success() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        Optional<User> result = userService.findByUsername("admin");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("findByUsername: пользователь не найден")
    void findByUsername_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserByUsername: успешное получение")
    void getUserByUsername_Success() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

        User result = userService.getUserByUsername("creator");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("creator");
    }

    @Test
    @DisplayName("getUserByUsername: пользователь не найден -> ошибка")
    void getUserByUsername_NotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByUsername("unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    @Test
    @DisplayName("existsByUsername: существует -> true")
    void existsByUsername_True() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        boolean result = userService.existsByUsername("admin");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByUsername: не существует -> false")
    void existsByUsername_False() {
        when(userRepository.existsByUsername("unknown")).thenReturn(false);

        boolean result = userService.existsByUsername("unknown");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("findAllTesters: с поиском")
    void findAllTesters_WithSearch_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(tester), pageable, 1);

        when(userRepository.searchTesters("петров", pageable)).thenReturn(page);

        Page<User> result = userService.findAllTesters("петров", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("tester");
        verify(userRepository).searchTesters("петров", pageable);
    }

    @Test
    @DisplayName("findAllTesters: без поиска")
    void findAllTesters_WithoutSearch_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(tester), pageable, 1);

        when(userRepository.findByRole("ROLE_TESTER", pageable)).thenReturn(page);

        Page<User> result = userService.findAllTesters(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByRole("ROLE_TESTER", pageable);
    }

    @Test
    @DisplayName("findAllTesters: с пустым поиском")
    void findAllTesters_EmptySearch_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(tester), pageable, 1);

        when(userRepository.findByRole("ROLE_TESTER", pageable)).thenReturn(page);

        Page<User> result = userService.findAllTesters("   ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByRole("ROLE_TESTER", pageable);
    }

    @Test
    @DisplayName("getUserById: успешное получение")
    void getUserById_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(creator));

        Optional<User> result = userService.getUserById(2L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getUserById: не найден -> пусто")
    void getUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isAdmin: пользователь с ролью ADMIN -> true")
    void isAdmin_True() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        boolean result = userService.isAdmin("admin");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAdmin: пользователь с другой ролью -> false")
    void isAdmin_False() {
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

        boolean result = userService.isAdmin("creator");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAdmin: пользователь не найден -> false")
    void isAdmin_UserNotFound_ReturnsFalse() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        boolean result = userService.isAdmin("unknown");

        assertThat(result).isFalse();
    }
}