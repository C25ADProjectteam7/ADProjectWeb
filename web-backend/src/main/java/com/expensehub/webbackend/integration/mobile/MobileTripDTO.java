package com.expensehub.webbackend.integration.mobile;

import java.math.BigDecimal;

/**
 * 对应 Mobile 组 com.team7.mobile.common.dto.TripDTO，供 Manager 审批模块 (Item 20)
 * 和数据可视化模块 (Item 23-25) 使用。日期字段沿用 Mobile 返回的字符串形式，
 * 由调用方按需 parse。
 */
public class MobileTripDTO {

    private Long id;
    private Long userId;
    private String title;
    private String destination;
    private String startDate;
    private String endDate;
    private BigDecimal budgetTotal;
    private String status;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getBudgetTotal() {
        return budgetTotal;
    }

    public void setBudgetTotal(BigDecimal budgetTotal) {
        this.budgetTotal = budgetTotal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
