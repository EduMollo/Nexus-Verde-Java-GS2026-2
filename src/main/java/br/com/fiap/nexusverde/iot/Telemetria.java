package br.com.fiap.nexusverde.iot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.constraints.*;

@Service
public class Telemetria {
    public record Entrada(@NotNull UUID evento, @NotNull Instant medidaEm,
                          @NotNull @Min(0) @Max(100) Integer percentual) {}
    public record Leitura(UUID evento, Instant medidaEm, int percentual, String origem) {}
    private final JdbcTemplate db;
    Telemetria(JdbcTemplate db) { this.db = db; }

    private void bloquear(long ponto) {
        if (db.queryForList("SELECT id FROM pontos_agua WHERE id=? FOR UPDATE", Long.class, ponto).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    @Transactional
    public String parear(long ponto) {
        bloquear(ponto);
        String chave = UUID.randomUUID().toString() + UUID.randomUUID();
        db.update("DELETE FROM iot_pareamento WHERE ponto_id=?", ponto);
        db.update("INSERT INTO iot_pareamento(ponto_id,chave_hash,expira_ms) VALUES (?,?,?)", ponto, hash(chave),
                Instant.now().plus(Duration.ofMinutes(30)).toEpochMilli());
        return chave;
    }

    @Transactional
    public void receber(long ponto, String token, Entrada dados) {
        if (token == null || token.length() != 72) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        bloquear(ponto);
        var agora = Instant.now();
        var chaves = db.queryForList("SELECT chave_hash FROM iot_pareamento WHERE ponto_id=? AND expira_ms>?",
                String.class, ponto, agora.toEpochMilli());
        if (chaves.isEmpty() || !MessageDigest.isEqual(chaves.get(0).getBytes(StandardCharsets.US_ASCII),
                hash(token).getBytes(StandardCharsets.US_ASCII))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (dados.medidaEm().isAfter(agora.plusSeconds(5)) || dados.medidaEm().isBefore(agora.minus(Duration.ofDays(1))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário fora da janela de simulação");
        var iguais = db.queryForList("SELECT percentual FROM iot_leitura WHERE ponto_id=? AND evento=? AND medida_ms=?",
                Integer.class, ponto, dados.evento(), dados.medidaEm().toEpochMilli());
        if (!iguais.isEmpty() && iguais.get(0).equals(dados.percentual())) return;
        var ultima = ultima(ponto);
        if (ultima.isPresent() && !dados.medidaEm().isAfter(ultima.get().medidaEm()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Leitura fora de ordem");
        if (db.queryForObject("SELECT COUNT(*) FROM iot_leitura WHERE ponto_id=? AND evento=?", Long.class,ponto,dados.evento()) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Identificador reutilizado");
        db.update("INSERT INTO iot_leitura(ponto_id,evento,medida_ms,percentual) VALUES (?,?,?,?)",
                ponto, dados.evento(), dados.medidaEm().toEpochMilli(), dados.percentual());
    }

    public Optional<Leitura> ultima(long ponto) {
        return db.query("SELECT evento,medida_ms,percentual FROM iot_leitura WHERE ponto_id=? ORDER BY medida_ms DESC LIMIT 1",
                (r,n) -> new Leitura(r.getObject(1, UUID.class), Instant.ofEpochMilli(r.getLong(2)),r.getInt(3),"Wokwi"), ponto)
                .stream().findFirst();
    }
}
