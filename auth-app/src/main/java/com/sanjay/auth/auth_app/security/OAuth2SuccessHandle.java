package com.sanjay.auth.auth_app.security;

import com.sanjay.auth.auth_app.entities.Provider;
import com.sanjay.auth.auth_app.entities.User;
import com.sanjay.auth.auth_app.repository.UserRepository;
import jakarta.servlet.FilterChain;
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

@Component
@AllArgsConstructor
public class OAuth2SuccessHandle implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
            logger.info("Successful authentication");
            logger.info(authentication.toString());
//            response.getWriter().write("Login Successful");

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        //identify user

        String registrationId = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token){
            registrationId = token.getAuthorizedClientRegistrationId();
        }

        logger.info("registrationId: " + registrationId);
        assert principal != null;
        logger.info("User: " + principal.getAttributes().toString());

        User user;
        switch (registrationId){
            case "google":
                String googleId = principal.getAttributes().getOrDefault("sub","").toString();
                String emailId = principal.getAttributes().getOrDefault("email","").toString();
                String name = principal.getAttributes().getOrDefault("name","").toString();
                String picture = principal.getAttributes().getOrDefault("picture","").toString();
                user=User.builder()
                        .email(emailId)
                        .name(name)
                        .image(picture)
                        .provider(Provider.GOOGLE)
                        .build();

                userRepository.save(user);
        }

        //  username
        // User email
        // new user create
        // jwt token - token ke sath frontend pr redirect kr denge
    }
}
