package com.expensehub.webbackend.service.impl;

import com.expensehub.webbackend.dto.AuditEntryResponse;
import com.expensehub.webbackend.dto.BudgetConfigRequest;
import com.expensehub.webbackend.dto.BudgetConfigResponse;
import com.expensehub.webbackend.dto.ReimbursementResponse;
import com.expensehub.webbackend.dto.ReimbursementReviewRequest;
import com.expensehub.webbackend.entity.BudgetAuditLog;
import com.expensehub.webbackend.entity.BudgetConfig;
import com.expensehub.webbackend.entity.BudgetPeriodType;
import com.expensehub.webbackend.entity.ReimbursementAuditLog;
import com.expensehub.webbackend.entity.ReimbursementCategory;
import com.expensehub.webbackend.entity.ReimbursementStatus;
import com.expensehub.webbackend.exception.ResourceNotFoundException;
import com.expensehub.webbackend.integration.mobile.MobileApiException;
import com.expensehub.webbackend.integration.mobile.MobileExpenseClient;
import com.expensehub.webbackend.integration.mobile.MobileExpenseDTO;
import com.expensehub.webbackend.integration.mobile.MobileUserDTO;
import com.expensehub.webbackend.repository.BudgetAuditLogRepository;
import com.expensehub.webbackend.repository.BudgetConfigRepository;
import com.expensehub.webbackend.repository.ReimbursementAuditLogRepository;
import com.expensehub.webbackend.service.BudgetPeriodResolver;
import com.expensehub.webbackend.service.FinanceService;
import com.expensehub.webbackend.service.ExpenseApprovalWorkflowService;
import com.expensehub.webbackend.service.ReimbursementPolicyEngine;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceServiceImpl implements FinanceService {

    private static final Logger log = LoggerFactory.getLogger(FinanceServiceImpl.class);

    private final BudgetConfigRepository budgetConfigRepository;
    private final BudgetAuditLogRepository budgetAuditLogRepository;
    private final ReimbursementAuditLogRepository reimbursementAuditLogRepository;
    private final ReimbursementPolicyEngine policyEngine;
    private final ReimbursementExcelExporter excelExporter;
    private final MobileExpenseClient mobileExpenseClient;
    private final ExpenseApprovalWorkflowService workflowService;

    public FinanceServiceImpl(
            BudgetConfigRepository budgetConfigRepository,
            BudgetAuditLogRepository budgetAuditLogRepository,
            ReimbursementAuditLogRepository reimbursementAuditLogRepository,
            ReimbursementPolicyEngine policyEngine,
            ReimbursementExcelExporter excelExporter,
            MobileExpenseClient mobileExpenseClient,
            ExpenseApprovalWorkflowService workflowService) {
        this.budgetConfigRepository = budgetConfigRepository;
        this.budgetAuditLogRepository = budgetAuditLogRepository;
        this.reimbursementAuditLogRepository = reimbursementAuditLogRepository;
        this.policyEngine = policyEngine;
        this.excelExporter = excelExporter;
        this.mobileExpenseClient = mobileExpenseClient;
        this.workflowService = workflowService;
    }

    //budget allocation

    @Override
    public List<BudgetConfigResponse> listBudgets(String department) {
        List<BudgetConfig> configs =
                (department == null || department.isBlank())
                        ? budgetConfigRepository.findAllByOrderByDepartmentAscPeriodLabelAsc()
                        : budgetConfigRepository.findByDepartment(department);
        List<ResolvedExpense> all = fetchResolvedExpenses();
        return configs.stream().map(c -> toBudgetResponse(c, all)).toList();
    }

    @Override
    @Transactional
    public BudgetConfigResponse upsertBudget(BudgetConfigRequest request, String actor) {
        Optional<BudgetConfig> existing =
                budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                        request.department(), request.periodType(), request.periodLabel());

        BigDecimal previousAmount = existing.map(BudgetConfig::getAmount).orElse(null);

        BudgetConfig config =
                existing.orElseGet(
                        () ->
                                BudgetConfig.builder()
                                        .department(request.department())
                                        .periodType(request.periodType())
                                        .periodLabel(request.periodLabel())
                                        .build());
        config.setAmount(request.amount());
        config.setUpdatedBy(actor);
        config.setUpdatedAt(Instant.now());
        config = budgetConfigRepository.save(config);

        budgetAuditLogRepository.save(
                BudgetAuditLog.builder()
                        .budgetId(config.getId())
                        .department(config.getDepartment())
                        .periodLabel(config.getPeriodLabel())
                        .previousAmount(previousAmount)
                        .newAmount(request.amount())
                        .changedBy(actor)
                        .changedAt(Instant.now())
                        .build());

        return toBudgetResponse(config, fetchResolvedExpenses());
    }

    @Override
    public List<AuditEntryResponse> getBudgetAudit(Long budgetId) {
        return budgetAuditLogRepository.findByBudgetIdOrderByChangedAtDesc(budgetId).stream()
                .map(
                        entry ->
                                new AuditEntryResponse(
                                        entry.getId(),
                                        "BUDGET_UPDATE",
                                        describeBudgetChange(entry),
                                        entry.getChangedBy(),
                                        entry.getChangedAt()))
                .toList();
    }

    private String describeBudgetChange(BudgetAuditLog entry) {
        String previous =
                entry.getPreviousAmount() == null ? "(new)" : entry.getPreviousAmount().toString();
        return entry.getDepartment()
                + " / "
                + entry.getPeriodLabel()
                + ": "
                + previous
                + " -> "
                + entry.getNewAmount();
    }

    private BudgetConfigResponse toBudgetResponse(BudgetConfig config, List<ResolvedExpense> all) {
        LocalDate anchor = anchorDateForPeriod(config);
        LocalDate from = BudgetPeriodResolver.periodStart(config.getPeriodType(), anchor);
        LocalDate to = BudgetPeriodResolver.periodEnd(config.getPeriodType(), anchor);
        BigDecimal spent = sumActiveAmount(all, config.getDepartment(), from, to);
        BigDecimal remaining = config.getAmount().subtract(spent);
        return new BudgetConfigResponse(
                config.getId(),
                config.getDepartment(),
                config.getPeriodType(),
                config.getPeriodLabel(),
                config.getAmount(),
                spent,
                remaining,
                spent.compareTo(config.getAmount()) > 0,
                config.getUpdatedBy(),
                config.getUpdatedAt());
    }

    private LocalDate anchorDateForPeriod(BudgetConfig config) {
        if (config.getPeriodType() == BudgetPeriodType.ANNUAL) {
            return LocalDate.of(Integer.parseInt(config.getPeriodLabel()), 1, 1);
        }
        String[] parts = config.getPeriodLabel().split("-Q");
        int year = Integer.parseInt(parts[0]);
        int quarter = Integer.parseInt(parts[1]);
        int month = (quarter - 1) * 3 + 1;
        return LocalDate.of(year, month, 1);
    }

    private BigDecimal sumActiveAmount(
            List<ResolvedExpense> all, String department, LocalDate from, LocalDate to) {
        return all.stream()
                .filter(r -> department.equalsIgnoreCase(r.department()))
                .filter(r -> r.status() != ReimbursementStatus.REJECTED)
                .filter(r -> r.submittedAt() != null)
                .filter(r -> withinRange(r.submittedAt().toLocalDate(), from, to))
                .map(ResolvedExpense::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean withinRange(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }


    @Override
    public Page<ReimbursementResponse> listReimbursements(
            ReimbursementStatus status,
            String department,
            ReimbursementCategory category,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        List<ResolvedExpense> all = fetchResolvedExpenses();
        List<ReimbursementResponse> filtered =
                filterAndSort(all, status, department, category, from, to).stream()
                        .filter(r -> workflowService.isReadyForFinance(r.id()))
                        .map(r -> toResponse(r, all))
                        .toList();

        int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        int fromIndex = Math.min(pageNumber * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return new PageImpl<>(filtered.subList(fromIndex, toIndex), pageable, filtered.size());
    }

    @Override
    public ReimbursementResponse getReimbursement(Long id) {
        List<ResolvedExpense> all = fetchResolvedExpenses();
        ResolvedExpense match =
                all.stream()
                        .filter(r -> r.id().equals(id))
                        .findFirst()
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Reimbursement request not found: " + id));
        return toResponse(match, all);
    }

    @Override
    @Transactional
    public ReimbursementResponse reviewReimbursement(
            Long id, ReimbursementReviewRequest request, String actor) {
        // Ensure the expense is ready for finance review
        if (!workflowService.isReadyForFinance(id)) {
            throw new IllegalStateException(
                "Expense " + id + " requires manager approval before finance can review it.");
        }
        MobileExpenseDTO updated =
                switch (request.decision()) {
                    case APPROVE -> mobileExpenseClient.approve(id, request.comment());
                    case REJECT -> mobileExpenseClient.reject(id, request.comment());
                    case REQUEST_INFO -> mobileExpenseClient.requestInfo(id, request.comment());
                };

        reimbursementAuditLogRepository.save(
                ReimbursementAuditLog.builder()
                        .requestId(id)
                        .action("REVIEW_" + request.decision())
                        .detail(
                                "status -> "
                                        + updated.getStatus()
                                        + (request.comment() == null || request.comment().isBlank()
                                                ? ""
                                                : "; comment: " + request.comment()))
                        .changedBy(actor)
                        .changedAt(Instant.now())
                        .build());

        List<ResolvedExpense> all = fetchResolvedExpenses();
        ResolvedExpense match =
                all.stream()
                        .filter(r -> r.id().equals(id))
                        .findFirst()
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Reimbursement request not found: " + id));
        return toResponse(match, all);
    }

    @Override
    public List<AuditEntryResponse> getReimbursementAudit(Long id) {
        return reimbursementAuditLogRepository.findByRequestIdOrderByChangedAtDesc(id).stream()
                .map(
                        entry ->
                                new AuditEntryResponse(
                                        entry.getId(),
                                        entry.getAction(),
                                        entry.getDetail(),
                                        entry.getChangedBy(),
                                        entry.getChangedAt()))
                .toList();
    }

    // Export to Excel (Item 18)

    @Override
    public byte[] exportReimbursementsToExcel(
            ReimbursementStatus status,
            String department,
            ReimbursementCategory category,
            LocalDate from,
            LocalDate to) {
        List<ResolvedExpense> all = fetchResolvedExpenses();
        List<ReimbursementResponse> records =
                filterAndSort(all, status, department, category, from, to).stream()
                        .filter(r -> workflowService.isReadyForFinance(r.id()))
                        .map(r -> toResponse(r, all))
                        .toList();
        return excelExporter.export(records);
    }

    private List<ResolvedExpense> fetchResolvedExpenses() {
        List<MobileExpenseDTO> raw = mobileExpenseClient.listAllExpenses();
        Map<Long, MobileUserDTO> userCache = new HashMap<>();
        List<ResolvedExpense> resolved = new ArrayList<>(raw.size());
        Map<Long, MobileUserDTO> userByExpenseId = new HashMap<>(raw.size());

        // Pass 1: resolve the employee (and therefore department) behind every
        // expense. No workflow writes here — the over-budget decision depends on
        // the department's TOTAL spend for the period, which isn't known until
        // every expense has been resolved.
        for (MobileExpenseDTO e : raw) {
            MobileUserDTO user = resolveUser(userCache, e.getUserId());
            userByExpenseId.put(e.getId(), user);
            resolved.add(
                    new ResolvedExpense(
                            e.getId(),
                            e.getUserId(),
                            user != null ? user.getUsername() : ("user#" + e.getUserId()),
                            user != null ? user.getDepartment() : null,
                            ReimbursementCategory.valueOf(e.getCategory()),
                            e.getAmount(),
                            e.getCurrency(),
                            e.getDescription(),
                            e.getReceiptUrl(),
                            e.getReceiptUrl() != null && !e.getReceiptUrl().isBlank(),
                            ReimbursementStatus.valueOf(e.getStatus()),
                            e.getSubmittedAt(),
                            e.getApprovalOpinion(),
                            e.getApproverName()));
        }

        refreshWorkflowState(raw, resolved, userByExpenseId);
        return resolved;
    }

    /**
     * Pass 2: bring each expense's manager-approval workflow row up to date.
     * <p>
     * Split out so the write can be moved off the read path (e.g. onto a
     * scheduled reconciliation job) without touching the resolution logic above.
     */
    private void refreshWorkflowState(
            List<MobileExpenseDTO> raw,
            List<ResolvedExpense> resolved,
            Map<Long, MobileUserDTO> userByExpenseId) {
        for (MobileExpenseDTO e : raw) {
            MobileUserDTO user = userByExpenseId.get(e.getId());
            if (user != null) {
                workflowService.updateWorkflowForExpense(
                        e.getId(), e, user, departmentPeriodSpend(resolved, user, e));
            }
        }
    }

    /**
     * Total non-rejected spend for this expense's department over the budget
     * period the expense falls in — the same figure {@link #computeFlags} feeds
     * the policy engine, so the manager-routing decision and the OVER_BUDGET
     * flag shown to finance are computed from identical inputs.
     *
     * @return null when there is no department or no configured budget, meaning
     *     "no limit to compare against" rather than a limit of zero.
     */
    private BigDecimal departmentPeriodSpend(
            List<ResolvedExpense> all, MobileUserDTO user, MobileExpenseDTO expense) {
        String department = user != null ? user.getDepartment() : null;
        if (department == null) {
            return null;
        }
        LocalDate anchor =
                expense.getSubmittedAt() != null
                        ? expense.getSubmittedAt().toLocalDate()
                        : LocalDate.now();
        Optional<BudgetConfig> budget = resolveBudgetFor(department, anchor);
        if (budget.isEmpty()) {
            return null;
        }
        BudgetConfig cfg = budget.get();
        LocalDate from = BudgetPeriodResolver.periodStart(cfg.getPeriodType(), anchor);
        LocalDate to = BudgetPeriodResolver.periodEnd(cfg.getPeriodType(), anchor);
        return sumActiveAmount(all, department, from, to);
    }

    /**
     * Budget lookup for a department at a point in time: quarterly first, then
     * annual. Shared by the flag computation and the workflow refresh so the two
     * can never resolve different budgets for the same expense.
     */
    private Optional<BudgetConfig> resolveBudgetFor(String department, LocalDate anchor) {
        return budgetConfigRepository
                .findByDepartmentAndPeriodTypeAndPeriodLabel(
                        department,
                        BudgetPeriodType.QUARTERLY,
                        BudgetPeriodResolver.quarterlyLabel(anchor))
                .or(
                        () ->
                                budgetConfigRepository.findByDepartmentAndPeriodTypeAndPeriodLabel(
                                        department,
                                        BudgetPeriodType.ANNUAL,
                                        BudgetPeriodResolver.annualLabel(anchor)));
    }

    private MobileUserDTO resolveUser(Map<Long, MobileUserDTO> cache, Long userId) {
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }
        MobileUserDTO user = null;
        try {
            user = mobileExpenseClient.getUser(userId);
        } catch (MobileApiException e) {
            log.warn("Could not resolve department for mobile userId={}: {}", userId, e.getMessage());
        }
        cache.put(userId, user);
        return user;
    }

    private List<ResolvedExpense> filterAndSort(
            List<ResolvedExpense> all,
            ReimbursementStatus status,
            String department,
            ReimbursementCategory category,
            LocalDate from,
            LocalDate to) {
        return all.stream()
                .filter(r -> status == null || r.status() == status)
                .filter(
                        r ->
                                department == null
                                        || department.isBlank()
                                        || (r.department() != null
                                                && r.department().equalsIgnoreCase(department)))
                .filter(r -> category == null || r.category() == category)
                .filter(r -> from == null || (r.submittedAt() != null && !r.submittedAt().toLocalDate().isBefore(from)))
                .filter(r -> to == null || (r.submittedAt() != null && !r.submittedAt().toLocalDate().isAfter(to)))
                .sorted(
                        Comparator.comparing(
                                ResolvedExpense::submittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<String> computeFlags(ResolvedExpense target, List<ResolvedExpense> all) {
        if (target.department() == null) {
            return policyEngine.evaluate(
                    target.amount(), target.category(), target.receiptAttached(), Optional.empty(), target.amount());
        }

        LocalDate anchor =
                target.submittedAt() != null ? target.submittedAt().toLocalDate() : LocalDate.now();

        Optional<BudgetConfig> budget = resolveBudgetFor(target.department(), anchor);

        BigDecimal periodSpent = target.amount();
        if (budget.isPresent()) {
            BudgetConfig cfg = budget.get();
            LocalDate from = BudgetPeriodResolver.periodStart(cfg.getPeriodType(), anchor);
            LocalDate to = BudgetPeriodResolver.periodEnd(cfg.getPeriodType(), anchor);
            periodSpent = sumActiveAmount(all, target.department(), from, to);
        }

        return policyEngine.evaluate(
                target.amount(), target.category(), target.receiptAttached(), budget, periodSpent);
    }

    private ReimbursementResponse toResponse(ResolvedExpense r, List<ResolvedExpense> all) {
        return new ReimbursementResponse(
                r.id(),
                r.employeeName(),
                r.userId(),
                r.department(),
                r.category(),
                r.amount(),
                r.currency(),
                r.description(),
                r.receiptAttached(),
                buildFullReceiptUrl(r.receiptUrl()),
                r.status(),
                r.submittedAt(),
                computeFlags(r, all),
                r.approvalOpinion(),
                r.approverName());
    }

    /**
     * Mobile's receiptUrl looks like "/uploads/receipts/2026-08-13/xxx.png".
     * The gateway (see gateway/nginx.conf.template) proxies "/mobile-uploads/"
     * to Mobile's "/uploads/", and since the gateway serves the frontend and
     * this API on the same origin, a relative path is all the browser needs —
     * no internal Docker-network IP/hostname is ever exposed to the client.
     */
    private String buildFullReceiptUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isBlank()) {
            return null;
        }
        String uploadsPath =
                relativeUrl.startsWith("/uploads/")
                        ? relativeUrl.substring("/uploads".length())
                        : relativeUrl;
        return "/mobile-uploads" + uploadsPath;
    }

    private record ResolvedExpense(
            Long id,
            Long userId,
            String employeeName,
            String department,
            ReimbursementCategory category,
            BigDecimal amount,
            String currency,
            String description,
            String receiptUrl,
            boolean receiptAttached,
            ReimbursementStatus status,
            LocalDateTime submittedAt,
            String approvalOpinion,
            String approverName) {}
}
