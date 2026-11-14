package br.com.fiap.nexusverde.web;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static ApiException naoEncontrado(String recurso) {
        return new ApiException(HttpStatus.NOT_FOUND, recurso + " não encontrado.");
    }

    public static ApiException proibido() {
        return new ApiException(HttpStatus.FORBIDDEN, "Você não tem permissão para esta operação.");
    }

    public static ApiException conflito(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException invalido(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
