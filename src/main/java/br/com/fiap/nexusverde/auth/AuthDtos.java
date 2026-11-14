package br.com.fiap.nexusverde.auth;

import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}

    public record Cadastro(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 6, max = 72) String senha,
            @Size(max = 32) String telefone,
            @Size(max = 120) String cidade) {}

    public record Login(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String senha) {}

    public record Perfil(Long id, String nome, String email, Papel papel, String telefone, String cidade) {
        static Perfil de(Usuario usuario) {
            return new Perfil(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel(),
                    usuario.telefone(), usuario.cidade());
        }
    }

    public record Sessao(String token, Perfil usuario) {}
}
