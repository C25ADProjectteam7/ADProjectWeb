package com.expensehub.webbackend.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 端到端验证 backlog Item 16-19：预算配置、报销请求创建/审核/编辑，以及未授权访问被拒绝。
 * 使用与 HealthControllerTest 相同的 H2 内存库 + test profile。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void reimbursementEndpointsRejectUnauthenticatedRequests() throws Exception {
        // 对应 backlog Item 1 验收标准："Unauthorized API calls return 403 Forbidden."
        mockMvc.perform(get("/api/finance/reimbursements")).andExpect(status().isForbidden());
    }

    @Test
    void employeeRoleCannotAccessFinanceEndpoints() throws Exception {
        mockMvc.perform(
                        get("/api/finance/reimbursements")
                                .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeStaffCanConfigureBudgetThenReviewAndEditAReimbursement() throws Exception {
        var financeUser = user("finance@example.com").roles("FINANCE_STAFF");

        // 1) 配置部门季度预算 (Item 16)
        String budgetPayload =
                objectMapper.writeValueAsString(
                        new java.util.HashMap<>() {
                            {
                                put("department", "Engineering");
                                put("periodType", "QUARTERLY");
                                put("periodLabel", "2026-Q1");
                                put("amount", 1000.00);
                            }
                        });

        mockMvc.perform(
                        put("/api/finance/budgets")
                                .with(financeUser)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(budgetPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.amount").value(1000.00));

        // 2) 创建一笔超出 per-diem、缺收据的报销请求，触发自动标记 (Item 17)
        String createPayload =
                objectMapper.writeValueAsString(
                        new java.util.HashMap<>() {
                            {
                                put("employeeName", "Alice");
                                put("department", "Engineering");
                                put("category", "MEALS");
                                put("amount", 200.00);
                                put("currency", "USD");
                                put("expenseDate", "2026-02-15");
                                put("receiptAttached", false);
                                put("description", "Team dinner");
                            }
                        });

        String createResponse =
                mockMvc.perform(
                                post("/api/finance/reimbursements")
                                        .with(financeUser)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createPayload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.policyFlags[0]").exists())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        // 3) 财务人员审核：批准并附评论 (Item 17)
        String reviewPayload =
                objectMapper.writeValueAsString(
                        new java.util.HashMap<>() {
                            {
                                put("decision", "APPROVE");
                                put("comment", "Confirmed with receipt follow-up");
                            }
                        });

        mockMvc.perform(
                        patch("/api/finance/reimbursements/" + id + "/review")
                                .with(financeUser)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reviewPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("finance@example.com"));

        // 4) 财务人员修正金额 (Item 19)，并检查审计记录已生成
        String editPayload =
                objectMapper.writeValueAsString(
                        new java.util.HashMap<>() {
                            {
                                put("department", "Engineering");
                                put("category", "MEALS");
                                put("amount", 60.00);
                                put("currency", "USD");
                                put("expenseDate", "2026-02-15");
                                put("receiptAttached", true);
                                put("description", "Team dinner - corrected");
                            }
                        });

        mockMvc.perform(
                        put("/api/finance/reimbursements/" + id)
                                .with(financeUser)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(editPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(60.00))
                .andExpect(jsonPath("$.policyFlags").isEmpty());

        mockMvc.perform(get("/api/finance/reimbursements/" + id + "/audit").with(financeUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").exists());

        // 5) 导出为 Excel 文件 (Item 18)
        mockMvc.perform(get("/api/finance/reimbursements/export").with(financeUser))
                .andExpect(status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                                .string(
                                        "Content-Disposition",
                                        org.hamcrest.Matchers.containsString(".xlsx")));
    }
}
