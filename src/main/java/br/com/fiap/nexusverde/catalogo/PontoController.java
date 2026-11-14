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
@RequestMapping("/api/pontos-agua")
class PontoController {
    private final CatalogoService catalogo;

    PontoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    Pagina<PontoSaida> listar(@PageableDefault(size = 50, sort = "id") Pageable pagina) {
        return catalogo.pontos(pagina);
    }

    @GetMapping("/{id}")
    PontoSaida detalhe(@PathVariable Long id) {
        return catalogo.ponto(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PontoSaida criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PontoEntrada dados) {
        return catalogo.salvarPonto(Ator.de(jwt), null, dados);
    }

    @PutMapping("/{id}")
    PontoSaida editar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                      @Valid @RequestBody PontoEntrada dados) {
        return catalogo.salvarPonto(Ator.de(jwt), id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        catalogo.excluirPonto(Ator.de(jwt), id);
    }
}
