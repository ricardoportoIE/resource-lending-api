package com.ricardoporto.lending;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ricardoporto.lending.auth.AutenticacaoController;
import com.ricardoporto.lending.customer.ClienteController;
import com.ricardoporto.lending.loan.EmprestimoController;
import com.ricardoporto.lending.loan.LoanController;
import com.ricardoporto.lending.resource.ExemplarController;
import com.ricardoporto.lending.resource.ResourceController;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureBoundariesTest {

  @Test
  void controllersDependOnlyOnApplicationServices() {
    var controllers =
        List.of(
            AutenticacaoController.class,
            ClienteController.class,
            EmprestimoController.class,
            LoanController.class,
            ExemplarController.class,
            ResourceController.class);

    assertAll(
        controllers.stream()
            .flatMap(controller -> Arrays.stream(controller.getDeclaredFields()))
            .map(
                field ->
                    () ->
                        assertTrue(
                            isService(field),
                            () ->
                                field.getDeclaringClass().getSimpleName()
                                    + " must not depend directly on "
                                    + field.getType().getSimpleName())));
  }

  private boolean isService(Field field) {
    return field.getType().getSimpleName().endsWith("Service");
  }
}
