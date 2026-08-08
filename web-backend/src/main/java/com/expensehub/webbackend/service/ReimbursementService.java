package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.Reimbursement;

import java.util.List;

public interface ReimbursementService {

    List<Reimbursement> listReimbursements();

    Reimbursement createReimbursement(Reimbursement reimbursement);

    Reimbursement updateReimbursement(Long id, Reimbursement reimbursement);

    Reimbursement approveReimbursement(Long id);

    Reimbursement rejectReimbursement(Long id);
}