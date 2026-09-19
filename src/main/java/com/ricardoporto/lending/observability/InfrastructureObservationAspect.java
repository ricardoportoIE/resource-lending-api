package com.ricardoporto.lending.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class InfrastructureObservationAspect {
  private final ObservationRegistry registry;

  public InfrastructureObservationAspect(ObservationRegistry registry) {
    this.registry = registry;
  }

  @Around("execution(* com.ricardoporto.lending..*Repository.*(..))")
  public Object observeRepository(ProceedingJoinPoint joinPoint) throws Throwable {
    return observe("db.repository", joinPoint);
  }

  @Around("execution(* com.ricardoporto.lending.report.OperationalReportService.*(..))")
  public Object observeReportQuery(ProceedingJoinPoint joinPoint) throws Throwable {
    return observe("db.report.query", joinPoint);
  }

  @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
  public Object observeScheduledJob(ProceedingJoinPoint joinPoint) throws Throwable {
    return observe("job.scheduled", joinPoint);
  }

  private Object observe(String name, ProceedingJoinPoint joinPoint) throws Throwable {
    var observation =
        Observation.createNotStarted(name, registry)
            .lowCardinalityKeyValue("operation", joinPoint.getSignature().getName())
            .lowCardinalityKeyValue(
                "component", joinPoint.getSignature().getDeclaringType().getSimpleName());
    observation.start();
    try (var scope = observation.openScope()) {
      return joinPoint.proceed();
    } catch (Throwable throwable) {
      observation.error(throwable);
      throw throwable;
    } finally {
      observation.stop();
    }
  }
}
