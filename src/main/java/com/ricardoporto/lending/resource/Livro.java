package com.ricardoporto.lending.resource;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Livro extends Exemplar {

  private String autor;
  private String editora;
  private Integer edicao;
}
