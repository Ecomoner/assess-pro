package com.frist.assesspro.dto.admin;

import com.frist.assesspro.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserManagementDTO {

    private Long id;

    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;

    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов (оставьте пустым, если не хотите менять)")
    private String password;

    @NotBlank(message = "Имя обязательно")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 100)
    private String lastName;

    @Size(max = 100)
    private String middleName;

    @NotBlank(message = "Роль обязательна")
    @Pattern(regexp = "^(ROLE_ADMIN|ROLE_CREATOR|ROLE_TESTER)$")
    private String role;

    private Boolean isActive;
    private Boolean isProfileComplete;
    private LocalDateTime createdAt;
    private Long testsCreated;      // для CREATOR
    private Long testsPassed;        // для TESTER
    private Double averageScore;     // для TESTER

    /**
     * Преобразование из Entity
     */
    public static UserManagementDTO fromEntity(User user) {
        UserManagementDTO dto = new UserManagementDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setMiddleName(user.getMiddleName());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setIsProfileComplete(user.getIsProfileComplete());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (lastName != null) sb.append(lastName);
        if (firstName != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(firstName);
        }
        if (middleName != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName);
        }
        return sb.length() > 0 ? sb.toString() : username;
    }
}