package com.frist.assesspro.service;

import com.frist.assesspro.dto.profile.ProfileCompletionDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    private User user;
    private ProfileCompletionDTO completionDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFirstName(null);
        user.setLastName(null);
        user.setMiddleName(null);
        user.setIsProfileComplete(false);

        completionDTO = new ProfileCompletionDTO();
        completionDTO.setFirstName("Иван");
        completionDTO.setLastName("Петров");
        completionDTO.setMiddleName("Сергеевич");
    }

    @Test
    @DisplayName("completeProfile: успешное заполнение профиля (с отчеством)")
    void completeProfile_WithMiddleName_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User updatedUser = profileService.completeProfile("testuser", completionDTO);

        assertThat(updatedUser.getFirstName()).isEqualTo("Иван");
        assertThat(updatedUser.getLastName()).isEqualTo("Петров");
        assertThat(updatedUser.getMiddleName()).isEqualTo("Сергеевич");
        assertThat(updatedUser.getIsProfileComplete()).isTrue();

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("completeProfile: успешное заполнение профиля (без отчества)")
    void completeProfile_WithoutMiddleName_Success() {
        completionDTO.setMiddleName(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User updatedUser = profileService.completeProfile("testuser", completionDTO);

        assertThat(updatedUser.getFirstName()).isEqualTo("Иван");
        assertThat(updatedUser.getLastName()).isEqualTo("Петров");
        assertThat(updatedUser.getMiddleName()).isNull();
        assertThat(updatedUser.getIsProfileComplete()).isTrue();
    }

    @Test
    @DisplayName("completeProfile: пустое имя -> ошибка")
    void completeProfile_EmptyFirstName_ThrowsException() {
        completionDTO.setFirstName("   ");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.completeProfile("testuser", completionDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Имя не может быть пустым");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeProfile: пустая фамилия -> ошибка")
    void completeProfile_EmptyLastName_ThrowsException() {
        completionDTO.setLastName("   ");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.completeProfile("testuser", completionDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Фамилия не может быть пустым");
    }

    @Test
    @DisplayName("completeProfile: слишком короткое имя (<2) -> ошибка")
    void completeProfile_FirstNameTooShort_ThrowsException() {
        completionDTO.setFirstName("A");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.completeProfile("testuser", completionDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Имя должно содержать минимум 2 символа");
    }

    @Test
    @DisplayName("completeProfile: слишком длинное имя (>100) -> ошибка")
    void completeProfile_FirstNameTooLong_ThrowsException() {
        completionDTO.setFirstName("A".repeat(101));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.completeProfile("testuser", completionDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Имя не должно превышать 100 символов");
    }

    @Test
    @DisplayName("completeProfile: недопустимые символы в имени (цифры) -> ошибка")
    void completeProfile_FirstNameInvalidChars_ThrowsException() {
        completionDTO.setFirstName("Иван123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.completeProfile("testuser", completionDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("может содержать только буквы, дефис и пробелы");
    }

    @Test
    @DisplayName("completeProfile: пользователь не найден -> ошибка")
    void completeProfile_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.completeProfile("unknown", completionDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    @Test
    @DisplayName("isProfileComplete: профиль заполнен -> true")
    void isProfileComplete_True() {
        user.setFirstName("Иван");
        user.setLastName("Петров");
        user.setIsProfileComplete(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        boolean result = profileService.isProfileComplete("testuser");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isProfileComplete: профиль не заполнен -> false")
    void isProfileComplete_False() {
        user.setIsProfileComplete(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        boolean result = profileService.isProfileComplete("testuser");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isProfileComplete: пользователь не найден -> false")
    void isProfileComplete_UserNotFound_ReturnsFalse() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        boolean result = profileService.isProfileComplete("unknown");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getProfile: успешное получение DTO профиля")
    void getProfile_Success() {
        user.setFirstName("Иван");
        user.setLastName("Петров");
        user.setMiddleName("Сергеевич");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        ProfileCompletionDTO dto = profileService.getProfile("testuser");

        assertThat(dto).isNotNull();
        assertThat(dto.getFirstName()).isEqualTo("Иван");
        assertThat(dto.getLastName()).isEqualTo("Петров");
        assertThat(dto.getMiddleName()).isEqualTo("Сергеевич");
    }

    @Test
    @DisplayName("getProfile: пользователь не найден -> ошибка")
    void getProfile_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile("unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Пользователь не найден");
    }
}