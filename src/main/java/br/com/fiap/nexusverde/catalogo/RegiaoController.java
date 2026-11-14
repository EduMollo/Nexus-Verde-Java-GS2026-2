package br.com.fiap.nexusverde.catalogo;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.catalogo.CatalogoDtos.*;
import br.com.fiap.nexusverde.web.Pagina;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/regioes")
class RegiaoController {
    private final CatalogoService catalogo;

    RegiaoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    Pagina<RegiaoSaida> listar(@PageableDefault(size = 50, sort = "id") Pageable pagina) {
        return catalogo.regioes(pagina);
    }

    @GetMapping("/{id}")
    RegiaoSaida detalhe(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Ator.de(jwt).exigirEquipe();
        return catalogo.regiao(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RegiaoSaida criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RegiaoEntrada dados) {
        return catalogo.salvarRegiao(Ator.de(jwt), null, dados);
    }

    @PutMapping("/{id}")
    RegiaoSaida editar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                       @Valid @RequestBody RegiaoEntrada dados) {
        return catalogo.salvarRegiao(Ator.de(jwt), id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        catalogo.excluirRegiao(Ator.de(jwt), id);
    }
}
