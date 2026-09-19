package com.ricardoporto.lending.report;

import com.ricardoporto.lending.loan.LoanStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Operational reports", description = "Staff dashboards and CSV exports")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@Validated
public class OperationalReportController {
  private final OperationalReportService reports;
  private final CsvReportService csvReports;

  public OperationalReportController(
      OperationalReportService reports, CsvReportService csvReports) {
    this.reports = reports;
    this.csvReports = csvReports;
  }

  @GetMapping("/dashboard")
  public OperationalSummary dashboard() {
    return reports.summary();
  }

  @GetMapping("/loans")
  public List<LoanReportRow> loans(@RequestParam(defaultValue = "ACTIVE") LoanStatus status) {
    return reports.loans(status);
  }

  @GetMapping("/resource-utilization")
  public List<ResourceUtilizationRow> resourceUtilization() {
    return reports.resourceUtilization();
  }

  @GetMapping("/reservation-wait")
  public ReservationWaitReport reservationWait() {
    return reports.reservationWait();
  }

  @GetMapping("/popular-resources")
  public List<PopularResourceRow> popularResources(
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
    return reports.popularResources(limit);
  }

  @GetMapping("/unavailable-items")
  public List<UnavailableItemRow> unavailableItems() {
    return reports.unavailableItems();
  }

  @GetMapping(value = "/export.csv", produces = "text/csv")
  @Operation(summary = "Export an operational report as RFC 4180-compatible CSV")
  public ResponseEntity<String> export(
      @RequestParam ReportType report,
      @RequestParam(defaultValue = "ACTIVE") LoanStatus loanStatus,
      @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int limit) {
    var body = csvReports.export(report, loanStatus, limit);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=resource-lending-" + report.name().toLowerCase() + ".csv")
        .body(body);
  }
}
