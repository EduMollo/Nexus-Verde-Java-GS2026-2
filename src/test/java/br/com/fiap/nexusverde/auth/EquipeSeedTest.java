package br.com.fiap.nexusverde.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class EquipeSeedTest {
    @Test
    void aceitaSenhaCurtaParaContaPrincipalDaDemonstracao() {
        var usuarios = mock(UsuarioRepository.class);
        var senhas = mock(PasswordEncoder.class);
        when(usuarios.findByEmail("demo@example.com")).thenReturn(Optional.empty());
        when(senhas.encode("Teste1")).thenReturn("hash-de-teste");

        new EquipeSeed(usuarios, senhas, "demo@example.com", "Teste1", true)
                .run(new DefaultApplicationArguments());

        verify(usuarios).save(any(Usuario.class));
    }

    @Test
    void exigeSenhaLongaForaDaDemonstracao() {
        var usuarios = mock(UsuarioRepository.class);
        var senhas = mock(PasswordEncoder.class);

        assertThatThrownBy(() -> new EquipeSeed(usuarios, senhas, "demo@example.com", "Teste1", false)
                .run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(usuarios, senhas);
    }
}
