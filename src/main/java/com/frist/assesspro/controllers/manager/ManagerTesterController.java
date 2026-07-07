package com.frist.assesspro.controllers.manager;


import com.frist.assesspro.dto.statistics.TesterAttemptDTO;
import com.frist.assesspro.dto.statistics.TesterTestStatDTO;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.EventService;
import com.frist.assesspro.service.ManagerService;
import com.frist.assesspro.service.TesterStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('MANAGER')")
public class ManagerTesterController {

    private final ManagerService  managerService;
    private final EventService eventService;
    private final TesterStatisticsService  testerStatisticsService;

    @GetMapping("/tester/{testerUsername}/full-statistics")
    public String showTesterFullStatistics(@PathVariable String testerUsername, Model model,
                                           Principal principal) {

        User tester = managerService.getTesterWithAccessCheck(testerUsername,principal.getName());

        List<TesterAttemptDTO> allAttempt = testerStatisticsService.getAllAttemptsByTester(testerUsername);

        long totalAttempts = allAttempt.size();
        long completedAttempts = allAttempt.stream().filter(a -> a.isCompleted()).count();

        double overallAverage = allAttempt.stream()
                .filter(a -> a.isCompleted())
                        .mapToDouble(TesterAttemptDTO::getPercentage)
                        .average().orElse(0.0);

        Map<Long,List<TesterAttemptDTO>> attemptsByTest = allAttempt.stream()
                .collect(Collectors.groupingBy(TesterAttemptDTO::getTestId));

        List<TesterTestStatDTO> testStatistics = new ArrayList<>();
        for (Map.Entry<Long, List<TesterAttemptDTO>> entry : attemptsByTest.entrySet()) {
            List<TesterAttemptDTO> testAttempts = entry.getValue();
            TesterAttemptDTO first = testAttempts.get(0);
            double avg = testAttempts.stream()
                    .filter(a -> a.getEndTime() != null)
                    .mapToDouble(TesterAttemptDTO::getPercentage)
                    .average().orElse(0.0);
            TesterTestStatDTO stat = new TesterTestStatDTO();
            stat.setTestId(first.getTestId());
            stat.setTestTitle(first.getTestTitle());
            stat.setTotalAttempts(testAttempts.size());
            stat.setAverageScore(avg);
            stat.setAttempts(testAttempts);
            testStatistics.add(stat);
        }
        model.addAttribute("testStatistics", testStatistics);

        model.addAttribute("tester", tester);
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("completedAttempts", completedAttempts);
        model.addAttribute("overallAverage", overallAverage);
        model.addAttribute("testStatistics", testStatistics);

        return "manager/tester-full-statistics";
    }
}
