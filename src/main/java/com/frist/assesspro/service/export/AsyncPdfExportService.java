package com.frist.assesspro.service.export;

import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.statistics.TesterDetailedAnswersDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncPdfExportService {

    private final StatisticsExportService statisticsExportService;
    private final AdminExportService adminExportService;
    private final TesterAttemptPdfService testerAttemptPdfService;
    private final TesterFullStatisticsPdfService testerFullStatisticsPdfService;// новый сервис

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Async
    public void generateTestStatistics(Test test, String testerUsername, Long categoryId, String requestId) {
        log.info("Асинхронная генерация PDF статистики теста {}", test.getId());
        byte[] pdf = statisticsExportService.generateTestStatisticsPDF(test, testerUsername, categoryId);
        storage.put(requestId, pdf);
    }

    @Async
    public void generateAppStatistics(String requestId) {
        log.info("Асинхронная генерация PDF статистики приложения");
        byte[] pdf = adminExportService.generateAppStatisticsPDF();
        storage.put(requestId, pdf);
    }

    @Async
    public void generateUsersList(String role, Boolean active, String requestId) {
        log.info("Асинхронная генерация PDF списка пользователей");
        byte[] pdf = adminExportService.generateUsersListPDF(role, active);
        storage.put(requestId, pdf);
    }

    @Async
    public void generateTesterAttemptPdf(TesterDetailedAnswersDTO dto, String testerFullName, String requestId) {
        log.info("Асинхронная генерация PDF результатов попытки {}", dto.getAttemptId());
        byte[] pdf = testerAttemptPdfService.generateTesterAttemptPdf(dto, testerFullName);
        storage.put(requestId, pdf);
    }

    public byte[] getPdf(String requestId) {
        return storage.remove(requestId);
    }

    @Async
    public void generateTesterFullStatistics(User tester, List<TesterAttemptDTO> attempts, String requestId) {
        log.info("Асинхронная генерация полной статистики тестировщика {}", tester.getUsername());
        byte[] pdf = testerFullStatisticsPdfService.generate(tester, attempts);
        storage.put(requestId, pdf);
    }
}