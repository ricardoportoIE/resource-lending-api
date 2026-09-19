package com.ricardoporto.lending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResourceLendingApplication {

  public static void main(String[] args) {
    SpringApplication.run(ResourceLendingApplication.class, args);
  }
}
