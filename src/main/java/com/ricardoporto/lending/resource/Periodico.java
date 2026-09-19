package com.ricardoporto.lending.resource;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Periodico extends Exemplar {

  private String editora;
}
