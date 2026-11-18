package br.com.fiap.nexusverde.iot;

import static org.assertj.core.api.Assertions.*;
import static br.com.fiap.nexusverde.catalogo.CatalogoDtos.StatusPonto.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservatorioTest {
    private final Instant agora=Instant.parse("2026-11-18T15:00:00Z");
    private Telemetria.Leitura leitura(int nivel,int idade) {
        return new Telemetria.Leitura(UUID.randomUUID(), agora.minusSeconds(idade), nivel,"Teste");
    }

    @Test void calculaDiferencaSemArredondarOPercentual() {
        var s=Situacao.avaliar(2,ESTOQUE_BAIXO,8000,1200,leitura(5,0),agora,900);
        assertThat(s.estimativa()).isEqualTo(400L);
        assertThat(s.diferenca()).isEqualTo(-800L);
        assertThat(s.consulta()).contains("Divergência");
    }

    @Test void priorizaQualidadeDoDadoESituacaoDoLocal() {
        assertThat(Situacao.avaliar(2,DISPONIVEL,8000,1200,null,agora,900).estimativa()).isNull();
        assertThat(Situacao.avaliar(2,DISPONIVEL,8000,1200,leitura(5,901),agora,900).consulta()).contains("desatualizada");
        assertThat(Situacao.avaliar(2,DISPONIVEL,0,0,leitura(5,0),agora,900).estimativa()).isNull();
        assertThat(Situacao.avaliar(2,FECHADO,4000,0,leitura(70,0),agora,900).consulta()).contains("fechado");
        assertThat(Situacao.avaliar(2,ESGOTADO,4000,0,leitura(0,0),agora,900).consulta()).contains("esgotado");
        assertThat(Situacao.avaliar(2,DISPONIVEL,10000,7000,leitura(70,0),agora,900).consulta()).contains("rotina");
    }

    @Test void avaliaRecuperacaoComConsultasIndependentes() throws Exception {
        var base=new Procedimentos();
        String[][] casos={{"sensor sem sinal","MED-01"},{"horário antigo, leitura atrasada","MED-01"},
                {"capacidade inválida","CAD-02"},{"volume do reservatório desconhecido","CAD-02"},
                {"confirmar reabertura do local","OPE-03"},{"distribuição em ponto fechado","OPE-03"},
                {"inventário e medição apresentam diferença","EST-04"},{"ajuste do volume estimado","EST-04"},
                {"reservatório vazio","REP-05"},{"encaminhar reposição","REP-05"},
                {"repetir na próxima ronda","ROT-06"},{"acompanhamento periódico","ROT-06"}};
        long top1=0;
        for (String[] caso : casos) {
            var hits=base.buscar(caso[0]);
            assertThat(hits).extracting(Procedimentos.Fonte::codigo).as(caso[0]).contains(caso[1]);
            if (hits.get(0).codigo().equals(caso[1])) top1++;
        }
        System.out.printf("RAG N=%d top1=%d top3=%d%n",casos.length,top1,casos.length);
        assertThat(base.buscar("xyzzy")).isEmpty();
    }
}
