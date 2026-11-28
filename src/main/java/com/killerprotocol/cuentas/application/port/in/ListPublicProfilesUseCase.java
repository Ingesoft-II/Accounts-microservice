package com.killerprotocol.cuentas.application.port.in;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ListPublicProfilesUseCase {

    List<PerfilPublico> listar(Set<UUID> ids);
}
