package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.config.OtpConfig;
import uz.pdp.online_university.entity.OtpVerification;
import uz.pdp.online_university.entity.User;
import uz.pdp.online_university.enums.OtpType;
import uz.pdp.online_university.exception.InvalidOperationException;
import uz.pdp.online_university.repository.OtpVerificationRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final OtpConfig otpConfig;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void generateAndSend(User user, OtpType type) {

        Optional<OtpVerification> latestOtp = otpRepository
                .findTopByUserIdAndTypeAndVerifiedFalseOrderByCreatedAtDesc(user.getId(), type);

        if (latestOtp.isPresent()) {
            OtpVerification existing = latestOtp.get();
            LocalDateTime cooldownEnd = existing.getCreatedAt()
                    .plusSeconds(otpConfig.getResendCooldownSeconds());

            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                throw new InvalidOperationException(
                        "Please wait before requesting a new code. Try again after "
                                + otpConfig.getResendCooldownSeconds() + " seconds."
                );
            }

            existing.setVerified(true);
            otpRepository.save(existing);
        }

        String code = generateCode();

        OtpVerification otp = OtpVerification.builder()
                .user(user)
                .code(code)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(otpConfig.getExpirationMinutes()))
                .build();

        otpRepository.save(otp);

        String purpose = type == OtpType.EMAIL_VERIFICATION
                ? "Email Verification"
                : "Password Reset";

        emailService.sendOtpEmail(user.getEmail(), code, purpose);

        log.info("OTP generated for user {} (type: {})", user.getEmail(), type);
    }

    /**
     * Verifies the OTP code.
     * Returns null if successful, or an error message if failed.
     * This method always commits the attempt count regardless of success/failure.
     */
    @Transactional
    public String verifyAndReturnError(User user, String code, OtpType type) {

        OtpVerification otp = otpRepository
                .findTopByUserIdAndTypeAndVerifiedFalseOrderByCreatedAtDesc(user.getId(), type)
                .orElse(null);

        if (otp == null) {
            return "No pending verification code found. Please request a new one.";
        }

        if (otp.getAttempts() >= otpConfig.getMaxAttempts()) {
            return "Maximum verification attempts exceeded. Please request a new code.";
        }

        if (otp.isExpired()) {
            return "Verification code has expired. Please request a new one.";
        }

        otp.incrementAttempts();

        if (!otp.getCode().equals(code)) {
            otpRepository.save(otp);
            int remaining = otpConfig.getMaxAttempts() - otp.getAttempts();
            return "Invalid verification code. " + remaining + " attempt(s) remaining.";
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        log.info("OTP verified for user {} (type: {})", user.getEmail(), type);
        return null;
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, otpConfig.getLength());
        int code = RANDOM.nextInt(bound);
        return String.format("%0" + otpConfig.getLength() + "d", code);
    }
}
