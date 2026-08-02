package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.entity.Approval;
import com.expensehub.webbackend.entity.ApprovalStatus;
import com.expensehub.webbackend.service.ManagerService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Corresponds to backlog Item 20: Manager Approval Notifications. */
@RestController
@RequestMapping("/api/manager/approvals")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
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
        return managerService.decide(id, ApprovalStatus.APPROVED, payload.get("note"));
    }

    @PostMapping("/{id}/reject")
    public Approval reject(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return managerService.decide(id, ApprovalStatus.REJECTED, payload.get("note"));
    }
}