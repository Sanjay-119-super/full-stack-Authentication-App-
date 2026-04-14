

package com.sanjay.auth.auth_app.security;

import com.sanjay.auth.auth_app.entities.Provider;
import com.sanjay.auth.auth_app.entities.RefreshToken;
import com.sanjay.auth.auth_app.entities.User;
import com.sanjay.auth.auth_app.repository.RefreshTokenRepository;
import com.sanjay.auth.auth_app.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@AllArgsConstructor
public class OAuth2SuccessHandle implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        logger.info("Successful authentication");

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        String registrationId = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();
        }

        logger.info("registrationId: {}", registrationId);

        // Find or create the user in database (managed entity)
        User user;
        switch (registrationId) {
            case "google" -> {
                String email = principal.getAttribute("email");
                String name = principal.getAttribute("name");
                String picture = principal.getAttribute("picture");

                user = userRepository.findByEmail(email)
                        .orElseGet(() -> {
                            User newUser = User.builder()
                                    .email(email)
                                    .name(name)
                                    .image(picture)
                                    .provider(Provider.GOOGLE)
                                    .enable(true)   // you may set default enabled status
                                    .build();
                            logger.info("Creating new user: {}", newUser.getEmail());
                            return userRepository.save(newUser);
                        });
                logger.info("User logged in: {}", user);
            }
            default -> throw new RuntimeException("Unsupported registrationId: " + registrationId);
        }

        // Create refresh token entry
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();

        RefreshToken refreshToken = RefreshToken.builder()
                .jti(jti)
                .user(user)   // managed user with ID
                .revoked(false)
                .createdAt(now)
                .expiresAt(now.plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build();

        refreshTokenRepository.save(refreshToken);

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshToken(user, refreshToken.getJti());

        // Attach refresh token as HTTP-only cookie
        cookieService.attachRefreshCookie(response, refreshTokenString, (int) jwtService.getRefreshTtlSeconds());

        // Optional: redirect or write response
        response.setContentType("application/json");
        response.getWriter().write("{\"message\": \"Login successful\", \"accessToken\": \"" + accessToken + "\"}");
    }
}