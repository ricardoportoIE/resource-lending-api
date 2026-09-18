package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.paidealuno;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.cliente.Cliente;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("PAI_DE_ALUNO")
public class PaiDeAluno extends Cliente {}
