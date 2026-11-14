package br.com.fiap.nexusverde.solicitacao;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.*;
import br.com.fiap.nexusverde.web.Pagina;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solicitacoes")
class SolicitacaoController {
    private final SolicitacaoService pedidos;

    SolicitacaoController(SolicitacaoService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping
    Pagina<Saida> listar(@AuthenticationPrincipal Jwt jwt,
                        @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pagina) {
        return pedidos.listar(Ator.de(jwt), pagina);
    }

    @GetMapping("/{id}")
    Saida detalhe(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pedidos.detalhe(Ator.de(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Saida criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Entrada dados) {
        return pedidos.criar(Ator.de(jwt), dados);
    }

    @PutMapping("/{id}")
    Saida editar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                 @Valid @RequestBody Entrada dados) {
        return pedidos.editar(Ator.de(jwt), id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        pedidos.excluir(Ator.de(jwt), id);
    }

    @PatchMapping("/{id}/status")
    Saida status(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                 @Valid @RequestBody MudancaStatus dados) {
        return pedidos.status(Ator.de(jwt), id, dados.status());
    }
}
