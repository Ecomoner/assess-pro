package com.frist.assesspro.dto;

import com.frist.assesspro.entity.User;
import com.frist.assesspro.validation.FieldMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@FieldMatch(first = "password", second = "confirmPassword", message = "Пароли не совпадают")
public class RegistrationDTO {

    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
            message = "Имя пользователя может содержать только буквы, цифры и подчеркивания")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 100, message = "Пароль должен содержать от 6 до 100 символов")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).+$",
            message = "Пароль должен содержать хотя бы одну букву и одну цифру")
    private String password;

    @NotBlank(message = "Подтверждение пароля обязательно")
    private String confirmPassword;

    @NotBlank(message = "Роль обязательна")
    @Pattern(regexp = "^(ROLE_TESTER|ROLE_CREATOR)$",
            message = "Некорректная роль. Допустимые значения: ROLE_TESTER, ROLE_CREATOR")
    private String role = "ROLE_TESTER";
}
