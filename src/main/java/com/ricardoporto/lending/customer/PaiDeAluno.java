package com.ricardoporto.lending.customer;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("PAI_DE_ALUNO")
public class PaiDeAluno extends Cliente {}
