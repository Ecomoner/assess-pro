package com.frist.assesspro.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Cacheable
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Роль обязательна")
    @Column(nullable = false)
    private String role;

    // Поля для ФИО
    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    @Column(name = "first_name")
    private String firstName;

    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    @Column(name = "last_name")
    private String lastName;

    @Size(max = 100, message = "Отчество не должно превышать 100 символов")
    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "is_profile_complete", nullable = false)
    private Boolean isProfileComplete = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Test> createdTests;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<TestAttempt> testAttempts;

    /**
     * Проверка заполненности профиля
     */
    @Transient
    public boolean isProfileComplete() {
        return isProfileComplete != null && isProfileComplete &&
                firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty();
    }

    /**
     *  Получение полного имени
     */
    @Transient
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (lastName != null && !lastName.isEmpty()) {
            fullName.append(lastName);
        }
        if (firstName != null && !firstName.isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(middleName);
        }
        return fullName.length() > 0 ? fullName.toString() : username;
    }

    /**
     * Получение инициалов
     */
    @Transient
    public String getInitials() {
        StringBuilder initials = new StringBuilder();
        if (lastName != null && !lastName.isEmpty()) {
            initials.append(lastName.charAt(0));
        }
        if (firstName != null && !firstName.isEmpty()) {
            initials.append(firstName.charAt(0));
        }
        if (middleName != null && !middleName.isEmpty()) {
            initials.append(middleName.charAt(0));
        }
        return initials.length() > 0 ? initials.toString().toUpperCase() : username.substring(0, 1).toUpperCase();
    }

    public static class Roles {
        public static final String ADMIN = "ROLE_ADMIN";
        public static final String CREATOR = "ROLE_CREATOR";
        public static final String TESTER = "ROLE_TESTER";

        public static boolean isValidRole(String role) {
            return ADMIN.equals(role) || CREATOR.equals(role) || TESTER.equals(role);
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive != null && isActive;
    }
}
