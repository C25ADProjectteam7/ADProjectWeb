package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.repository.UserRepository;
import com.expensehub.webbackend.service.ManagerService;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/** Corresponds to backlog Item 20: Manager Approval Notifications. */
@RestController
@RequestMapping("/api/manager/approvals")
public class ManagerController {

    private final ManagerService managerService;
    private final UserRepository userRepository;

    public ManagerController(ManagerService managerService, UserRepository userRepository) {
        this.managerService = managerService;
        this.userRepository = userRepository;
    }

    @GetMapping("/pending")
    public List<Approval> listPending() {
        return managerService.listPending();
    }

    @GetMapping("/history")
    public List<Approval> listHistory() {
        return managerService.listHistory();
    }

    @PostMapping("/{id}/approve")
    public Approval approve(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return managerService.decide(id, ApprovalStatus.APPROVED, payload.get("note"), currentUserId());
    }

    @PostMapping("/{id}/reject")
    public Approval reject(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return managerService.decide(id, ApprovalStatus.REJECTED, payload.get("note"), currentUserId());
    }

    /**
     * Resolves the acting manager's id from the authenticated JWT identity
     * (email) rather than trusting a client-supplied value — a managerId in
     * the request body would let any authenticated caller attribute a
     * decision to someone else.
     */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository
                .findByEmail(email)
                .map(user -> user.getId())
                .orElse(null);
    }
}
