package br.com.fiap.nexusverde.painel;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.auth.Papel;
import br.com.fiap.nexusverde.catalogo.CatalogoDtos.PontoEntrada;
import br.com.fiap.nexusverde.catalogo.CatalogoDtos.StatusPonto;
import br.com.fiap.nexusverde.catalogo.CatalogoDtos.TipoPonto;
import br.com.fiap.nexusverde.catalogo.CatalogoService;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Status;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Urgencia;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoService;
import br.com.fiap.nexusverde.web.ApiException;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/painel")
class PainelController {
    private static final Sort ORDEM_PEDIDOS =
            Sort.by(Sort.Order.desc("prioridade"), Sort.Order.asc("criadaEm"), Sort.Order.asc("id"));
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

    @ModelAttribute("demo")
    boolean demonstracao() {
        return demo;
    }

    @GetMapping("/entrar")
    String entrar() {
        return "entrar";
    }

    @GetMapping
    String painel(Principal principal, Model model) {
        Ator equipe = ator(principal);
        model.addAttribute("abertas", pedidos.contar(equipe, Status.ABERTA));
        model.addAttribute("emAtendimento", pedidos.contar(equipe, Status.EM_ATENDIMENTO));
        model.addAttribute("triagem", pedidos.triagem(equipe, PageRequest.of(0, 1, ORDEM_PEDIDOS)).totalElements());
        model.addAttribute("totalPontos", catalogo.totalPontos());
        model.addAttribute("prioritarios", pedidos.pesquisar(equipe, "", Status.ABERTA, null,
                PageRequest.of(0, 4, ORDEM_PEDIDOS)).content());
        return "painel";
    }

    @GetMapping("/solicitacoes")
    String solicitacoes(Principal principal, Model model, @RequestParam(defaultValue = "") String busca,
                        @RequestParam(required = false) Status status, @RequestParam(required = false) Urgencia urgencia,
                        @RequestParam(defaultValue = "0") int pagina) {
        model.addAttribute("pedidos", pedidos.pesquisar(ator(principal), busca, status, urgencia,
                PageRequest.of(Math.max(0, pagina), 10, ORDEM_PEDIDOS)));
        model.addAttribute("busca", busca);
        model.addAttribute("status", status);
        model.addAttribute("urgencia", urgencia);
        model.addAttribute("situacoes", Status.values());
        model.addAttribute("urgencias", Urgencia.values());
        return "painel/solicitacoes";
    }

    @GetMapping("/solicitacoes/{id}")
    String solicitacao(Principal principal, @PathVariable Long id, Model model) {
        Ator equipe = ator(principal);
        var detalhe = pedidos.detalhePainel(equipe, id);
        model.addAttribute("pedido", detalhe.pedido());
        model.addAttribute("cadastradaPor", detalhe.cadastradaPor());
        model.addAttribute("historico", pedidos.historico(equipe, id));
        model.addAttribute("iaDisponivel", ia.disponivel());
        return "painel/solicitacao";
    }

    @PostMapping("/pedidos/{id}/status")
    String status(Principal principal, @PathVariable Long id, @RequestParam Status status,
                  @RequestParam(required = false) String motivo, RedirectAttributes retorno) {
        Ator equipe = ator(principal);
        return executar(retorno, "/painel/solicitacoes/" + id, "Situação atualizada.", () -> {
            if (status == Status.CANCELADA) {
                pedidos.cancelarNoPainel(equipe, id, motivo);
            } else {
                pedidos.status(equipe, id, status);
            }
        });
    }

    @PostMapping("/pedidos/{id}/analise")
    String analisar(Principal principal, @PathVariable Long id, RedirectAttributes retorno) {
        return executar(retorno, "/painel/solicitacoes/" + id, "Análise gerada para revisão.",
                () -> ia.analisar(ator(principal), id));
    }

    @GetMapping("/pontos-agua")
    String pontos(Model model, @RequestParam(defaultValue = "") String busca, @RequestParam(required = false) StatusPonto status,
                  @RequestParam(defaultValue = "0") int pagina) {
        model.addAttribute("pontos", catalogo.pesquisarPontos(busca, status,
                PageRequest.of(Math.max(0, pagina), 9, Sort.by("nome", "id"))));
        model.addAttribute("busca", busca);
        model.addAttribute("status", status);
        model.addAttribute("situacoes", StatusPonto.values());
        return "painel/pontos";
    }

    @GetMapping("/pontos-agua/novo")
    String novoPonto(Model model) {
        model.addAttribute("dados", new PontoEntrada("", null, "", "", null, null,
                StatusPonto.DISPONIVEL, null, null, "", "", null));
        return formularioPonto(model, null, null);
    }

    @GetMapping("/pontos-agua/{id}/editar")
    String editarPonto(@PathVariable Long id, Model model) {
        var ponto = catalogo.ponto(id);
        model.addAttribute("dados", new PontoEntrada(ponto.nome(), ponto.tipo(), ponto.endereco(), ponto.cidade(),
                ponto.latitude(), ponto.longitude(), ponto.status(), ponto.capacidadeLitros(), ponto.estoqueLitros(),
                ponto.horarioFuncionamento(), ponto.telefone(), ponto.regiaoId()));
        return formularioPonto(model, id, ponto.atualizadoEm().toString());
    }

    @PostMapping({"/pontos-agua/novo", "/pontos-agua/{id}/editar"})
    String gravarPonto(Principal principal, @PathVariable(required = false) Long id,
                       @RequestParam(required = false) String versao, @Valid @ModelAttribute("dados") PontoEntrada dados,
                       BindingResult erros, Model model, RedirectAttributes retorno) {
        if (erros.hasErrors()) {
            return formularioPonto(model, id, versao);
        }
        try {
            var ponto = catalogo.salvarPontoNoPainel(ator(principal), id, dados, versao);
            retorno.addFlashAttribute("sucesso", "Ponto de água salvo.");
            retorno.addAttribute("busca", ponto.nome());
            return "redirect:/painel/pontos-agua";
        } catch (ApiException e) {
            erros.reject("ponto.invalido", e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            erros.reject("ponto.conflito", "O ponto mudou. Abra a edição novamente para conferir os dados.");
        }
        return formularioPonto(model, id, versao);
    }

    @PostMapping("/pontos/{id}/disponibilidade")
    String ponto(Principal principal, @PathVariable Long id, @RequestParam StatusPonto status,
                 @RequestParam int estoque, RedirectAttributes retorno) {
        return executar(retorno, "/painel/pontos-agua", "Disponibilidade atualizada.",
                () -> catalogo.atualizarDisponibilidade(ator(principal), id, status, estoque));
    }

    private static String formularioPonto(Model model, Long id, String versao) {
        model.addAttribute("pontoId", id);
        model.addAttribute("versao", versao);
        model.addAttribute("tipos", TipoPonto.values());
        model.addAttribute("situacoes", StatusPonto.values());
        return "painel/ponto-formulario";
    }

    static Ator ator(Principal principal) {
        return new Ator(Long.parseLong(principal.getName()), Papel.EQUIPE);
    }

    private static String executar(RedirectAttributes retorno, String destino, String sucesso, Runnable acao) {
        try {
            acao.run();
            retorno.addFlashAttribute("sucesso", sucesso);
        } catch (ApiException e) {
            retorno.addFlashAttribute("erro", e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            retorno.addFlashAttribute("erro", "O registro mudou. Confira os dados antes de atualizar novamente.");
        }
        return "redirect:" + destino;
    }
}
