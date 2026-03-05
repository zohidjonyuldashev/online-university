package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.dto.request.*;
import uz.pdp.online_university.dto.response.AuthResponse;
import uz.pdp.online_university.dto.response.MessageResponse;
import uz.pdp.online_university.dto.response.UserResponse;
import uz.pdp.online_university.entity.Role;
import uz.pdp.online_university.entity.User;
import uz.pdp.online_university.enums.OtpType;
import uz.pdp.online_university.enums.RoleName;
import uz.pdp.online_university.exception.AuthenticationFailedException;
import uz.pdp.online_university.exception.DuplicateResourceException;
import uz.pdp.online_university.exception.InvalidOperationException;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.RoleRepository;
import uz.pdp.online_university.repository.UserRepository;
import uz.pdp.online_university.security.CustomUserDetails;
import uz.pdp.online_university.security.JwtTokenProvider;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;

    @Transactional
    public MessageResponse register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidOperationException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("User", "phone", request.getPhone());
        }

        Role applicantRole = roleRepository.findByName(RoleName.APPLICANT)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.APPLICANT));

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .roles(Set.of(applicantRole))
                .build();

        user = userRepository.save(user);

        otpService.generateAndSend(user, OtpType.EMAIL_VERIFICATION);

        log.info("New applicant registered: {} ({}). Verification email sent.",
                user.getEmail(), user.getId());

        return MessageResponse.builder()
                .message("Registration successful. A verification code has been sent to " + user.getEmail())
                .build();
    }

    public MessageResponse verifyEmail(VerifyEmailRequest request) {

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (user.isEmailVerified()) {
            throw new InvalidOperationException("Email is already verified");
        }

        String error = otpService.verifyAndReturnError(user, request.getCode(), OtpType.EMAIL_VERIFICATION);
        if (error != null) {
            throw new InvalidOperationException(error);
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified for user: {} ({})", user.getEmail(), user.getId());

        return MessageResponse.builder()
                .message("Email verified successfully. You can now login.")
                .build();
    }

    @Transactional
    public MessageResponse resendOtp(ResendOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (user.isEmailVerified()) {
            throw new InvalidOperationException("Email is already verified");
        }

        otpService.generateAndSend(user, OtpType.EMAIL_VERIFICATION);

        return MessageResponse.builder()
                .message("A new verification code has been sent to " + user.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (!user.isEmailVerified()) {
            throw new InvalidOperationException(
                    "Email not verified. Please check your inbox or request a new verification code."
            );
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        user.incrementTokenVersion();
        user = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        log.info("User logged in: {} ({})", user.getEmail(), user.getId());

        return buildAuthResponse(userDetails, user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationFailedException("Invalid or expired refresh token");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new AuthenticationFailedException("Provided token is not a refresh token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Long tokenVersion = jwtTokenProvider.getTokenVersion(refreshToken);

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!tokenVersion.equals(user.getTokenVersion())) {
            throw new AuthenticationFailedException(
                    "Refresh token has been invalidated. Please login again."
            );
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);

        if (!userDetails.isEnabled()) {
            throw new AuthenticationFailedException("Account is deactivated");
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new AuthenticationFailedException("Account is locked");
        }

        user.incrementTokenVersion();
        user = userRepository.save(user);

        log.info("Token refreshed for user: {} ({})", user.getEmail(), user.getId());

        return buildAuthResponse(new CustomUserDetails(user), user);
    }

    private AuthResponse buildAuthResponse(CustomUserDetails userDetails, User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails, user.getTokenVersion());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getTokenVersion());

        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .accessState(user.getAccessState().name())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }
}