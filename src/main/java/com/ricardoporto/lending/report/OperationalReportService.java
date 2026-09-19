package com.ricardoporto.lending.report;

import com.ricardoporto.lending.loan.LoanStatus;
import com.ricardoporto.lending.resource.ResourceItemStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OperationalReportService {
  private final JdbcTemplate jdbcTemplate;

  public OperationalReportService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public OperationalSummary summary() {
    var wait = reservationWait();
    return jdbcTemplate.queryForObject(
        """
        SELECT
          COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active_loans,
          COUNT(*) FILTER (WHERE status = 'OVERDUE') AS overdue_loans,
          COUNT(*) FILTER (WHERE status = 'RETURNED') AS returned_loans,
          (SELECT COUNT(*) FROM resource_items WHERE status <> 'AVAILABLE') AS unavailable_items,
          (SELECT COUNT(*) FROM resource_items WHERE status = 'MAINTENANCE') AS maintenance_items
        FROM loans
        """,
        (result, row) ->
            new OperationalSummary(
                result.getLong("active_loans"),
                result.getLong("overdue_loans"),
                result.getLong("returned_loans"),
                result.getLong("unavailable_items"),
                result.getLong("maintenance_items"),
                wait.averageWaitSeconds(),
                wait.sampleSize(),
                Instant.now()));
  }

  public List<LoanReportRow> loans(LoanStatus status) {
    return jdbcTemplate.query(
        """
        SELECT l.id, l.status, u.email, r.name, ri.asset_tag,
               l.requested_at, l.due_at, l.returned_at
        FROM loans l
        JOIN usuarios u ON u.id = l.borrower_id
        JOIN resource_items ri ON ri.id = l.resource_item_id
        JOIN resources r ON r.id = ri.resource_id
        WHERE l.status = ?
        ORDER BY COALESCE(l.due_at, l.requested_at) ASC
        """,
        this::mapLoan,
        status.name());
  }

  public List<ResourceUtilizationRow> resourceUtilization() {
    return jdbcTemplate.query(
        """
        SELECT r.id, r.name,
               COUNT(ri.id) AS total_items,
               COUNT(ri.id) FILTER (WHERE ri.status = 'ON_LOAN') AS on_loan_items,
               COUNT(ri.id) FILTER (WHERE ri.status <> 'AVAILABLE') AS unavailable_items,
               CASE WHEN COUNT(ri.id) = 0 THEN 0
                    ELSE ROUND(100.0 * COUNT(ri.id) FILTER (WHERE ri.status = 'ON_LOAN')
                               / COUNT(ri.id), 2)
               END AS utilization
        FROM resources r
        LEFT JOIN resource_items ri ON ri.resource_id = r.id
        GROUP BY r.id, r.name
        ORDER BY utilization DESC, r.name
        """,
        (result, row) ->
            new ResourceUtilizationRow(
                result.getObject("id", UUID.class),
                result.getString("name"),
                result.getLong("total_items"),
                result.getLong("on_loan_items"),
                result.getLong("unavailable_items"),
                result.getDouble("utilization")));
  }

  public ReservationWaitReport reservationWait() {
    return jdbcTemplate.queryForObject(
        """
        SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (ready_at - created_at))), 0) AS average_wait,
               COUNT(ready_at) AS sample_size
        FROM reservations
        WHERE ready_at IS NOT NULL
        """,
        (result, row) ->
            new ReservationWaitReport(
                result.getDouble("average_wait"), result.getLong("sample_size")));
  }

  public List<PopularResourceRow> popularResources(int limit) {
    return jdbcTemplate.query(
        """
        WITH loan_counts AS (
          SELECT ri.resource_id, COUNT(*) AS count
          FROM loans l JOIN resource_items ri ON ri.id = l.resource_item_id
          GROUP BY ri.resource_id
        ), reservation_counts AS (
          SELECT resource_id, COUNT(*) AS count FROM reservations GROUP BY resource_id
        )
        SELECT r.id, r.name,
               COALESCE(lc.count, 0) AS loan_requests,
               COALESCE(rc.count, 0) AS reservation_requests,
               COALESCE(lc.count, 0) + COALESCE(rc.count, 0) AS total_requests
        FROM resources r
        LEFT JOIN loan_counts lc ON lc.resource_id = r.id
        LEFT JOIN reservation_counts rc ON rc.resource_id = r.id
        ORDER BY total_requests DESC, r.name
        LIMIT ?
        """,
        (result, row) ->
            new PopularResourceRow(
                result.getObject("id", UUID.class),
                result.getString("name"),
                result.getLong("loan_requests"),
                result.getLong("reservation_requests"),
                result.getLong("total_requests")),
        limit);
  }

  public List<UnavailableItemRow> unavailableItems() {
    return jdbcTemplate.query(
        """
        SELECT ri.id, ri.asset_tag, ri.status, ri.updated_at, r.id AS resource_id, r.name
        FROM resource_items ri
        JOIN resources r ON r.id = ri.resource_id
        WHERE ri.status <> 'AVAILABLE'
        ORDER BY ri.status, r.name, ri.asset_tag
        """,
        (result, row) ->
            new UnavailableItemRow(
                result.getObject("id", UUID.class),
                result.getString("asset_tag"),
                result.getObject("resource_id", UUID.class),
                result.getString("name"),
                ResourceItemStatus.valueOf(result.getString("status")),
                result.getTimestamp("updated_at").toInstant()));
  }

  private LoanReportRow mapLoan(ResultSet result, int row) throws SQLException {
    var dueAt = result.getTimestamp("due_at");
    var returnedAt = result.getTimestamp("returned_at");
    return new LoanReportRow(
        result.getObject("id", UUID.class),
        LoanStatus.valueOf(result.getString("status")),
        result.getString("email"),
        result.getString("name"),
        result.getString("asset_tag"),
        result.getTimestamp("requested_at").toInstant(),
        dueAt == null ? null : dueAt.toInstant(),
        returnedAt == null ? null : returnedAt.toInstant());
  }
}
