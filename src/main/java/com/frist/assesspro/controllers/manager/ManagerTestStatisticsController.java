package com.frist.assesspro.controllers.manager;


import com.frist.assesspro.dto.TestDTO;
import com.frist.assesspro.dto.statistics.TestSummaryDTO;
import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.statistics.TesterDetailedAnswersDTO;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.mapper.TestMapper;
import com.frist.assesspro.service.ManagerService;
import com.frist.assesspro.service.TestService;
import com.frist.assesspro.service.UserService;
import com.frist.assesspro.service.export.AsyncPdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/manager/tests/{testId}/statistics")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerTestStatisticsController {

    private final ManagerService managerService;
    private final TestService testService;
    private final TestMapper testMapper;
    private final AsyncPdfExportService asyncPdfExportService;
    private final UserService userService;

    @GetMapping("/tester/{attemptId}")
    @Transactional(readOnly = true)
    public String getTesterDetailedAnswers(@PathVariable Long testId,
                                           @PathVariable Long attemptId,
                                           Principal principal,
                                           Model model){
        TesterDetailedAnswersDTO detailedAnswers = managerService.getFilteredTesterDetailedAnswers(attemptId, principal.getName());
        TestDTO testDTO = testMapper.toDto(testService.getTestByIdWithoutOwnershipCheck(testId));

        model.addAttribute("test", testDTO);
        model.addAttribute("detailedAnswers",detailedAnswers);

        return "manager/tester-detailed-answers";
    }


    @PostMapping("/export")
    @Transactional
    public ResponseEntity<Map<String, String>> exportTestStatistics(@PathVariable Long testId,
                                                                    @RequestParam(required = false) String testerUsername,
                                                                    Principal principal) {

        Test test = testService.getTestByIdWithoutOwnershipCheck(testId);
        String requestId = UUID.randomUUID().toString();
        asyncPdfExportService.generateManagerTestStatistics(test, principal.getName(), testerUsername, null, requestId);
        return ResponseEntity.ok(Map.of("requestId", requestId, "message", "Отчёт формируется"));
    }

    @GetMapping("/tester/{attemptId}/export")
    @Transactional
    public ResponseEntity<Map<String, String>> exportTesterDetailedAnswers(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            Principal principal) {

        // Получаем DTO с проверкой доступа
        TesterDetailedAnswersDTO dto = managerService.getFilteredTesterDetailedAnswers(attemptId, principal.getName());

        // Для подписи в PDF можно получить полное имя тестировщика
        User tester = userService.findByUsername(dto.getTesterUsername()).orElse(null);
        String testerFullName = tester != null ? tester.getFullName() : dto.getTesterUsername();

        String requestId = UUID.randomUUID().toString();
        asyncPdfExportService.generateTesterAttemptPdf(dto, testerFullName, requestId);

        return ResponseEntity.ok(Map.of(
                "requestId", requestId,
                "message", "Результаты готовятся..."
        ));
    }

}
