package br.com.fiap.nexusverde.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.data.mapping.PropertyReferenceException;

@RestControllerAdvice(basePackages = {
    "br.com.fiap.nexusverde.auth", "br.com.fiap.nexusverde.catalogo", "br.com.fiap.nexusverde.solicitacao"
})
public class ApiErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> negocio(ApiException e) {
        return ResponseEntity.status(e.status()).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validacao(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("Confira os campos informados.", errors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            PropertyReferenceException.class})
    ResponseEntity<ApiError> formato() {
        return ResponseEntity.badRequest().body(new ApiError("Formato de dados inválido."));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> alteracaoSimultanea() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("O registro mudou. Atualize os dados e tente novamente."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integridade() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("A operação conflita com um registro existente."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> inesperado(Exception e) {
        log.error("Falha não tratada na API", e);
        return ResponseEntity.internalServerError().body(new ApiError("Não foi possível concluir a operação."));
    }
}
