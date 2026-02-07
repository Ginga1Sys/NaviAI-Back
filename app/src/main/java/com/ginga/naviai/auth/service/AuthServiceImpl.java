package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.auth.dto.LoginRequest;
import com.ginga.naviai.auth.dto.LoginResponse;
import com.ginga.naviai.auth.dto.TokenResponse;
import com.ginga.naviai.auth.exception.AccountNotEnabledException;
import com.ginga.naviai.auth.exception.InvalidCredentialsException;
import com.ginga.naviai.auth.exception.InvalidTokenException;
import com.ginga.naviai.auth.exception.TokenExpiredException;
import java.util.Optional;
import java.util.UUID;

import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.entity.RefreshToken;
import com.ginga.naviai.auth.exception.DuplicateResourceException;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.auth.repository.RefreshTokenRepository;
import com.ginga.naviai.auth.repository.ConfirmationTokenRepository;
import com.ginga.naviai.auth.util.TokenUtil;
import com.ginga.naviai.auth.util.JwtTokenUtil;
import com.ginga.naviai.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ConfirmationTokenService tokenService;
    private final MailService mailService;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${token.secret}")
    private String tokenSecret;

    @Value("${token.access.expiration:3600}")
    private long accessTokenExpiration;

    @Value("${token.refresh.expiration:2592000}")
    private long refreshTokenExpiration;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           ConfirmationTokenService tokenService,
                           MailService mailService,
                           TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.tokenBlacklistService = tokenBlacklistService;
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
    @Transactional
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

        String accessTokenJti = UUID.randomUUID().toString();
        String accessToken = JwtTokenUtil.generateAccessToken(
            String.valueOf(user.getId()),
            accessTokenJti,
            accessTokenExpiration,
            tokenSecret
        );

        // Generate and store refresh token
        String refreshTokenValue = TokenUtil.generateSecureToken();
        String refreshTokenHash = TokenUtil.hashToken(refreshTokenValue, tokenSecret);
        
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpiration);
        RefreshToken refreshToken = new RefreshToken(user, refreshTokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setDisplayName(user.getDisplayName());
        userResponse.setCreatedAt(user.getCreatedAt());

        return new LoginResponse(userResponse, accessToken, accessTokenExpiration, refreshTokenValue);
    }

    @Override
    @Transactional
    public TokenResponse refreshTokens(String refreshTokenValue) {
        // Hash the incoming token
        String tokenHash = TokenUtil.hashToken(refreshTokenValue, tokenSecret);

        // Find token in database
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Check if revoked
        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        // Check if expired
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Refresh token has expired");
        }

        User user = refreshToken.getUser();

        // Check if user is still enabled
        if (!user.isEnabled()) {
            throw new AccountNotEnabledException("Account has been disabled");
        }

        // Update last used timestamp
        refreshToken.setLastUsedAt(Instant.now());

        String newAccessTokenJti = UUID.randomUUID().toString();
        String newAccessToken = JwtTokenUtil.generateAccessToken(
            String.valueOf(user.getId()),
            newAccessTokenJti,
            accessTokenExpiration,
            tokenSecret
        );

        // Token rotation: generate new refresh token and revoke old one
        String newRefreshTokenValue = TokenUtil.generateSecureToken();
        String newRefreshTokenHash = TokenUtil.hashToken(newRefreshTokenValue, tokenSecret);
        
        Instant newExpiresAt = Instant.now().plusSeconds(refreshTokenExpiration);
        RefreshToken newRefreshToken = new RefreshToken(user, newRefreshTokenHash, newExpiresAt);
        
        // Mark old token as revoked and link to new token
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now());
        refreshToken.setReplacedBy(newRefreshToken.getJti());
        
        refreshTokenRepository.save(refreshToken);
        refreshTokenRepository.save(newRefreshToken);

        return new TokenResponse(newAccessToken, accessTokenExpiration, newRefreshTokenValue);
    }

    @Override
    @Transactional
    public void logout(String username, Optional<String> refreshTokenValue, Optional<String> accessTokenJti) {
        // リフレッシュトークンが指定されている場合、DB 上で無効化する
        if (refreshTokenValue.isPresent() && !refreshTokenValue.get().isEmpty()) {
            String tokenHash = TokenUtil.hashToken(refreshTokenValue.get(), tokenSecret);
            Optional<RefreshToken> optToken = refreshTokenRepository.findByTokenHash(tokenHash);
            
            if (optToken.isPresent()) {
                RefreshToken refreshToken = optToken.get();
                
                // トークンがリクエストしたユーザーに属しているか確認
                if (!refreshToken.getUser().getUsername().equals(username)) {
                    throw new InvalidTokenException("Token does not belong to the user");
                }
                
                // まだ無効化されていない場合のみ処理
                if (!refreshToken.isRevoked()) {
                    refreshToken.setRevoked(true);
                    refreshToken.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(refreshToken);
                }
            }
            // トークンが存在しない場合は静かに成功扱い（冪等性確保）
        }
        
        // アクセストークンの jti を Redis ブラックリストに追加
        if (accessTokenJti.isPresent() && !accessTokenJti.get().isEmpty()) {
            // TTL はアクセストークンの有効期限（残存時間を正確に計算するには別途対応が必要だが、
            // 簡易実装として全有効期限を使用）
            tokenBlacklistService.addToBlacklist(accessTokenJti.get(), accessTokenExpiration);
        }
    }
}
