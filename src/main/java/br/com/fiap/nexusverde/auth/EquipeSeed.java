package br.com.fiap.nexusverde.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class EquipeSeed implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final String email;
    private final String senha;
    private final boolean demonstracao;

    EquipeSeed(UsuarioRepository usuarios, PasswordEncoder senhas,
               @Value("${nexus.equipe-email}") String email, @Value("${nexus.equipe-senha}") String senha,
               @Value("${nexus.demo}") boolean demonstracao) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.email = email;
        this.senha = senha;
        this.demonstracao = demonstracao;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && senha.isBlank()) {
            return;
        }
        int tamanhoMinimo = demonstracao ? 6 : 12;
        if (!email.contains("@") || senha.length() < tamanhoMinimo) {
            throw new IllegalStateException("Configure EQUIPE_EMAIL e uma EQUIPE_SENHA com "
                    + tamanhoMinimo + " caracteres ou mais.");
        }
        AuthService.validarTamanhoSenha(senha);
        String normalizado = AuthService.normalizarEmail(email);
        Usuario atual = usuarios.findByEmail(normalizado).orElse(null);
        if (atual == null) {
            usuarios.save(new Usuario("Equipe Nexus Verde", normalizado, senhas.encode(senha),
                    Papel.EQUIPE, null, null));
        } else if (atual.getPapel() != Papel.EQUIPE) {
            throw new IllegalStateException("EQUIPE_EMAIL já pertence a uma conta de cidadão.");
        } else if (!senhas.matches(senha, atual.senhaHash())) {
            atual.atualizarSenha(senhas.encode(senha));
            usuarios.save(atual);
        }
    }
}
