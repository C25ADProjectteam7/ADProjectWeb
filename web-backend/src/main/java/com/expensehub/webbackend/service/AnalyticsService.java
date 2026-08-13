package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.integration.mobile.MobileExpenseClient;
import com.expensehub.webbackend.integration.mobile.MobileExpenseDTO;
import com.expensehub.webbackend.integration.mobile.MobileTripDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import com.expensehub.webbackend.repository.ApprovalRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final MobileExpenseClient mobileExpenseClient;
    private final ApprovalRepository approvalRepository;
    private final DepartmentalBudgetService departmentalBudgetService;

    public AnalyticsService(MobileExpenseClient mobileExpenseClient,
                             ApprovalRepository approvalRepository,
                             DepartmentalBudgetService departmentalBudgetService) {
        this.mobileExpenseClient = mobileExpenseClient;
        this.approvalRepository = approvalRepository;
        this.departmentalBudgetService = departmentalBudgetService;
    }

    private BigDecimal budgetOrZero(String department) {
        return departmentalBudgetService.getLimit(department).orElse(BigDecimal.ZERO);
    }

    private Map<Long, MobileUserDTO> buildUserCache(List<Long> userIds) {
        Map<Long, MobileUserDTO> cache = new HashMap<>();
        for (Long id : new HashSet<>(userIds)) {
            try {
                cache.put(id, mobileExpenseClient.getUser(id));
            } catch (Exception e) {
                // lookup failed for this id; caller falls back to "Unknown"
            }
        }
        return cache;
    }

    private String departmentOf(Long userId, Map<Long, MobileUserDTO> cache) {
        MobileUserDTO user = cache.get(userId);
        return (user != null && user.getDepartment() != null) ? user.getDepartment() : "Unknown";
    }

    public List<Map<String, Object>> getDepartmentExpenseComparison() {
        List<MobileExpenseDTO> approvedExpenses = mobileExpenseClient.listAllExpenses().stream()
                .filter(e -> "APPROVED".equals(e.getStatus()))
                .toList();

        Map<Long, MobileUserDTO> userCache = buildUserCache(
                approvedExpenses.stream().map(MobileExpenseDTO::getUserId).toList());

        Map<String, BigDecimal> totalsByDept = approvedExpenses.stream()
                .collect(Collectors.groupingBy(
                        e -> departmentOf(e.getUserId(), userCache),
                        Collectors.reducing(BigDecimal.ZERO, MobileExpenseDTO::getAmount, BigDecimal::add)
                ));

        return totalsByDept.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "department", entry.getKey(),
                        "totalExpense", entry.getValue(),
                        "budget", budgetOrZero(entry.getKey())
                ))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getEmployeeTravelFrequency() {
        List<MobileTripDTO> trips = mobileExpenseClient.listAllTrips();

        Map<Long, MobileUserDTO> userCache = buildUserCache(
                trips.stream().map(MobileTripDTO::getUserId).toList());

        Map<Long, Long> tripCountByUser = trips.stream()
                .collect(Collectors.groupingBy(MobileTripDTO::getUserId, Collectors.counting()));

        return tripCountByUser.entrySet().stream()
                .map(entry -> {
                    MobileUserDTO user = userCache.get(entry.getKey());
                    return Map.<String, Object>of(
                            "userId", entry.getKey(),
                            "userName", user != null ? user.getUsername() : "Unknown",
                            "department", (user != null && user.getDepartment() != null) ? user.getDepartment() : "Unknown",
                            "tripCount", entry.getValue()
                    );
                })
                .sorted((a, b) -> ((Long) b.get("tripCount")).compareTo((Long) a.get("tripCount")))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getBudgetOverrunAlerts() {
        BigDecimal nearLimitRatio = BigDecimal.valueOf(0.85);

        return getDepartmentExpenseComparison().stream()
                .filter(row -> {
                    BigDecimal actual = (BigDecimal) row.get("totalExpense");
                    BigDecimal budget = (BigDecimal) row.get("budget");
                    if (budget.compareTo(BigDecimal.ZERO) <= 0) return false;
                    BigDecimal ratio = actual.divide(budget, 4, RoundingMode.HALF_UP);
                    return ratio.compareTo(nearLimitRatio) >= 0;
                })
                .map(row -> {
                    BigDecimal actual = (BigDecimal) row.get("totalExpense");
                    BigDecimal budget = (BigDecimal) row.get("budget");
                    int overPercent = actual.subtract(budget)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(budget, 0, RoundingMode.HALF_UP)
                            .intValue();
                    String level = actual.compareTo(budget) > 0 ? "OVER" : "NEAR";
                    return Map.<String, Object>of(
                            "department", row.get("department"),
                            "budget", budget,
                            "actual", actual,
                            "overPercent", overPercent,
                            "level", level
                    );
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getExpenseCategoryBreakdown() {
        List<MobileExpenseDTO> approvedExpenses = mobileExpenseClient.listAllExpenses().stream()
                .filter(e -> "APPROVED".equals(e.getStatus()))
                .toList();

        Map<String, BigDecimal> totalsByCategory = approvedExpenses.stream()
                .collect(Collectors.groupingBy(
                        MobileExpenseDTO::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, MobileExpenseDTO::getAmount, BigDecimal::add)
                ));

        return totalsByCategory.entrySet().stream()
                .map(entry -> Map.<String, Object>of("category", entry.getKey(), "amount", entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMonthlySpendTrend() {
        List<MobileExpenseDTO> approvedExpenses = mobileExpenseClient.listAllExpenses().stream()
                .filter(e -> "APPROVED".equals(e.getStatus()))
                .toList();

        Map<String, BigDecimal> totalsByMonth = approvedExpenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getSubmittedAt().toString().substring(0, 7),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, MobileExpenseDTO::getAmount, BigDecimal::add)
                ));

        return totalsByMonth.entrySet().stream()
                .map(entry -> Map.<String, Object>of("month", entry.getKey(), "amount", entry.getValue()))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getApprovalOutcomeSummary() {
        List<Approval> all = approvalRepository.findAll();

        long approved = all.stream().filter(a -> a.getStatus() == ApprovalStatus.APPROVED).count();
        long rejected = all.stream().filter(a -> a.getStatus() == ApprovalStatus.REJECTED).count();
        long pending = all.stream().filter(a -> a.getStatus() == ApprovalStatus.PENDING).count();

        double avgHours = all.stream()
                .filter(a -> a.getDecidedAt() != null)
                .mapToLong(a -> Duration.between(a.getSubmittedAt(), a.getDecidedAt()).toHours())
                .average()
                .orElse(0);

        return Map.of(
                "approved", approved,
                "rejected", rejected,
                "pending", pending,
                "avgTurnaroundHours", Math.round(avgHours * 10) / 10.0
        );
    }
}