package edu.arizona.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.from:no-reply@coolengineeringgames.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /**
     * Sent in the background so the caller's response time does not reveal whether the address
     * belongs to an account. A mail failure is logged, never surfaced to the caller.
     */
    @Async
    public void sendPasswordReset(String to, String username, String resetLink, long validMinutes) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("No mail server configured; password reset email was not sent");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Reset your Cool Engineering Games password");
        message.setText("Hi " + username + ",\n\n"
                + "We received a request to reset your password. Use the link below within "
                + validMinutes + " minutes:\n\n"
                + resetLink + "\n\n"
                + "If you did not ask for this, you can ignore this email and your password stays the same.\n");

        try {
            sender.send(message);
        } catch (MailException e) {
            log.warn("Could not send password reset email: {}", e.getMessage());
        }
    }
}
