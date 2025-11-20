package com.immortals.authapp.security.filters;

import com.immortals.authapp.security.exception.JwtNotFoundException;
import com.immortals.authapp.security.jwt.JwtProvider;
import com.immortals.authapp.service.RedisTokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static com.immortals.authapp.constants.AuthAppConstant.HEADER_STRING;
import static com.immortals.authapp.constants.AuthAppConstant.TOKEN_PREFIX;

@Order(2)
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final RedisTokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request,response);

            if (jwtProvider.validateToken(jwt)) {
                String username = jwtProvider.getUsernameFromToken(jwt);
                List<String> permissions = jwtProvider.getPermissionsFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                List<GrantedAuthority> combinedAuthorities = Stream.concat(
                        userDetails.getAuthorities().stream(),
                        permissions.stream().map(SimpleGrantedAuthority::new)
                ).toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, combinedAuthorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }


    private String getJwtFromRequest(HttpServletRequest request,HttpServletResponse response) throws JwtNotFoundException, IOException {
        String bearerToken = request.getHeader(HEADER_STRING);
        if (!(StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX))) {
            throw new JwtNotFoundException("Unable to Obtain JwtToken From header");
        }
        String token = bearerToken.substring(7);
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted.");
            return "";
        }
        return token;
    }
}