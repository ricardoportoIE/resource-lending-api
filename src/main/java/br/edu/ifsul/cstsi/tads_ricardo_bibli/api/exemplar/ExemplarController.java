package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.exemplar;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.artigo.Artigo;
import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.artigo.ArtigoRepository;
import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.livro.Livro;
import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.livro.LivroRepository;
import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.periodico.Periodico;
import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.periodico.PeriodicoRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("api/v1/exemplares")
public class ExemplarController {

  private final ExemplarRepository exemplarRepository;
  private final LivroRepository livroRepository;
  private final ArtigoRepository artigoRepository;
  private final PeriodicoRepository periodicoRepository;

  public ExemplarController(
      ExemplarRepository exemplarRepository,
      LivroRepository livroRepository,
      ArtigoRepository artigoRepository,
      PeriodicoRepository periodicoRepository) {
    this.exemplarRepository = exemplarRepository;
    this.livroRepository = livroRepository;
    this.artigoRepository = artigoRepository;
    this.periodicoRepository = periodicoRepository;
  }

  @GetMapping
  public ResponseEntity<List<ExemplarDto>> getExemplares() {
    var exemplares = exemplarRepository.findAll();
    return ResponseEntity.ok().body(exemplares.stream().map(ExemplarDto::new).toList());
  }

  @GetMapping("{codigo}")
  public ResponseEntity<?> getExemplarById(@PathVariable("codigo") Long codigo) {
    var exemplar = exemplarRepository.findById(codigo);

    if (exemplar.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Exemplar com código " + codigo + " não encontrado.");
    }

    return ResponseEntity.ok(new ExemplarDto(exemplar.get()));
  }

  @GetMapping("tipo/{tipo}")
  public ResponseEntity<List<ExemplarDto>> getExemplaresByTipo(@PathVariable String tipo) {
    List<Exemplar> todos = exemplarRepository.findAll();

    List<ExemplarDto> filtrados =
        todos.stream()
            .filter(e -> e.getClass().getSimpleName().equalsIgnoreCase(tipo))
            .map(ExemplarDto::new)
            .toList();

    return ResponseEntity.ok(filtrados);
  }

  // PATCH
  @PatchMapping("/{codigo}")
  public ResponseEntity<ExemplarDto> updateParcial(
      @PathVariable Long codigo, @RequestBody @Valid ExemplarPatchDto dto) {
    return exemplarRepository
        .findById(codigo)
        .map(
            exemplar -> {
              if (dto.nome() != null) exemplar.setNome(dto.nome());

              // Atualiza campos específicos baseado no tipo de exemplar
              if (exemplar instanceof Livro livro) {
                if (dto.autor() != null) livro.setAutor(dto.autor());
                if (dto.editora() != null) livro.setEditora(dto.editora());
                if (dto.edicao() != null) livro.setEdicao(dto.edicao());
                livroRepository.save(livro);
              } else if (exemplar instanceof Artigo artigo) {
                if (dto.autor() != null) artigo.setAutor(dto.autor());
                artigoRepository.save(artigo);
              } else if (exemplar instanceof Periodico periodico) {
                if (dto.editora() != null) periodico.setEditora(dto.editora());
                periodicoRepository.save(periodico);
              } else {
                exemplarRepository.save(exemplar);
              }

              return ResponseEntity.ok(new ExemplarDto(exemplar));
            })
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Exemplar com código " + codigo + " não encontrado."));
  }

  // POST
  @PostMapping
  public ResponseEntity<ExemplarDto> create(@Valid @RequestBody ExemplarPostDto dto) {
    Exemplar novoExemplar;

    if ("LIVRO".equalsIgnoreCase(dto.tipo())) {
      Livro livro = new Livro();
      if (dto.autor() != null) livro.setAutor(dto.autor());
      if (dto.editora() != null) livro.setEditora(dto.editora());
      if (dto.edicao() != null) livro.setEdicao(dto.edicao());
      novoExemplar = livro;
    } else if ("ARTIGO".equalsIgnoreCase(dto.tipo())) {
      Artigo artigo = new Artigo();
      if (dto.autor() != null) artigo.setAutor(dto.autor());
      novoExemplar = artigo;
    } else if ("PERIODICO".equalsIgnoreCase(dto.tipo())) {
      Periodico periodico = new Periodico();
      if (dto.editora() != null) periodico.setEditora(dto.editora());
      novoExemplar = periodico;
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de exemplar inválido");
    }

    novoExemplar.setNome(dto.nome());

    Exemplar saved;
    if (novoExemplar instanceof Livro) {
      saved = livroRepository.save((Livro) novoExemplar);
    } else if (novoExemplar instanceof Artigo) {
      saved = artigoRepository.save((Artigo) novoExemplar);
    } else {
      saved = periodicoRepository.save((Periodico) novoExemplar);
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(new ExemplarDto(saved));
  }

  // DELETE
  @DeleteMapping("/{codigo}")
  public ResponseEntity<?> delete(@PathVariable Long codigo) {
    return exemplarRepository
        .findById(codigo)
        .map(
            exemplar -> {
              exemplarRepository.delete(exemplar);
              return ResponseEntity.noContent().build();
            })
        .orElse(
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Exemplar com código " + codigo + " não encontrado."));
  }
}
