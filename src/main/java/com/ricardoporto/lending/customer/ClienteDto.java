package com.ricardoporto.lending.customer;

import java.io.Serializable;

public record ClienteDto(
    Long codigo, String nome, Integer idade, String telefone, String endereco, String tipo)
    implements Serializable {}
