package br.com.fiap.nexusverde.painel;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.auth.Papel;
import br.com.fiap.nexusverde.catalogo.CatalogoDtos.StatusPonto;
import br.com.fiap.nexusverde.catalogo.CatalogoService;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Status;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoService;
import br.com.fiap.nexusverde.web.ApiException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/painel")
class PainelController {
    private final SolicitacaoService pedidos;
    private final CatalogoService catalogo;
    private final TriagemIa ia;
    private final boolean demo;

    PainelController(SolicitacaoService pedidos, CatalogoService catalogo, TriagemIa ia,
                      @Value("${nexus.demo}") boolean demo) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
        this.ia = ia;
        this.demo = demo;
    }

    @GetMapping("/entrar")
    String entrar() {
        return "entrar";
    }

    @GetMapping
    String painel(Principal principal, Model model,
                  @RequestParam(defaultValue = "0") int pagina,
                  @RequestParam(defaultValue = "0") int paginaPontos) {
        Ator equipe = ator(principal);
        var ordem = Sort.by(Sort.Order.desc("prioridade"), Sort.Order.asc("criadaEm"), Sort.Order.asc("id"));
        var pedidoPagina = PageRequest.of(Math.max(0, pagina), 15, ordem);
        var pontoPagina = PageRequest.of(Math.max(0, paginaPontos), 12, Sort.by("nome", "id"));
        model.addAttribute("pedidos", pedidos.listar(equipe, pedidoPagina));
        model.addAttribute("triagem", pedidos.triagem(equipe, PageRequest.of(0, 1, ordem)).totalElements());
        model.addAttribute("pontos", catalogo.pontos(pontoPagina));
        model.addAttribute("statusPonto", StatusPonto.values());
        model.addAttribute("iaDisponivel", ia.disponivel());
        model.addAttribute("demo", demo);
        return "painel";
    }

    @PostMapping("/pedidos/{id}/status")
    String status(Principal principal, @PathVariable Long id, @RequestParam Status status,
                  RedirectAttributes retorno) {
        return executar(retorno, "Situação atualizada.", () -> pedidos.status(ator(principal), id, status));
    }

    @PostMapping("/pedidos/{id}/analise")
    String analisar(Principal principal, @PathVariable Long id, RedirectAttributes retorno) {
        return executar(retorno, "Análise gerada para revisão.", () -> ia.analisar(ator(principal), id));
    }

    @PostMapping("/pontos/{id}/disponibilidade")
    String ponto(Principal principal, @PathVariable Long id, @RequestParam StatusPonto status,
                 @RequestParam int estoque, RedirectAttributes retorno) {
        return executar(retorno, "Disponibilidade atualizada.",
                () -> catalogo.atualizarDisponibilidade(ator(principal), id, status, estoque));
    }

    private static Ator ator(Principal principal) {
        return new Ator(Long.parseLong(principal.getName()), Papel.EQUIPE);
    }

    private static String executar(RedirectAttributes retorno, String sucesso, Runnable acao) {
        try {
            acao.run();
            retorno.addFlashAttribute("sucesso", sucesso);
        } catch (ApiException e) {
            retorno.addFlashAttribute("erro", e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            retorno.addFlashAttribute("erro", "O registro mudou. Confira os dados antes de atualizar novamente.");
        }
        return "redirect:/painel";
    }
}
