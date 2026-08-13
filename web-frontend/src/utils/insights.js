export function buildInsights({ deptExpenses, alerts, monthlyTrend, categories, approvalOutcomes }) {
  const insights = [];

  const overBudget = deptExpenses
    .filter((d) => d.totalExpense > d.budget)
    .sort((a, b) => (b.totalExpense - b.budget) - (a.totalExpense - a.budget));
  if (overBudget.length > 0) {
    const worst = overBudget[0];
    const pct = Math.round(((worst.totalExpense - worst.budget) / worst.budget) * 100);
    insights.push(
      `${worst.department} is the most over budget, exceeding its S$${worst.budget.toLocaleString()} limit by ${pct}% (S$${(worst.totalExpense - worst.budget).toLocaleString()}).`
    );
  } else {
    insights.push('All departments are currently within their approved budget.');
  }

  if (monthlyTrend.length >= 2) {
    const last = monthlyTrend[monthlyTrend.length - 1];
    const prev = monthlyTrend[monthlyTrend.length - 2];
    const change = ((last.amount - prev.amount) / prev.amount) * 100;
    const direction = change >= 0 ? 'up' : 'down';
    insights.push(
      `Spend in ${last.month} is ${direction} ${Math.abs(Math.round(change))}% from the previous month (S$${prev.amount.toLocaleString()} → S$${last.amount.toLocaleString()}).`
    );
  }

  if (categories.length > 0) {
    const total = categories.reduce((sum, c) => sum + c.amount, 0);
    const top = [...categories].sort((a, b) => b.amount - a.amount)[0];
    const share = Math.round((top.amount / total) * 100);
    insights.push(`${top.category} is the largest expense category, making up ${share}% of approved spend.`);
  }

  if (approvalOutcomes) {
    const { approved, rejected } = approvalOutcomes;
    const decided = approved + rejected;
    if (decided > 0) {
      const rejectRate = Math.round((rejected / decided) * 100);
      if (rejectRate >= 25) {
        insights.push(`${rejectRate}% of decided requests have been rejected — worth reviewing whether budget limits need adjusting.`);
      }
    }
  }

  return insights;
}