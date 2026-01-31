package com.ginga.naviai.test.legacy;

import com.ginga.naviai.auth.entity.ConfirmationToken;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.repository.ConfirmationTokenRepository;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.mail.SmtpMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

// Legacy/test helper - not a Controller. Kept for reference; not component-scanned.
public class LegacyTestSupportController {

    private final ConfirmationTokenRepository tokenRepo;
    private final UserRepository userRepo;

    @Autowired
    public LegacyTestSupportController(ConfirmationTokenRepository tokenRepo, UserRepository userRepo) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
    }

    public ResponseEntity<?> getTokenByEmail(String email) {
        Optional<User> u = userRepo.findByEmail(email);
        if (u.isEmpty()) return ResponseEntity.notFound().build();
        Optional<ConfirmationToken> ct = tokenRepo.findAll().stream().filter(t -> t.getUser().getId().equals(u.get().getId())).findFirst();
        return ct.map(c -> ResponseEntity.ok(c.getToken())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getUser(String email) {
        Optional<User> u = userRepo.findByEmail(email);
        return u.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> setMailFail(boolean enable) {
        SmtpMailService.setSimulateFailure(enable);
        return ResponseEntity.ok().build();
    }
}
