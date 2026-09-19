package com.ricardoporto.lending.report;

import com.ricardoporto.lending.loan.LoanStatus;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CsvReportService {
  private final OperationalReportService reports;

  public CsvReportService(OperationalReportService reports) {
    this.reports = reports;
  }

  public String export(ReportType report, LoanStatus loanStatus, int limit) {
    return switch (report) {
      case LOANS -> loans(reports.loans(loanStatus));
      case RESOURCE_UTILIZATION -> utilization(reports.resourceUtilization());
      case POPULAR_RESOURCES -> popular(reports.popularResources(limit));
      case UNAVAILABLE_ITEMS -> unavailable(reports.unavailableItems());
    };
  }

  private String loans(List<LoanReportRow> rows) {
    var csv =
        new StringBuilder(
            "loan_id,status,borrower_email,resource_name,asset_tag,requested_at,due_at,returned_at\n");
    rows.forEach(
        row ->
            line(
                csv,
                row.loanId(),
                row.status(),
                row.borrowerEmail(),
                row.resourceName(),
                row.assetTag(),
                row.requestedAt(),
                row.dueAt(),
                row.returnedAt()));
    return csv.toString();
  }

  private String utilization(List<ResourceUtilizationRow> rows) {
    var csv =
        new StringBuilder(
            "resource_id,resource_name,total_items,on_loan_items,unavailable_items,utilization_percent\n");
    rows.forEach(
        row ->
            line(
                csv,
                row.resourceId(),
                row.resourceName(),
                row.totalItems(),
                row.onLoanItems(),
                row.unavailableItems(),
                row.currentUtilizationPercent()));
    return csv.toString();
  }

  private String popular(List<PopularResourceRow> rows) {
    var csv =
        new StringBuilder(
            "resource_id,resource_name,loan_requests,reservation_requests,total_requests\n");
    rows.forEach(
        row ->
            line(
                csv,
                row.resourceId(),
                row.resourceName(),
                row.loanRequests(),
                row.reservationRequests(),
                row.totalRequests()));
    return csv.toString();
  }

  private String unavailable(List<UnavailableItemRow> rows) {
    var csv = new StringBuilder("item_id,asset_tag,resource_id,resource_name,status,updated_at\n");
    rows.forEach(
        row ->
            line(
                csv,
                row.itemId(),
                row.assetTag(),
                row.resourceId(),
                row.resourceName(),
                row.status(),
                row.updatedAt()));
    return csv.toString();
  }

  private void line(StringBuilder csv, Object... values) {
    for (int index = 0; index < values.length; index++) {
      if (index > 0) csv.append(',');
      csv.append(escape(values[index]));
    }
    csv.append('\n');
  }

  private String escape(Object value) {
    if (value == null) return "";
    var text = String.valueOf(value);
    if (!text.contains(",") && !text.contains("\"") && !text.contains("\n")) return text;
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }
}
