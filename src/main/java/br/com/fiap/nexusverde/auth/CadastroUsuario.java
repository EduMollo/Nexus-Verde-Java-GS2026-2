package br.com.fiap.nexusverde.auth;

import br.com.fiap.nexusverde.auth.AuthDtos.Cadastro;
import jakarta.validation.constraints.*;

record CadastroUsuario(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull Papel papel,
        @NotBlank @Size(min = 6, max = 72) String senha,
        @NotBlank @Size(max = 72) String confirmacao,
        @Size(max = 32) String telefone,
        @Size(max = 120) String cidade) {

    Cadastro dados() {
        return new Cadastro(nome, email, senha, telefone, cidade);
    }
}
