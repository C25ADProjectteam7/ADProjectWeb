package com.expensehub.webbackend.service;

import com.expensehub.webbackend.entity.BudgetPeriodType;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.IsoFields;

// Utility class for resolving budget period labels and start/end dates based on a given date and period type (annual or quarterly).

public final class BudgetPeriodResolver {

    private BudgetPeriodResolver() {}

    public static String annualLabel(LocalDate date) {
        return String.valueOf(date.getYear());
    }

    public static String quarterlyLabel(LocalDate date) {
        int quarter = date.get(IsoFields.QUARTER_OF_YEAR);
        return date.getYear() + "-Q" + quarter;
    }

    public static LocalDate periodStart(BudgetPeriodType type, LocalDate date) {
        if (type == BudgetPeriodType.ANNUAL) {
            return LocalDate.of(date.getYear(), Month.JANUARY, 1);
        }
        int quarter = date.get(IsoFields.QUARTER_OF_YEAR);
        Month startMonth = Month.of((quarter - 1) * 3 + 1);
        return LocalDate.of(date.getYear(), startMonth, 1);
    }

    public static LocalDate periodEnd(BudgetPeriodType type, LocalDate date) {
        if (type == BudgetPeriodType.ANNUAL) {
            return LocalDate.of(date.getYear(), Month.DECEMBER, 31);
        }
        LocalDate start = periodStart(type, date);
        return start.plusMonths(3).minusDays(1);
    }
}
