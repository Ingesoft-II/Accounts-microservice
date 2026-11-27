package com.killerprotocol.cuentas.fakes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.killerprotocol.cuentas.application.port.out.AccountRepository;
import com.killerprotocol.cuentas.domain.model.Account;

public class InMemoryAccountRepository implements AccountRepository {

    private final Map<UUID, Account> porId = new ConcurrentHashMap<>();
    private final Map<String, UUID> porGoogleSubject = new ConcurrentHashMap<>();

    /** Carga una cuenta previa al escenario de la prueba. */
    public void sembrar(Account cuenta) {
        guardar(cuenta);
    }

    @Override
    public Account buscarPorGoogleSubject(String googleSubject) {
        UUID id = porGoogleSubject.get(googleSubject);
        return id == null ? null : porId.get(id);
    }

    @Override
    public Account buscarPorId(UUID id) {
        return porId.get(id);
    }

    @Override
    public void guardar(Account cuenta) {
        porId.put(cuenta.id(), cuenta);
        porGoogleSubject.put(cuenta.googleSubject(), cuenta.id());
    }

    @Override
    public Set<Account> listarPublicosPorIds(Set<UUID> ids) {
        Set<Account> resultado = new HashSet<>();
        for (UUID id : ids) {
            Account cuenta = porId.get(id);
            if (cuenta != null) {
                resultado.add(cuenta);
            }
        }
        return resultado;
    }
}
