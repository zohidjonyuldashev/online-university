package uz.pdp.online_university.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import uz.pdp.online_university.audit.RequestIdFilter;
import uz.pdp.online_university.exception.ErrorResponse;
import uz.pdp.online_university.security.CustomUserDetailsService;
import uz.pdp.online_university.security.JwtAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CustomUserDetailsService userDetailsService;
        private final ObjectMapper objectMapper;
        private final RequestIdFilter requestIdFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/api-docs/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                                                .anyRequest().authenticated())
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .exceptionHandling(exceptions -> exceptions
                                                // 401 — not authenticated
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        ErrorResponse errorResponse = ErrorResponse.builder()
                                                                        .status(HttpStatus.UNAUTHORIZED.value())
                                                                        .error(HttpStatus.UNAUTHORIZED
                                                                                        .getReasonPhrase())
                                                                        .message("Authentication required. Please provide a valid token.")
                                                                        .path(request.getRequestURI())
                                                                        .timestamp(LocalDateTime.now())
                                                                        .build();

                                                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        objectMapper.writeValue(response.getWriter(), errorResponse);
                                                })
                                                // 403 — authenticated but insufficient permissions
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        ErrorResponse errorResponse = ErrorResponse.builder()
                                                                        .status(HttpStatus.FORBIDDEN.value())
                                                                        .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                                                                        .message("You do not have permission to access this resource.")
                                                                        .path(request.getRequestURI())
                                                                        .timestamp(LocalDateTime.now())
                                                                        .build();

                                                        response.setStatus(HttpStatus.FORBIDDEN.value());
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        objectMapper.writeValue(response.getWriter(), errorResponse);
                                                }));

                return http.build();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
