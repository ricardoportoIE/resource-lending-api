package com.ricardoporto.lending.outbox;

import com.ricardoporto.lending.loan.LoanRepository;
import com.ricardoporto.lending.loan.LoanStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DueSoonNotificationScheduler {
  private final LoanRepository loanRepository;
  private final OutboxService outboxService;
  private final Duration dueSoonWindow;

  public DueSoonNotificationScheduler(
      LoanRepository loanRepository,
      OutboxService outboxService,
      @Value("${api.notifications.due-soon-window:PT24H}") Duration dueSoonWindow) {
    this.loanRepository = loanRepository;
    this.outboxService = outboxService;
    this.dueSoonWindow = dueSoonWindow;
  }

  @Scheduled(fixedDelayString = "${api.notifications.due-soon-scan-ms:60000}")
  @Transactional
  public void publishDueSoonEvents() {
    var now = Instant.now();
    loanRepository
        .findAllByStatusAndDueAtBetween(LoanStatus.ACTIVE, now, now.plus(dueSoonWindow))
        .forEach(
            loan ->
                outboxService.publish(
                    "LOAN_DUE_SOON",
                    "Loan",
                    loan.getId(),
                    Map.of(
                        "loanId", loan.getId(),
                        "borrowerEmail", loan.getBorrower().getEmail(),
                        "assetTag", loan.getResourceItem().getAssetTag(),
                        "dueAt", loan.getDueAt()),
                    "LOAN_DUE_SOON:" + loan.getId()));
  }
}
