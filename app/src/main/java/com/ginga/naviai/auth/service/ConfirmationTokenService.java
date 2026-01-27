package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.entity.ConfirmationToken;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.exception.DuplicateResourceException;
import com.ginga.naviai.auth.repository.ConfirmationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConfirmationTokenService {

    private final ConfirmationTokenRepository tokenRepository;

    @Autowired
    public ConfirmationTokenService(ConfirmationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public String createTokenForUser(User user) {
        ConfirmationToken token = new ConfirmationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        ConfirmationToken saved = tokenRepository.save(token);
        return saved.getToken();
    }

    public Optional<ConfirmationToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public void setConfirmedAt(ConfirmationToken token, Instant when) {
        token.setConfirmedAt(when);
        tokenRepository.save(token);
    }
}
