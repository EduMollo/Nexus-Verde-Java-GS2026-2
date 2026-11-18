package br.com.fiap.nexusverde.iot;

import br.com.fiap.nexusverde.catalogo.CatalogoDtos.StatusPonto;
import java.time.Instant;

public record Situacao(long pontoId, StatusPonto status, int capacidade, int estoque, Telemetria.Leitura leitura,
                       Long estimativa, Long diferenca, String consulta) {
    static Situacao avaliar(long id, StatusPonto status, int capacidade, int estoque,
                            Telemetria.Leitura leitura, Instant agora, long validadeSegundos) {
        Long estimativa = leitura == null || capacidade <= 0 ? null : Math.round(capacidade * (leitura.percentual()/100.0));
        Long diferenca = estimativa == null ? null : estimativa - estoque;
        String consulta;
        if (leitura == null || leitura.medidaEm().isBefore(agora.minusSeconds(validadeSegundos))) {
            consulta = "Leitura ausente ou desatualizada";
        } else if (capacidade <= 0) {
            consulta = "Capacidade desconhecida ou zero";
        } else if (status == StatusPonto.FECHADO) {
            consulta = "Ponto fechado";
        } else if (Math.abs(diferenca) >= Math.max(1,capacidade * 0.05)) {
            consulta = "Divergência de estoque";
        } else if (leitura.percentual() <= 15 || status == StatusPonto.ESGOTADO || status == StatusPonto.ESTOQUE_BAIXO) {
            consulta = "Nível baixo ou esgotado";
        } else {
            consulta = "Acompanhamento de rotina";
        }
        return new Situacao(id,status,capacidade,estoque,leitura,estimativa,diferenca,consulta);
    }
}
