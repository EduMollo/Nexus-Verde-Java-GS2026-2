package br.com.fiap.nexusverde.catalogo;

import java.time.Instant;
import jakarta.validation.constraints.*;

public final class CatalogoDtos {
    private CatalogoDtos() {}

    public enum NivelRisco { BAIXO, MODERADO, ALTO, CRITICO }
    public enum TipoPonto { POSTO_DISTRIBUICAO, CAMINHAO_PIPA, RESERVATORIO, ABRIGO }
    public enum StatusPonto { DISPONIVEL, ESTOQUE_BAIXO, ESGOTADO, FECHADO }

    public record RegiaoEntrada(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Size(max = 120) String cidade,
            @NotNull NivelRisco nivelRisco,
            @NotBlank @Size(max = 160) String situacao,
            @Size(max = 1000) String descricao,
            @NotNull @PositiveOrZero Integer populacaoAfetada) {}

    public record RegiaoSaida(Long id, String nome, String cidade, NivelRisco nivelRisco,
                             String situacao, String descricao, int populacaoAfetada, Instant atualizadoEm) {}

    public record PontoEntrada(
            @NotBlank @Size(max = 120) String nome,
            @NotNull TipoPonto tipo,
            @NotBlank @Size(max = 240) String endereco,
            @NotBlank @Size(max = 120) String cidade,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @NotNull StatusPonto status,
            @NotNull @PositiveOrZero Integer capacidadeLitros,
            @NotNull @PositiveOrZero Integer estoqueLitros,
            @Size(max = 120) String horarioFuncionamento,
            @Size(max = 32) String telefone,
            @Positive Long regiaoId) {}

    public record PontoSaida(Long id, String nome, TipoPonto tipo, String endereco, String cidade,
                            Double latitude, Double longitude, StatusPonto status, int capacidadeLitros,
                            int estoqueLitros, String horarioFuncionamento, String telefone,
                            Instant atualizadoEm, Long regiaoId) {}
}
