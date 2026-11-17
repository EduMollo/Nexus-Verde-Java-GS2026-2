package br.com.fiap.nexusverde.solicitacao;

import java.time.Instant;
import jakarta.validation.constraints.*;

public final class SolicitacaoDtos {
    private SolicitacaoDtos() {}

    public enum Tipo { FALTA_DE_AGUA, AGUA_CONTAMINADA, ENTREGA_DE_AGUA, OUTRO }
    public enum Urgencia { BAIXA, MEDIA, ALTA, CRITICA }
    public enum Status { ABERTA, EM_ATENDIMENTO, ATENDIDA, CANCELADA }

    public record Entrada(
            @NotNull Tipo tipo,
            @NotBlank @Size(max = 1000) String descricao,
            @NotNull @Positive Integer quantidadePessoas,
            @NotNull Urgencia urgencia,
            @NotNull Boolean grupoVulneravel,
            @NotBlank @Size(max = 240) String endereco,
            @NotBlank @Size(max = 120) String cidade,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Positive Long regiaoId) {}

    public record MudancaStatus(@NotNull Status status) {}
    public record Solicitante(Long id, String nome) {}
    public record Saida(Long id, Tipo tipo, String descricao, int quantidadePessoas, Urgencia urgencia,
                        boolean grupoVulneravel, String endereco, String cidade, Double latitude, Double longitude,
                        Status status, Integer prioridade, String analiseIa, Solicitante solicitante,
                        Instant criadaEm, Instant atualizadaEm, Long regiaoId) {}

    public record Movimentacao(Status statusAnterior, Status statusNovo, String motivo, Instant registradaEm) {}

    public record ContextoTriagem(long versao, Tipo tipo, Urgencia urgencia, int quantidadePessoas,
                                  boolean grupoVulneravel, int prioridade) {}
}
