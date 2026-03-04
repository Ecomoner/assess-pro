package com.frist.assesspro.service.export;

import com.frist.assesspro.dto.admin.AppStatisticsDTO;
import com.frist.assesspro.dto.admin.UserManagementDTO;
import com.frist.assesspro.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminExportServiceTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminExportService adminExportService;

    @Test
    @DisplayName("generateAppStatisticsPDF: успешная генерация PDF")
    void generateAppStatisticsPDF_Success() {
        AppStatisticsDTO stats = new AppStatisticsDTO();
        stats.setTopCreators(new ArrayList<>()); // пустой список
        stats.setTopTesters(new ArrayList<>());
        stats.setBestTesters(new ArrayList<>());
        // можно добавить тестовые данные, если нужно
        // Например, добавить одного создателя:

        when(adminService.getAppStatistics()).thenReturn(stats);

        byte[] pdfBytes = adminExportService.generateAppStatisticsPDF();

        assertThat(pdfBytes).isNotEmpty();
    }

    @Test
    @DisplayName("generateUsersListPDF: успешная генерация PDF")
    void generateUsersListPDF_Success() {
        UserManagementDTO userDTO = new UserManagementDTO();
        userDTO.setUsername("testuser");
        userDTO.setFirstName("Иван");
        userDTO.setLastName("Петров");
        userDTO.setRole("ROLE_TESTER");
        userDTO.setIsActive(true);
        userDTO.setIsProfileComplete(true);
        userDTO.setCreatedAt(LocalDateTime.now());

        Page<UserManagementDTO> page = new PageImpl<>(List.of(userDTO));
        when(adminService.getAllUsers(eq("ROLE_TESTER"), eq(null), eq(true), any(PageRequest.class)))
                .thenReturn(page);

        byte[] pdfBytes = adminExportService.generateUsersListPDF("ROLE_TESTER", true);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("generateUsersListPDF: пустой список -> PDF не пустой, но с сообщением")
    void generateUsersListPDF_EmptyList() {
        Page<UserManagementDTO> emptyPage = new PageImpl<>(List.of());
        when(adminService.getAllUsers(eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(emptyPage);

        byte[] pdfBytes = adminExportService.generateUsersListPDF(null, null);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }
}