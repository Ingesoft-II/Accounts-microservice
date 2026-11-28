package com.killerprotocol.cuentas.config;

import java.nio.file.Path;
import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.killerprotocol.cuentas.adapter.out.jwt.KeyStore;
import com.killerprotocol.cuentas.adapter.out.jwt.NimbusTokenIssuer;
import com.killerprotocol.cuentas.adapter.in.web.HeaderJugadorResolver;
import com.killerprotocol.cuentas.application.ReglasAuth;
import com.killerprotocol.cuentas.application.port.in.AuthenticateWithGoogleUseCase;
import com.killerprotocol.cuentas.application.port.in.GetAccountUseCase;
import com.killerprotocol.cuentas.application.port.in.ListPublicProfilesUseCase;
import com.killerprotocol.cuentas.application.port.in.LogoutUseCase;
import com.killerprotocol.cuentas.application.port.in.RefreshSessionUseCase;
import com.killerprotocol.cuentas.application.port.in.UpdateProfileUseCase;
import com.killerprotocol.cuentas.application.port.in.VerifyTwoFactorUseCase;
import com.killerprotocol.cuentas.application.port.out.AccountRepository;
import com.killerprotocol.cuentas.application.port.out.ChallengeRepository;
import com.killerprotocol.cuentas.application.port.out.EmailSender;
import com.killerprotocol.cuentas.application.port.out.EventPublisher;
import com.killerprotocol.cuentas.application.port.out.GoogleIdentityVerifier;
import com.killerprotocol.cuentas.application.port.out.RefreshTokenRepository;
import com.killerprotocol.cuentas.application.port.out.TokenIssuer;
import com.killerprotocol.cuentas.application.service.AuthenticateWithGoogleService;
import com.killerprotocol.cuentas.application.service.GetAccountService;
import com.killerprotocol.cuentas.application.service.ListPublicProfilesService;
import com.killerprotocol.cuentas.application.service.LogoutService;
import com.killerprotocol.cuentas.application.service.RefreshSessionService;
import com.killerprotocol.cuentas.application.service.UpdateProfileService;
import com.killerprotocol.cuentas.application.service.VerifyTwoFactorService;

@Configuration
public class CuentasConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ReglasAuth reglasAuth(CuentasProperties p) {
        return new ReglasAuth(p.bootstrap().moderatorEmail(),
                p.auth().twoFactor().ttlSec(),
                p.auth().twoFactor().maxChallengesPerWindow(),
                p.auth().twoFactor().windowSec(),
                p.jwt().ttlSec(),
                p.auth().refresh().ttlDays());
    }

    @Bean
    public KeyStore keyStore(CuentasProperties properties) {
        return new KeyStore(Path.of(properties.jwt().keyPath()));
    }

    @Bean
    public TokenIssuer tokenIssuer(KeyStore keyStore, CuentasProperties properties, Clock clock) {
        CuentasProperties.Jwt jwt = properties.jwt();
        return new NimbusTokenIssuer(keyStore, jwt.issuer(), jwt.audience(), jwt.ttlSec(), clock);
    }

    @Bean
    public AuthenticateWithGoogleUseCase authenticateWithGoogle(
            GoogleIdentityVerifier identidad,
            AccountRepository cuentas,
            ChallengeRepository desafios,
            EmailSender correo,
            ReglasAuth reglas,
            Clock clock) {
        return new AuthenticateWithGoogleService(identidad, cuentas, desafios, correo, reglas, clock);
    }

    @Bean
    public VerifyTwoFactorUseCase verifyTwoFactor(
            ChallengeRepository desafios,
            AccountRepository cuentas,
            RefreshTokenRepository refreshTokens,
            TokenIssuer tokens,
            EventPublisher eventos,
            ReglasAuth reglas,
            Clock clock) {
        return new VerifyTwoFactorService(desafios, cuentas, refreshTokens, tokens, eventos,
                reglas, clock);
    }

    @Bean
    public RefreshSessionUseCase refreshSession(
            RefreshTokenRepository refreshTokens,
            AccountRepository cuentas,
            TokenIssuer tokens,
            ReglasAuth reglas,
            Clock clock) {
        return new RefreshSessionService(refreshTokens, cuentas, tokens, reglas, clock);
    }

    @Bean
    public LogoutUseCase logout(RefreshTokenRepository refreshTokens, Clock clock) {
        return new LogoutService(refreshTokens, clock);
    }

    @Bean
    public HeaderJugadorResolver headerJugadorResolver() {
        return new HeaderJugadorResolver();
    }

    @Bean
    public GetAccountUseCase getAccount(AccountRepository cuentas) {
        return new GetAccountService(cuentas);
    }

    @Bean
    public UpdateProfileUseCase updateProfile(AccountRepository cuentas) {
        return new UpdateProfileService(cuentas);
    }

    @Bean
    public ListPublicProfilesUseCase listPublicProfiles(AccountRepository cuentas) {
        return new ListPublicProfilesService(cuentas);
    }
}
