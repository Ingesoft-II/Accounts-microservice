package com.killerprotocol.cuentas.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.killerprotocol.cuentas.application.port.in.ListPublicProfilesUseCase;
import com.killerprotocol.cuentas.application.port.in.PerfilPublico;
import com.killerprotocol.cuentas.application.port.out.AccountRepository;

public class ListPublicProfilesService implements ListPublicProfilesUseCase {

    public static final int MAX_IDS = 20;

    private final AccountRepository cuentas;

    public ListPublicProfilesService(AccountRepository cuentas) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
    }

    @Override
    public List<PerfilPublico> listar(Set<UUID> ids) {
        Objects.requireNonNull(ids, "ids");
        if (ids.size() > MAX_IDS) {
            throw new IllegalArgumentException("máximo " + MAX_IDS + " ids por consulta");
        }
        return cuentas.listarPublicosPorIds(ids).stream()
                .map(PerfilPublico::de)
                .sorted(Comparator.comparing(p -> p.playerId().toString()))
                .toList();
    }
}
