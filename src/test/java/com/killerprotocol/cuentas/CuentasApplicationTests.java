package com.killerprotocol.cuentas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.killerprotocol.cuentas.application.port.out.AccountRepository;
import com.killerprotocol.cuentas.application.port.out.ChallengeRepository;
import com.killerprotocol.cuentas.application.port.out.EmailSender;
import com.killerprotocol.cuentas.application.port.out.EventPublisher;
import com.killerprotocol.cuentas.application.port.out.GoogleIdentityVerifier;
import com.killerprotocol.cuentas.application.port.out.RefreshTokenRepository;

@SpringBootTest
class CuentasApplicationTests {

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private ChallengeRepository challengeRepository;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private GoogleIdentityVerifier googleIdentityVerifier;

    @MockBean
    private EmailSender emailSender;

    @MockBean
    private EventPublisher eventPublisher;

    @Test
    void contextLoads() {
    }
}
