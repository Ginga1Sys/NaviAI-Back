package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.auth.dto.LoginRequest;
import com.ginga.naviai.auth.dto.LoginResponse;
import com.ginga.naviai.auth.exception.AccountNotEnabledException;
import com.ginga.naviai.auth.exception.InvalidCredentialsException;
import java.util.UUID;

import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.exception.DuplicateResourceException;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.auth.repository.ConfirmationTokenRepository;
import com.ginga.naviai.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ConfirmationTokenService tokenService;
    private final MailService mailService;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           ConfirmationTokenService tokenService,
                           MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailService = mailService;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateResourceException("username is already taken");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("email is already registered");
        }

        User u = new User();
        u.setUsername(request.getUsername());
        u.setEmail(request.getEmail());
        u.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        u.setDisplayName(request.getDisplayName());
        u.setEnabled(false);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());

        User saved = userRepository.save(u);

        // create confirmation token and send email
        String token = tokenService.createTokenForUser(saved);
        String confirmUrl = String.format("%s/api/v1/auth/confirm?token=%s", "http://localhost:8080", token);
        String subject = "NaviAI - メール確認";
        String body = "登録ありがとうございます。以下のURLからメールを確認してください:\n" + confirmUrl;
        try {
            mailService.send(saved.getEmail(), subject, body);
        } catch (Exception ex) {
            // log and proceed; mail failures should be retried in production
        }

        UserResponse res = new UserResponse();
        res.setId(saved.getId());
        res.setUsername(saved.getUsername());
        res.setEmail(saved.getEmail());
        res.setDisplayName(saved.getDisplayName());
        res.setCreatedAt(saved.getCreatedAt());
        return res;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            throw new AccountNotEnabledException("Account is not enabled yet. Please check your email.");
        }

        // Generate a dummy token or use existing utility
        // Currently, we use UUID as a placeholder for session token/JWT
        String token = UUID.randomUUID().toString();
        long expiresIn = 3600; // 1 hour

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setDisplayName(user.getDisplayName());
        userResponse.setCreatedAt(user.getCreatedAt());

        return new LoginResponse(userResponse, token, expiresIn);
    }
}
