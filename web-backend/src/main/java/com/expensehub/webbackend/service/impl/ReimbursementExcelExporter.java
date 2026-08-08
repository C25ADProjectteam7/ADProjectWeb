package com.expensehub.webbackend.service.impl;

import com.expensehub.webbackend.dto.ReimbursementResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

// Corresponding to backlog Item 18: Export reimbursements to Excel
@Component
public class ReimbursementExcelExporter {

    private static final String[] HEADERS = {
        "Reimbursement ID",
        "Employee",
        "Department",
        "Category",
        "Amount",
        "Currency",
        "Description",
        "Receipt Attached",
        "Receipt URL",
        "Status",
        "Policy Flags",
        "Submitted At",
        "Review Comment",
        "Reviewed By",
    };

    public byte[] export(List<ReimbursementResponse> records) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            SXSSFSheet sheet = workbook.createSheet("Reimbursements");

            CellStyle headerStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ReimbursementResponse r : records) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(r.id());
                row.createCell(col++).setCellValue(nullToEmpty(r.employeeName()));
                row.createCell(col++).setCellValue(nullToEmpty(r.department()));
                row.createCell(col++).setCellValue(String.valueOf(r.category()));
                row.createCell(col++).setCellValue(r.amount().doubleValue());
                row.createCell(col++).setCellValue(nullToEmpty(r.currency()));
                row.createCell(col++).setCellValue(nullToEmpty(r.description()));
                row.createCell(col++).setCellValue(r.receiptAttached() ? "Yes" : "No");
                row.createCell(col++).setCellValue(nullToEmpty(r.receiptUrl()));
                row.createCell(col++).setCellValue(String.valueOf(r.status()));
                row.createCell(col++).setCellValue(String.join(", ", r.policyFlags()));
                row.createCell(col++)
                        .setCellValue(r.submittedAt() == null ? "" : r.submittedAt().toString());
                row.createCell(col++).setCellValue(nullToEmpty(r.reviewComment()));
                row.createCell(col).setCellValue(nullToEmpty(r.reviewedBy()));
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.trackColumnForAutoSizing(i);
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose(); // Dispose of temporary files backing this workbook on disk
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate reimbursement export", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
