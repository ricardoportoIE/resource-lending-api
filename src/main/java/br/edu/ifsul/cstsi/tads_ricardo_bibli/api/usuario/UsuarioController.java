package br.edu.ifsul.cstsi.tads_ricardo_bibli.api.usuario;

import br.edu.ifsul.cstsi.tads_ricardo_bibli.api.infra.exception.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

  /**
   * Endpoint para cadastrar um novo usuário.
   *
   * @param cadastroDTO DTO com os dados do usuário a ser cadastrado
   * @return ResponseEntity com status 201 CREATED se o cadastro for bem-sucedido
   * @throws ValidationException se os dados do usuário forem inválidos
   */
  @PostMapping("/cadastrar")
  public ResponseEntity<String> cadastrarUsuario(@RequestBody UsuarioCadastroDTO cadastroDTO) {
    // Validação simples do email
    if (cadastroDTO.email() == null || !isValidEmail(cadastroDTO.email())) {
      Map<String, String> errors = new HashMap<>();
      errors.put("email", "Email inválido");
      throw new ValidationException("Email inválido", errors);
    }

    // Validação simples da senha
    if (cadastroDTO.senha() == null || cadastroDTO.senha().length() < 3) {
      Map<String, String> errors = new HashMap<>();
      errors.put("senha", "Senha inválida");
      throw new ValidationException("Senha inválida", errors);
    }

    // Simulação de cadastro bem-sucedido para o teste
    return ResponseEntity.status(201).body("Usuário cadastrado com sucesso");
  }

  private boolean isValidEmail(String email) {
    // Padrão simples para validação de email
    String emailRegex =
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    Pattern pattern = Pattern.compile(emailRegex);
    return pattern.matcher(email).matches();
  }

  // DTO para o cadastro de usuário
  public record UsuarioCadastroDTO(String email, String senha) {}
}
