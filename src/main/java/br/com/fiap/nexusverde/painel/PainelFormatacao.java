package br.com.fiap.nexusverde.painel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("painelFormato")
public class PainelFormatacao {
    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
                    .withZone(ZoneId.of("America/Sao_Paulo"));

    public String data(Instant instante) {
        return DATA.format(instante);
    }

    public String rotulo(Enum<?> valor) {
        return switch (valor.name()) {
            case "ABERTA" -> "Aberta";
            case "EM_ATENDIMENTO" -> "Em atendimento";
            case "ATENDIDA" -> "Atendida";
            case "CANCELADA" -> "Cancelada";
            case "BAIXA" -> "Baixa";
            case "MEDIA" -> "Média";
            case "ALTA" -> "Alta";
            case "CRITICA" -> "Crítica";
            case "FALTA_DE_AGUA" -> "Falta de água";
            case "AGUA_CONTAMINADA" -> "Água contaminada";
            case "ENTREGA_DE_AGUA" -> "Entrega de água";
            case "OUTRO" -> "Outro";
            case "POSTO_DISTRIBUICAO" -> "Posto de distribuição";
            case "CAMINHAO_PIPA" -> "Caminhão-pipa";
            case "RESERVATORIO" -> "Reservatório";
            case "ABRIGO" -> "Abrigo";
            case "DISPONIVEL" -> "Disponível";
            case "ESTOQUE_BAIXO" -> "Estoque baixo";
            case "ESGOTADO" -> "Esgotado";
            case "FECHADO" -> "Fechado";
            default -> valor.name();
        };
    }
}
