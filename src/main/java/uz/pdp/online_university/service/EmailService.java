package uz.pdp.online_university.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String to, String otpCode, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Online University - " + purpose);
            helper.setText(buildOtpEmailBody(otpCode, purpose), true);

            mailSender.send(message);
            log.info("OTP email sent to: {} for purpose: {}", to, purpose);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
        }
    }

    private String buildOtpEmailBody(String otpCode, String purpose) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto;">
                    <h2 style="color: #2c3e50;">Online University</h2>
                    <p>Your verification code for <strong>%s</strong>:</p>
                    <div style="background-color: #f0f0f0; padding: 20px; text-align: center;
                                border-radius: 8px; margin: 20px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px;
                                     color: #2c3e50;">%s</span>
                    </div>
                    <p style="color: #7f8c8d; font-size: 14px;">
                        This code expires in 5 minutes. Do not share it with anyone.
                    </p>
                </div>
                """.formatted(purpose, otpCode);
    }
}
