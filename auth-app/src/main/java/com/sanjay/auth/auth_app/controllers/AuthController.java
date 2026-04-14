package com.sanjay.auth.auth_app.controllers;

import com.sanjay.auth.auth_app.dtos.LoginRequest;
import com.sanjay.auth.auth_app.dtos.RefreshTokenRequest;
import com.sanjay.auth.auth_app.dtos.TokenResponse;
import com.sanjay.auth.auth_app.dtos.UserDto;
import com.sanjay.auth.auth_app.entities.RefreshToken;
import com.sanjay.auth.auth_app.entities.User;
import com.sanjay.auth.auth_app.repository.RefreshTokenRepository;
import com.sanjay.auth.auth_app.repository.UserRepository;
import com.sanjay.auth.auth_app.security.CookieService;
import com.sanjay.auth.auth_app.security.JwtService;
import com.sanjay.auth.auth_app.services.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtService jwtService;

    private final ModelMapper modelMapper;

    private final CookieService cookieService;


    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response){
        //first Authenticate user
        Authentication authenticate = authenticate(loginRequest);
        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!user.isEnabled()){
            throw new DisabledException("User is disable");
        }

        String jti = UUID.randomUUID().toString();
        var refreshTokenObject = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenObject);



        //generate access token
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        //use cookie ro attach refresh token on cookie
        cookieService.attachRefreshCookie(response,refreshToken,(int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeader(response);

        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDto.class));
        return ResponseEntity.ok(tokenResponse);

    }

    private Authentication authenticate(LoginRequest loginRequest) {
        try {
           return authenticationManager.authenticate(new  UsernamePasswordAuthenticationToken(loginRequest.email(),loginRequest.password()));
        }catch (Exception e){
            throw new BadCredentialsException("Invalid credential | username . password invalid");
        }
    }

    //Access & refresh Token renew krne ke liye
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        String refreshToken = readRequestTokenFromRequest(body, request).orElseThrow(() -> new BadCredentialsException("Refresh Token is missing"));

        if (!jwtService.isRefreshToken(refreshToken)){
            throw new BadCredentialsException("Invalid Refresh Token Type");
        }

        String jti = jwtService.getJti(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);

        RefreshToken storedRefreshToken = refreshTokenRepository.findByJti(jti).orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (storedRefreshToken.isRevoked()){
            throw new BadCredentialsException("Refresh Token expired or revoked");
        }
        if (storedRefreshToken.getExpiresAt().isBefore(Instant.now())){
            throw new BadCredentialsException("Refresh token expired");
        }
        if (!storedRefreshToken.getUser().getId().equals(userId)){
            throw new BadCredentialsException("Refresh Token does not belong to this user");
        }

        //Rotate access token !important in production
        storedRefreshToken.setRevoked(true);
        String newJti = UUID.randomUUID().toString();
        storedRefreshToken.setReplacedByToken(newJti);
        refreshTokenRepository.save(storedRefreshToken);

        User user = storedRefreshToken.getUser();

        var  newRefreshTokenObject = RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build();

        refreshTokenRepository.save(newRefreshTokenObject);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user , jti);

        cookieService.attachRefreshCookie(response,newRefreshToken,(int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeader(response);
        return ResponseEntity.ok(TokenResponse.of(newAccessToken, newRefreshToken, jwtService.getAccessTtlSeconds(), modelMapper.map(user,UserDto.class)));


    }

    //1. header
    private Optional<String> readRequestTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        //1. prefer to read refresh token from cookie first
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(cookie -> cookieService.getRefreshTokenCookieName().equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .filter(value -> !value.isBlank())
                    .findFirst();

            if (fromCookie.isPresent()) {
                return fromCookie;
            }
        }
        //2. body
        if (body!=null && body.refreshToken() != null && !body.refreshToken().isBlank()){
            return Optional.of(body.refreshToken());
        }

        //3. custom header
        String refreshHeader = request.getHeader("X-Refresh-Token");
        if(refreshHeader != null && !refreshHeader.trim().isEmpty()){
            return Optional.of(refreshHeader.trim());
        }

        //4. Authorization = Bearer <token>
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true,0,"Bearer", 0,6)){
            String candidate = authHeader.substring(7).trim();
            if (!candidate.isEmpty()){
                try {
                    if (jwtService.isRefreshToken(candidate)){
                        return Optional.of(candidate);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return Optional.empty();

    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){
        readRequestTokenFromRequest(null,request).ifPresent(token->{
            try {
                if (jwtService.isRefreshToken(token));
                String jit = jwtService.getJti(token);
                refreshTokenRepository.findByJti(jit).ifPresent(rt->{
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
            }catch (JwtException ignored){

            }
        });

        cookieService.clearRefreshToken(response);
        cookieService.addNoStoreHeader(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
