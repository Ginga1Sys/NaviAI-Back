package com.ginga.naviai.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SmtpMailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private SmtpMailService smtpMailService;

    @Test
    void send_invokesJavaMailSender() {
        // SmtpMailService が JavaMailSender.send を呼び、SimpleMailMessage の内容が正しいことを検証する
        smtpMailService.send("to@ginga.info", "subj", "body text");
        ArgumentCaptor<SimpleMailMessage> cap = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(cap.capture());
        SimpleMailMessage msg = cap.getValue();
        assertEquals("to@ginga.info", msg.getTo()[0]);
        assertEquals("subj", msg.getSubject());
        assertEquals("body text", msg.getText());
    }

    @Test
    void send_whenJavaMailSenderThrows_propagates() {
        // JavaMailSender.send が例外を投げた場合、その例外が SmtpMailService.send を通じて伝播することを検証する
        doThrow(new org.springframework.mail.MailSendException("fail")).when(javaMailSender).send(any(SimpleMailMessage.class));
        assertThrows(org.springframework.mail.MailSendException.class, () -> smtpMailService.send("x@ginga.info", "s", "b"));
    }

    @Test
    void send_simulateFailure_throwsRuntime() {
        // テスト用フラグ `simulateFailure` が有効な場合に SmtpMailService.send が RuntimeException を投げることを検証する
        try {
            SmtpMailService.setSimulateFailure(true);
            assertThrows(RuntimeException.class, () -> smtpMailService.send("y@ginga.info", "s", "b"));
        } finally {
            SmtpMailService.setSimulateFailure(false);
        }
    }
}
