package com.frist.assesspro.service;

import com.frist.assesspro.dto.admin.AppStatisticsDTO;
import com.frist.assesspro.dto.admin.UserManagementDTO;
import com.frist.assesspro.entity.TestAttempt;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private User admin;
    private User creator;
    private User tester;
    private UserManagementDTO userDTO;
    private Page<User> userPage;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(User.Roles.ADMIN);
        admin.setIsActive(true);

        creator = new User();
        creator.setId(2L);
        creator.setUsername("creator");
        creator.setRole(User.Roles.CREATOR);
        creator.setFirstName("Иван");
        creator.setLastName("Петров");
        creator.setIsActive(true);
        creator.setIsProfileComplete(true);
        creator.setCreatedAt(LocalDateTime.now().minusDays(10));

        tester = new User();
        tester.setId(3L);
        tester.setUsername("tester");
        tester.setRole(User.Roles.TESTER);
        tester.setFirstName("Петр");
        tester.setLastName("Иванов");
        tester.setIsActive(true);
        tester.setIsProfileComplete(false);
        tester.setCreatedAt(LocalDateTime.now().minusDays(5));

        userDTO = new UserManagementDTO();
        userDTO.setUsername("newuser");
        userDTO.setPassword("password123");
        userDTO.setRole(User.Roles.TESTER);
        userDTO.setFirstName("Новый");
        userDTO.setLastName("Пользователь");
        userDTO.setMiddleName("Тестович");
        userDTO.setIsActive(true);

        userPage = new PageImpl<>(List.of(admin, creator, tester));
    }

    // ============= ТЕСТЫ УПРАВЛЕНИЯ ПОЛЬЗОВАТЕЛЯМИ =============

    @Disabled
    @Test
    @DisplayName("getAllUsers: успешное получение всех пользователей с фильтрацией")
    void getAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findUsersWithFilters(any(), any(), any(), eq(pageable)))
                .thenReturn(userPage);

        Page<UserManagementDTO> result = adminService.getAllUsers(null, null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("admin");
        verify(userRepository).findUsersWithFilters(null, null, null, pageable);
    }


    @Disabled
    @Test
    @DisplayName("createUser: успешное создание")
    void createUser_Success() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        User created = adminService.createUser(userDTO, "admin");

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getUsername()).isEqualTo("newuser");
        assertThat(created.getPassword()).isEqualTo("encoded");
        assertThat(created.getRole()).isEqualTo(User.Roles.TESTER);
        assertThat(created.getFirstName()).isEqualTo("Новый");
        assertThat(created.getLastName()).isEqualTo("Пользователь");
        assertThat(created.getMiddleName()).isEqualTo("Тестович");
        assertThat(created.getIsProfileComplete()).isTrue();
        assertThat(created.getIsActive()).isTrue();

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser: имя уже существует -> ошибка")
    void createUser_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createUser(userDTO, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Имя пользователя уже занято");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: успешное обновление всех полей")
    void updateUser_Success() {
        User existing = creator;
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));

        UserManagementDTO updateDTO = new UserManagementDTO();
        updateDTO.setFirstName("ОбновленноеИмя");
        updateDTO.setLastName("ОбновленнаяФамилия");
        updateDTO.setMiddleName("ОбновленноеОтчество");
        updateDTO.setRole(User.Roles.ADMIN);
        updateDTO.setIsActive(false);
        updateDTO.setPassword("newpass");

        when(passwordEncoder.encode("newpass")).thenReturn("encodedNew");
        when(userRepository.save(any(User.class))).thenReturn(existing);

        User updated = adminService.updateUser(2L, updateDTO, "admin");

        assertThat(updated.getFirstName()).isEqualTo("ОбновленноеИмя");
        assertThat(updated.getLastName()).isEqualTo("ОбновленнаяФамилия");
        assertThat(updated.getMiddleName()).isEqualTo("ОбновленноеОтчество");
        assertThat(updated.getRole()).isEqualTo(User.Roles.ADMIN);
        assertThat(updated.getIsActive()).isFalse();
        assertThat(updated.getPassword()).isEqualTo("encodedNew");
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("updateUser: частичное обновление (только имя)")
    void updateUser_Partial_Success() {
        User existing = creator;
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));

        UserManagementDTO updateDTO = new UserManagementDTO();
        updateDTO.setFirstName("НовоеИмя");
        // остальные null

        when(userRepository.save(any(User.class))).thenReturn(existing);

        User updated = adminService.updateUser(2L, updateDTO, "admin");

        assertThat(updated.getFirstName()).isEqualTo("НовоеИмя");
        assertThat(updated.getLastName()).isEqualTo("Петров"); // не изменилось
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("updateUser: пользователь не найден -> ошибка")
    void updateUser_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUser(99L, userDTO, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    @Test
    @DisplayName("toggleUserStatus: активация пользователя")
    void toggleUserStatus_Activate_Success() {
        User existing = creator;
        existing.setIsActive(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        User result = adminService.toggleUserStatus(2L, true, "admin");

        assertThat(result.getIsActive()).isTrue();
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("toggleUserStatus: деактивация пользователя")
    void toggleUserStatus_Deactivate_Success() {
        User existing = creator;
        existing.setIsActive(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        User result = adminService.toggleUserStatus(2L, false, "admin");

        assertThat(result.getIsActive()).isFalse();
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("toggleUserStatus: попытка деактивировать самого себя -> ошибка")
    void toggleUserStatus_DeactivateSelf_ThrowsException() {
        User existing = admin;
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> adminService.toggleUserStatus(1L, false, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Нельзя деактивировать собственную учетную запись");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser: успешное удаление")
    void deleteUser_Success() {
        User existing = creator;
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));

        adminService.deleteUser(2L, "admin");

        verify(userRepository).delete(existing);
    }

    @Test
    @DisplayName("deleteUser: попытка удалить самого себя -> ошибка")
    void deleteUser_DeleteSelf_ThrowsException() {
        User existing = admin;
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> adminService.deleteUser(1L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Нельзя удалить собственную учетную запись");

        verify(userRepository, never()).delete(any());
    }



}