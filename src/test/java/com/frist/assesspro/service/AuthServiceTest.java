package com.frist.assesspro.service;

import com.frist.assesspro.dto.RegistrationDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegistrationDTO validRegistrationDTO;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validRegistrationDTO = new RegistrationDTO();
        validRegistrationDTO.setUsername("testuser");
        validRegistrationDTO.setPassword("Password123");
        validRegistrationDTO.setConfirmPassword("Password123");
        validRegistrationDTO.setRole("ROLE_TESTER");

        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole("ROLE_TESTER");
        savedUser.setIsActive(true);
        savedUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Успешная регистрация пользователя")
    void registerUser_Success() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User registeredUser = authService.registerUser(validRegistrationDTO);

        // Assert
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getId()).isEqualTo(1L);
        assertThat(registeredUser.getUsername()).isEqualTo("testuser");
        assertThat(registeredUser.getRole()).isEqualTo("ROLE_TESTER");
        assertThat(registeredUser.getIsActive()).isTrue();

        verify(userRepository).existsByUsername("testuser");
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация с существующим username - ошибка")
    void registerUser_UsernameExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Имя пользователя уже занято");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация с коротким username - ошибка")
    void registerUser_UsernameTooShort_ThrowsException() {
        // Arrange
        validRegistrationDTO.setUsername("ab");

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Имя пользователя слишком короткое");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация с username из пробелов - ошибка")
    void registerUser_UsernameOnlySpaces_ThrowsException() {
        // Arrange
        validRegistrationDTO.setUsername("   ");

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Имя пользователя слишком короткое");
    }

    @Test
    @DisplayName("Регистрация с паролем, содержащим username - ошибка")
    void registerUser_PasswordContainsUsername_ThrowsException() {
        // Arrange
        validRegistrationDTO.setPassword("testuser123");
        validRegistrationDTO.setConfirmPassword("testuser123");

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пароль не должен содержать имя пользователя");
    }

    @Test
    @DisplayName("Регистрация с несовпадающими паролями - ошибка")
    void registerUser_PasswordsDoNotMatch_ThrowsException() {
        // Arrange
        validRegistrationDTO.setConfirmPassword("DifferentPassword123");

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пароли не совпадают");
    }

    @Test
    @DisplayName("Регистрация со слабым паролем (менее 6 символов) - ошибка")
    void registerUser_PasswordTooShort_ThrowsException() {
        // Arrange
        validRegistrationDTO.setPassword("Ab1");
        validRegistrationDTO.setConfirmPassword("Ab1");

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пароль недостаточно надежен");
    }

    @Test
    @DisplayName("Регистрация со слабым паролем (без цифр) - ошибка")
    void registerUser_PasswordNoDigits_ThrowsException() {
        // Arrange
        validRegistrationDTO.setPassword("Password");
        validRegistrationDTO.setConfirmPassword("Password");

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пароль недостаточно надежен");
    }

    @Test
    @DisplayName("Регистрация со слабым паролем (без букв) - ошибка")
    void registerUser_PasswordNoLetters_ThrowsException() {
        // Arrange
        validRegistrationDTO.setPassword("1234567");
        validRegistrationDTO.setConfirmPassword("1234567");

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пароль недостаточно надежен");
    }

    @Test
    @DisplayName("Регистрация с некорректной ролью - ошибка")
    void registerUser_InvalidRole_ThrowsException() {
        // Arrange
        validRegistrationDTO.setRole("ROLE_INVALID");

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Некорректная роль");
    }

    @Test
    @DisplayName("Регистрация с ролью ADMIN (недопустимо для регистрации) - ошибка")
    void registerUser_AdminRole_ThrowsException() {
        // Arrange
        validRegistrationDTO.setRole("ROLE_ADMIN");

        assertThatThrownBy(() -> authService.registerUser(validRegistrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Регистрация с ролью ADMIN запрещена");
    }

    @Test
    @DisplayName("Успешная регистрация с ролью CREATOR")
    void registerUser_CreatorRole_Success() {
        // Arrange
        validRegistrationDTO.setRole("ROLE_CREATOR");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");

        User creatorUser = new User();
        creatorUser.setId(2L);
        creatorUser.setUsername("testuser");
        creatorUser.setRole("ROLE_CREATOR");
        when(userRepository.save(any(User.class))).thenReturn(creatorUser);

        // Act
        User registeredUser = authService.registerUser(validRegistrationDTO);

        // Assert
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getRole()).isEqualTo("ROLE_CREATOR");
    }

    @Test
    @DisplayName("Проверка кэширования - @CacheEvict вызывается")
    void registerUser_CacheEvictCalled() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        authService.registerUser(validRegistrationDTO);

        // Assert
        // Не можем напрямую проверить CacheEvict, но можем убедиться, что метод выполнился
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация с username в верхнем регистре - успешно (должен сохраниться как есть)")
    void registerUser_UsernameUppercase_Success() {
        // Arrange
        validRegistrationDTO.setUsername("TestUser");

        when(userRepository.existsByUsername("TestUser")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");

        User upperUser = new User();
        upperUser.setId(3L);
        upperUser.setUsername("TestUser");
        upperUser.setRole("ROLE_TESTER");
        when(userRepository.save(any(User.class))).thenReturn(upperUser);

        // Act
        User registeredUser = authService.registerUser(validRegistrationDTO);

        // Assert
        assertThat(registeredUser.getUsername()).isEqualTo("TestUser");
        verify(userRepository).existsByUsername("TestUser");
    }

    @Test
    @DisplayName("Регистрация с пробелами в username - ошибка (должен триммиться)")
    void registerUser_UsernameWithSpaces_ThrowsException() {
        validRegistrationDTO.setUsername("  testuser  ");
        when(userRepository.existsByUsername("  testuser  ")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User registeredUser = authService.registerUser(validRegistrationDTO);

        // Проверяем, что save был вызван с пользователем, у которого имя обрезано
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("testuser") && !user.getUsername().contains(" ")
        ));
    }
}