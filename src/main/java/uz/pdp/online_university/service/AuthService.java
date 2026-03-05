package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.dto.request.LoginRequest;
import uz.pdp.online_university.dto.request.RefreshTokenRequest;
import uz.pdp.online_university.dto.request.RegisterRequest;
import uz.pdp.online_university.dto.response.AuthResponse;
import uz.pdp.online_university.dto.response.UserResponse;
import uz.pdp.online_university.entity.Role;
import uz.pdp.online_university.entity.User;
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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidOperationException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
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
                .roles(Set.of(applicantRole))
                .build();

        user = userRepository.save(user);

        log.info("New applicant registered: {} ({})", user.getEmail(), user.getId());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        return buildAuthResponse(authentication, user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        log.info("User logged in: {} ({})", user.getEmail(), user.getId());

        return buildAuthResponse(authentication, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationFailedException("Invalid or expired refresh token");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new AuthenticationFailedException("Provided token is not a refresh token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        if (!userDetails.isEnabled()) {
            throw new AuthenticationFailedException("Account is deactivated");
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new AuthenticationFailedException("Account is locked");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );

        log.info("Token refreshed for user: {} ({})", user.getEmail(), user.getId());

        return buildAuthResponse(authentication, user);
    }

    private AuthResponse buildAuthResponse(Authentication authentication, User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

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
