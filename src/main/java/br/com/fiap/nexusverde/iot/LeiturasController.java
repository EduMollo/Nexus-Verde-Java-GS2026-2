package br.com.fiap.nexusverde.iot;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iot/pontos")
class LeiturasController {
    private final Telemetria telemetria;
    LeiturasController(Telemetria telemetria) { this.telemetria = telemetria; }

    @PostMapping("/{id}/leituras")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void receber(@PathVariable long id, @RequestHeader(value="X-Sensor-Key", required=false) String chave,
                 @RequestBody @Valid Telemetria.Entrada leitura) {
        telemetria.receber(id, chave, leitura);
    }
}
