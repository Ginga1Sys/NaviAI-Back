package com.ginga.naviai.config;

import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.entity.UserRole;
import com.ginga.naviai.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Development bootstrap that ensures a known admin user exists on startup.
 * This is intended for local/dev use only and should not be used in production.
 */
@Component
public class DevAdminBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevAdminBootstrap.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Environment env;

    @Autowired
    public DevAdminBootstrap(UserRepository userRepository,
                             BCryptPasswordEncoder passwordEncoder,
                             Environment env) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String adminUser = env.getProperty("DEV_ADMIN_USERNAME", "admin");
        String adminPass = env.getProperty("DEV_ADMIN_PASSWORD", "adminpass");
        String adminEmail = env.getProperty("DEV_ADMIN_EMAIL", "admin@local");

        Optional<User> existing = userRepository.findByUsername(adminUser);
        if (existing.isPresent()) {
            logger.info("Dev admin already exists: {}", adminUser);
            return;
        }

        User u = new User();
        u.setUsername(adminUser);
        u.setEmail(adminEmail);
        u.setDisplayName("Admin");
        u.setPasswordHash(passwordEncoder.encode(adminPass));
        u.setRole(UserRole.ADMIN);
        u.setEnabled(true);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());

        userRepository.save(u);
        logger.info("Created dev admin user '{}' with supplied password (for dev only)", adminUser);
    }
}
