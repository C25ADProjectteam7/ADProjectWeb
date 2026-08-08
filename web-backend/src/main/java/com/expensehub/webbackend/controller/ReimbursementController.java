package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.entity.Reimbursement;
import com.expensehub.webbackend.service.ReimbursementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/reimbursements")
public class ReimbursementController {

    private final ReimbursementService reimbursementService;

    public ReimbursementController(
            ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    /**
     * 查看所有报销申请
     */
    @GetMapping
    public List<Reimbursement> listReimbursements() {
        return reimbursementService.listReimbursements();
    }

    /**
     * 创建报销申请
     */
    @PostMapping
    public Reimbursement createReimbursement(
            @RequestBody Reimbursement reimbursement) {

        return reimbursementService.createReimbursement(reimbursement);
    }
    
    @PutMapping("/{id}")
    public Reimbursement updateReimbursement(
            @PathVariable Long id,
            @RequestBody Reimbursement reimbursement) {

        return reimbursementService.updateReimbursement(
            id,
            reimbursement);
    }
    
    /**
     * 批准报销申请
     */
    @PatchMapping("/{id}/approve")
    public Reimbursement approveReimbursement(
            @PathVariable Long id) {

        return reimbursementService.approveReimbursement(id);
    }

    /**
     * 拒绝报销申请
     */
    @PatchMapping("/{id}/reject")
    public Reimbursement rejectReimbursement(
            @PathVariable Long id) {

        return reimbursementService.rejectReimbursement(id);
    }
}