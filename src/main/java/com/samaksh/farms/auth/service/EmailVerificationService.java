package com.samaksh.farms.auth.service;

import com.samaksh.farms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailVerificationService.class);

    private final JavaMailSender mailSender;

    @Value("${app.auth.email-verification.base-url:http://localhost:5173/verify-email}")
    private String verificationBaseUrl;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendVerification(
            User user
    ) {

        String link =
                verificationBaseUrl +
                        "?token=" +
                        user.getEmailVerificationToken();

        String body =
                "Hi " + user.getName() + ",\n\n" +
                        "Your Samaksh Farms verification OTP is: " +
                        user.getEmailVerificationOtp() + "\n\n" +
                        "You can also verify using this link:\n" +
                        link + "\n\n" +
                        "After email verification, your account will go to super admin approval.";

        if (!mailEnabled) {
            LOGGER.info(
                    "Email verification for {} | OTP: {} | Link: {}",
                    user.getEmail(),
                    user.getEmailVerificationOtp(),
                    link
            );
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(user.getEmail());
            message.setSubject("Verify your Samaksh Farms email");
            message.setText(body);

            mailSender.send(message);
        } catch (Exception ex) {
            LOGGER.warn(
                    "Unable to send verification email to {}. OTP: {}, Link: {}",
                    user.getEmail(),
                    user.getEmailVerificationOtp(),
                    link,
                    ex
            );
        }
    }
}
