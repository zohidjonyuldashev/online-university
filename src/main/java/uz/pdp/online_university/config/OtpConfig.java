package uz.pdp.online_university.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.otp")
@Getter
@Setter
public class OtpConfig {

    private int length;
    private int expirationMinutes;
    private int resendCooldownSeconds;
    private int maxAttempts;
}
