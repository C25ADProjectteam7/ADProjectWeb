package com.expensehub.webbackend.service.impl;

import com.expensehub.webbackend.entity.Reimbursement;
import com.expensehub.webbackend.entity.ReimbursementStatus;
import com.expensehub.webbackend.repository.ReimbursementRepository;
import com.expensehub.webbackend.service.ReimbursementService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReimbursementServiceImpl implements ReimbursementService {

    private final ReimbursementRepository reimbursementRepository;

    public ReimbursementServiceImpl(
            ReimbursementRepository reimbursementRepository) {
        this.reimbursementRepository = reimbursementRepository;
    }

    @Override
    public List<Reimbursement> listReimbursements() {
        return reimbursementRepository.findAll();
    }

    @Override
    public Reimbursement createReimbursement(
            Reimbursement reimbursement) {

        reimbursement.setStatus(ReimbursementStatus.PENDING);
        reimbursement.setSubmittedAt(LocalDateTime.now());

        return reimbursementRepository.save(reimbursement);
    }
    
    @Override
    public Reimbursement updateReimbursement(
            Long id,
            Reimbursement updated) {

        Reimbursement reimbursement =
            reimbursementRepository.findById(id)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Reimbursement not found"));

        if (reimbursement.getStatus() != ReimbursementStatus.PENDING) {
            throw new IllegalStateException(
                "Only pending reimbursements can be corrected");
        }

        reimbursement.setEmployeeEmail(updated.getEmployeeEmail());
        reimbursement.setDepartment(updated.getDepartment());
        reimbursement.setAmount(updated.getAmount());
        reimbursement.setDescription(updated.getDescription());

        return reimbursementRepository.save(reimbursement);
    }
    
    @Override
    public Reimbursement approveReimbursement(Long id) {

        Reimbursement reimbursement =
                reimbursementRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Reimbursement not found"));

        reimbursement.setStatus(ReimbursementStatus.APPROVED);
        reimbursement.setReviewedAt(LocalDateTime.now());

        return reimbursementRepository.save(reimbursement);
    }

    @Override
    public Reimbursement rejectReimbursement(Long id) {

        Reimbursement reimbursement =
                reimbursementRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Reimbursement not found"));

        reimbursement.setStatus(ReimbursementStatus.REJECTED);
        reimbursement.setReviewedAt(LocalDateTime.now());

        return reimbursementRepository.save(reimbursement);
    }
}