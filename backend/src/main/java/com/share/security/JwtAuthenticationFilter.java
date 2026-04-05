package com.share.security;

import com.share.entity.User;
import com.share.service.UserService;
import com.share.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_ACCOUNT_BLOCKED = "accountBlocked";
    public static final String ATTR_ACCOUNT_BLOCK_REASON = "accountBlockedReason";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (token != null && jwtUtil.validateToken(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.findByUsername(username);

            if (user != null && UserStatusCodes.isUsable(user.getStatus())) {
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("USER"));
                if (user.getId() != null) {
                    List<String> permissions = userService.getPermissionCodesByUserId(user.getId());
                    if (permissions != null) {
                        for (String permission : permissions) {
                            if (permission == null || permission.isBlank()) {
                                continue;
                            }
                            authorities.add(new SimpleGrantedAuthority(permission));
                        }
                    }
                }

                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else if (user != null && UserStatusCodes.isBanned(user.getStatus())) {
                request.setAttribute(ATTR_ACCOUNT_BLOCKED, Boolean.TRUE);
                if (user.getBanReason() != null && !user.getBanReason().isBlank()) {
                    request.setAttribute(ATTR_ACCOUNT_BLOCK_REASON, user.getBanReason().trim());
                }
            }
        }

        chain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
