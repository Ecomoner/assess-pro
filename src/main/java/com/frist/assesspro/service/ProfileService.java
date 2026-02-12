package com.frist.assesspro.service;

import com.frist.assesspro.dto.profile.ProfileCompletionDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;

    /**
     * Заполнение профиля пользователя (ФИО)
     */
    @Transactional
    public User completeProfile(String username, ProfileCompletionDTO profileDTO) {
        log.info("Заполнение профиля пользователем: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Валидация ФИО
        validateName(profileDTO.getFirstName(), "Имя");
        validateName(profileDTO.getLastName(), "Фамилия");

        // Отчество может быть пустым
        if (profileDTO.getMiddleName() != null && !profileDTO.getMiddleName().trim().isEmpty()) {
            validateName(profileDTO.getMiddleName(), "Отчество");
        }

        user.setFirstName(profileDTO.getFirstName().trim());
        user.setLastName(profileDTO.getLastName().trim());
        user.setMiddleName(profileDTO.getMiddleName() != null ? profileDTO.getMiddleName().trim() : null);
        user.setIsProfileComplete(true);

        User savedUser = userRepository.save(user);
        log.info("Профиль пользователя {} успешно заполнен", username);

        return savedUser;
    }

    /**
     * Проверка, заполнен ли профиль пользователя
     */
    @Transactional(readOnly = true)
    public boolean isProfileComplete(String username) {
        return userRepository.findByUsername(username)
                .map(User::isProfileComplete)
                .orElse(false);
    }

    /**
     * Получение профиля пользователя
     */
    @Transactional(readOnly = true)
    public ProfileCompletionDTO getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        ProfileCompletionDTO dto = new ProfileCompletionDTO();
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setMiddleName(user.getMiddleName());

        return dto;
    }

    /**
     * Валидация имени/фамилии/отчества
     */
    private void validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым");
        }

        String trimmed = name.trim();
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException(fieldName + " должно содержать минимум 2 символа");
        }

        if (trimmed.length() > 100) {
            throw new IllegalArgumentException(fieldName + " не должно превышать 100 символов");
        }

        // Проверка на допустимые символы (буквы, дефис, пробел)
        if (!trimmed.matches("^[a-zA-Zа-яА-ЯёЁ\\-\\s]+$")) {
            throw new IllegalArgumentException(fieldName + " может содержать только буквы, дефис и пробелы");
        }
    }
}
