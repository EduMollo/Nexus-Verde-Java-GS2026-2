package br.com.fiap.nexusverde.auth;

import br.com.fiap.nexusverde.auth.AuthDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthService servico;

    AuthController(AuthService servico) {
        this.servico = servico;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar cidadão e emitir JWT", security = {})
    Sessao cadastrar(@Valid @RequestBody Cadastro dados) {
        return servico.cadastrar(dados);
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar e emitir JWT", security = {})
    Sessao entrar(@Valid @RequestBody Login dados) {
        return servico.entrar(dados);
    }
}
