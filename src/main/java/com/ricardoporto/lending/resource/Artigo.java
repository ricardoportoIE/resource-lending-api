package com.ricardoporto.lending.resource;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Artigo extends Exemplar {

  private String autor;
}
