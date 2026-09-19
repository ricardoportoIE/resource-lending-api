package com.ricardoporto.lending.resource;

import java.io.Serializable;

public record ExemplarDto(Long codigo, String nome, String tipo) implements Serializable {}
