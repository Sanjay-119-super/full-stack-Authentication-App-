package com.sanjay.auth.auth_app.config;

import com.sanjay.auth.auth_app.security.JwtAuthenticationFilter;
import com.sanjay.auth.auth_app.security.OAuth2SuccessHandle;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter authenticationFilter;

    private final OAuth2SuccessHandle oAuth2SuccessHandle;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Tera existing APIs
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/logout").permitAll()

                        // 🔥 YEH ADD KAR — OAuth2 ke liye zaroori!
                        .requestMatchers("/login/oauth2/code/*").permitAll()   // Google callback
                        .requestMatchers("/oauth2/authorization/*").permitAll() // OAuth2 start
                        .requestMatchers("/login").permitAll()                  // Error redirect

                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandle)
                        // 🔥 NULL mat daal! Ya remove kar ya proper handler daal
                        .failureHandler((request, response, exception) -> {
                            // Simple failure handling
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"message\": \"OAuth2 Login Failed: " + exception.getMessage() + "\"}"
                            );
                        })
                )

                .logout(AbstractHttpConfigurer::disable)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            String error = (String) request.getAttribute("error");
                            String message = error != null ? error : "Unauthorized: " + authException.getMessage();
                            Map<String, String> errorMap = Map.of(
                                    "message", message,
                                    "statusCode", "401"
                            );
                            new ObjectMapper().writeValue(response.getWriter(), errorMap);
                        })
                )
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration){
        return configuration.getAuthenticationManager();
    }

    /*@Bean
    public UserDetailsService users(){
        *//*Only For Learning purpose HAHA*//*
        User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();

        UserDetails user1 = userBuilder.username("Sanjay")
                .password("password1234")
                .roles("ADMIN")
                .build();

        UserDetails user2 = userBuilder.username("Demo")
                .password("demo1234")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user1,user2);
    }*/
}
