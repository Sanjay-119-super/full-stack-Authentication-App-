package com.sanjay.auth.auth_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http){

        http.csrf(csrf-> csrf.disable());
        http.authorizeHttpRequests(authorizeHttpRequest->
                authorizeHttpRequest.requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated()

                )
                .httpBasic(Customizer.withDefaults());


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
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
