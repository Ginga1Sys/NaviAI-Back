package com.ginga.naviai.test.controller;

import com.ginga.naviai.auth.entity.ConfirmationToken;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.repository.ConfirmationTokenRepository;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.mail.SmtpMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/test")
public class TestSupportController {

    private final ConfirmationTokenRepository tokenRepo;
    private final UserRepository userRepo;

    @Autowired
    public TestSupportController(ConfirmationTokenRepository tokenRepo, UserRepository userRepo) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/token")
    public ResponseEntity<?> getTokenByEmail(@RequestParam("email") String email) {
        Optional<User> u = userRepo.findByEmail(email);
        if (u.isEmpty()) return ResponseEntity.notFound().build();
        Optional<ConfirmationToken> ct = tokenRepo.findAll().stream().filter(t -> t.getUser().getId().equals(u.get().getId())).findFirst();
        return ct.map(c -> ResponseEntity.ok(c.getToken())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUser(@RequestParam("email") String email) {
        Optional<User> u = userRepo.findByEmail(email);
        return u.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/mail/fail")
    public ResponseEntity<?> setMailFail(@RequestParam("enable") boolean enable) {
        SmtpMailService.setSimulateFailure(enable);
        return ResponseEntity.ok().build();
    }
}
