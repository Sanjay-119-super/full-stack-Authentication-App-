package com.sanjay.auth.auth_app.security;

import com.sanjay.auth.auth_app.entities.User;
import com.sanjay.auth.auth_app.repository.UserRepository;
import com.sanjay.auth.auth_app.utills.UserHelper;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService service;

    private final UserRepository userRepository;

    private final Logger logger= LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        logger.info("Authorization header :  {}",header);

        final String authorizationHeader = request.getHeader("Authorization");

        // check is header not null or start with Bearer
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){

            String token = authorizationHeader.substring(7);
           //checks for user token access
            if (!service.isAccessToken(token)){
                filterChain.doFilter(request,response);
                return;
            }

            try {
                Jws<Claims> parsed = service.parse(token);
                Claims payload = parsed.getPayload();


                String userId = payload.getSubject();
                UUID userUuid = UserHelper.parseUUID(userId);

                userRepository.findById(userUuid).ifPresent((User user)->{
                  //check for user enable or not

                    if (user.isEnabled()) {

                        List<GrantedAuthority> authorityList = user.getRoles() == null ? List.of() : user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user.getEmail(),
                                        null,
                                        authorityList
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        /*Important*/
                        if (SecurityContextHolder.getContext().getAuthentication() == null)
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                        });


            }catch (ExpiredJwtException e){
                e.printStackTrace();
            }catch (MalformedJwtException e){
                e.printStackTrace();
            }catch (JwtException e){
                e.printStackTrace();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        filterChain.doFilter(request,response); // req - not correct
    }
}
