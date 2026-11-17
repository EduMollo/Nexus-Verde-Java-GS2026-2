package br.com.fiap.nexusverde.auth;

import java.io.Serializable;
import java.security.Principal;

record IdentidadePainel(long id, long versao) implements Principal, Serializable {
    @Override
    public String getName() {
        return Long.toString(id);
    }
}
