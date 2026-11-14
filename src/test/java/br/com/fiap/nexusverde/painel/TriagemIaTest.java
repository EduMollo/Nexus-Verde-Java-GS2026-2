package br.com.fiap.nexusverde.painel;

import static br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Tipo.AGUA_CONTAMINADA;
import static br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.Urgencia.ALTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import br.com.fiap.nexusverde.auth.Ator;
import br.com.fiap.nexusverde.auth.Papel;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoDtos.ContextoTriagem;
import br.com.fiap.nexusverde.solicitacao.SolicitacaoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.List;

class TriagemIaTest {
    @Test
    void usaSpringAiEPersisteARespostaComAVersaoDaSolicitacao() {
        var equipe = new Ator(1L, Papel.EQUIPE);
        var pedidos = mock(SolicitacaoService.class);
        var provedor = mock(ChatModel.class);
        var beans = new StaticListableBeanFactory();
        beans.addBean("provedor", provedor);
        var ia = new TriagemIa(beans.getBeanProvider(ChatModel.class), pedidos);
        var captura = ArgumentCaptor.forClass(Prompt.class);
        String analise = "A urgência e o grupo vulnerável elevam a prioridade. Confirme a demanda com a equipe local.";
        when(pedidos.contexto(equipe, 7L))
                .thenReturn(new ContextoTriagem(2L, AGUA_CONTAMINADA, ALTA, 5, true, 75));
        when(provedor.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(analise)))));

        assertThat(ia.disponivel()).isTrue();
        ia.analisar(equipe, 7L);

        verify(provedor).call(captura.capture());
        assertThat(captura.getValue().getUserMessage().getText())
                .contains("AGUA_CONTAMINADA", "Pessoas: 5", "75/100");
        verify(pedidos).salvarAnalise(eq(equipe), eq(7L), eq(2L), eq(analise));
    }
}
