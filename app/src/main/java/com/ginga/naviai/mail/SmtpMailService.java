package com.ginga.naviai.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.Optional;

@Service
public class SmtpMailService implements MailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpMailService.class);

    private JavaMailSender mailSender;

    // test helper: allow simulating failures
    private static volatile boolean simulateFailure = false;

    public static void setSimulateFailure(boolean fail) { simulateFailure = fail; }
    public static boolean isSimulateFailure() { return simulateFailure; }

    public SmtpMailService(Optional<JavaMailSender> mailSender) {
        if (mailSender == null) {
            this.mailSender = null;
        } else {
            this.mailSender = mailSender.orElse(null);
        }
    }

    // Constructor accepting JavaMailSender directly to support Mockito constructor injection
    public SmtpMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // No-arg constructor for test frameworks (Mockito @InjectMocks)
    public SmtpMailService() {
        this.mailSender = null;
    }

    @Override
    @Async("mailTaskExecutor")
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void send(String to, String subject, String body) {
        if (simulateFailure) {
            throw new RuntimeException("simulated mail failure");
        }
        if (this.mailSender == null) {
            logger.warn("JavaMailSender not configured; skipping send to {}", to);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
        logger.info("sent mail to {} subject={}", to, subject);
    }

    @Recover
    public void recover(Exception ex, String to, String subject, String body) {
        logger.error("Failed to send mail to {} after retries: {}", to, ex.getMessage());
        // In production, enqueue for manual retry or save to a failure table for later processing
    }
}
