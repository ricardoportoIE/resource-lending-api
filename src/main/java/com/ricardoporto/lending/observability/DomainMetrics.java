package com.ricardoporto.lending.observability;

import com.ricardoporto.lending.loan.LoanRepository;
import com.ricardoporto.lending.loan.LoanStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class DomainMetrics {
  private final Counter loansRequested;
  private final Counter transitionConflicts;
  private final Timer reservationQueueWait;

  public DomainMetrics(MeterRegistry registry, LoanRepository loanRepository) {
    loansRequested =
        Counter.builder("loans.requested")
            .description("Loan requests accepted by the application")
            .register(registry);
    transitionConflicts =
        Counter.builder("loan.transition.conflicts")
            .description("Rejected loan state transitions")
            .register(registry);
    reservationQueueWait =
        Timer.builder("reservation.queue.wait")
            .description("Time between reservation creation and becoming ready")
            .publishPercentileHistogram()
            .register(registry);
    Gauge.builder(
            "loans.overdue",
            loanRepository,
            repository -> repository.countByStatus(LoanStatus.OVERDUE))
        .description("Current number of overdue loans")
        .register(registry);
  }

  public void loanRequested() {
    loansRequested.increment();
  }

  public void loanTransitionConflict() {
    transitionConflicts.increment();
  }

  public void recordReservationQueueWait(Duration wait) {
    reservationQueueWait.record(wait.isNegative() ? Duration.ZERO : wait);
  }
}
