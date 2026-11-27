package com.killerprotocol.cuentas.application.port.in;

public interface AuthenticateWithGoogleUseCase {

    ChallengeCreado authenticate(String idToken);
}
