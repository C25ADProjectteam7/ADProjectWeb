package com.expensehub.webbackend.repository;

import com.expensehub.webbackend.entity.Reimbursement;
import com.expensehub.webbackend.entity.ReimbursementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReimbursementRepository
        extends JpaRepository<Reimbursement, Long> {

    List<Reimbursement> findByStatus(ReimbursementStatus status);

    List<Reimbursement> findByDepartment(String department);

    List<Reimbursement> findByEmployeeEmail(String employeeEmail);
}