package br.com.fiap.nexusverde.iot;

import br.com.fiap.nexusverde.catalogo.CatalogoService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/painel/pontos-agua/{id}/reservatorio")
class PainelReservatorio {
    private static final Logger log = LoggerFactory.getLogger(PainelReservatorio.class);
    private final CatalogoService catalogo;
    private final Telemetria telemetria;
    private final Copiloto copiloto;
    private final long validade;

    PainelReservatorio(CatalogoService catalogo, Telemetria telemetria, Copiloto copiloto,
                       @Value("${IOT_VALIDADE_SEGUNDOS:900}") long validade) {
        this.catalogo=catalogo; this.telemetria=telemetria; this.copiloto=copiloto;
        if (validade<1) throw new IllegalArgumentException("IOT_VALIDADE_SEGUNDOS deve ser positivo");
        this.validade=validade;
    }

    private Situacao carregar(long id, boolean simular, Model model) {
        var ponto=catalogo.ponto(id);
        var agora=Instant.now();
        var leitura=simular ? new Telemetria.Leitura(UUID.randomUUID(),agora,5,"Cenário de avaliação")
                : telemetria.ultima(id).orElse(null);
        var situacao=Situacao.avaliar(id,ponto.status(),ponto.capacidadeLitros(),ponto.estoqueLitros(),
                leitura,agora,validade);
        model.addAttribute("ponto",ponto);
        model.addAttribute("situacao",situacao);
        return situacao;
    }

    @GetMapping
    String abrir(@PathVariable long id, Model model) {
        carregar(id,false,model);
        return "painel/reservatorio";
    }

    @PostMapping("/parear")
    String parear(@PathVariable long id, RedirectAttributes retorno) {
        retorno.addFlashAttribute("chaveSensor",telemetria.parear(id));
        return "redirect:/painel/pontos-agua/"+id+"/reservatorio";
    }

    @PostMapping("/analisar")
    String analisar(@PathVariable long id, @RequestParam(defaultValue="false") boolean simular, Model model) {
        var situacao=carregar(id,simular,model);
        try {
            model.addAttribute("parecer",copiloto.analisar(situacao));
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Análise de abastecimento indisponível ({})",ex.getClass().getSimpleName());
            model.addAttribute("erroIa","Não foi possível gerar uma orientação. Confira a leitura e tente novamente.");
        }
        return "painel/reservatorio";
    }
}
