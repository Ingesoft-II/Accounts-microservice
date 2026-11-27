package com.killerprotocol.cuentas.application.port.out;

import java.util.Set;
import java.util.UUID;

import com.killerprotocol.cuentas.domain.model.Account;

public interface AccountRepository {

    Account buscarPorGoogleSubject(String googleSubject);

    Account buscarPorId(UUID id);

    void guardar(Account cuenta);

    Set<Account> listarPublicosPorIds(Set<UUID> ids);
}
