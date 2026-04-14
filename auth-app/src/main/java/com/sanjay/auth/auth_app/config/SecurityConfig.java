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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http){

        http.csrf(csrf-> csrf.disable())
                        .cors(Customizer.withDefaults())
                        .sessionManagement(sm->sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(authorizeHttpRequest->authorizeHttpRequest
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/logout").permitAll()
                        .anyRequest().authenticated()

                )
                .oauth2Login(auth2->
                    auth2.successHandler(oAuth2SuccessHandle)
                            .failureHandler(null)
                )
                .logout(AbstractHttpConfigurer::disable)

                .exceptionHandling(ex->ex.authenticationEntryPoint((request, response, authException) -> {
                        //send error sms
                    authException.printStackTrace();
                    response.setStatus(401);
                    response.setContentType("application/json");
                    String message = "Unauthorized Access ! " + authException.getMessage();
                    String error = (String) request.getAttribute("error");
                    if (error != null)
                        message=error;

                    String massage = " Unauthorized access " + authException.getMessage();
                    Map<String , String> errorMap = Map.of("message",massage, "statusCode", Integer.toString(401));
                    var objectMapper = new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(errorMap));

    } ))
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
