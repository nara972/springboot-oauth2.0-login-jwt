package com.example.springsocial.security.jwt;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.example.springsocial.model.User;
import com.example.springsocial.repository.UserRepository;
import com.example.springsocial.security.UserPrincipal;

import io.jsonwebtoken.Claims;

public class JwtCommonAuthorizationFilter extends BasicAuthenticationFilter {
	
    private UserRepository userRepository;
	private JwtTokenProvider jwtTokenProvider;
	
	
    public JwtCommonAuthorizationFilter(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        super(authenticationManager);
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Read the Authorization header, where the JWT token should be
        String header = request.getHeader("Authorization");
        
        // If header does not contain BEARER or is null delegate to Spring impl and exit
        if (header == null || !header.startsWith("Bearer")) {
            chain.doFilter(request, response);
            return;
        }
        // If header is present, try grab user principal from database and perform authorization
        Authentication authentication = getUsernamePasswordAuthentication(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Continue filter execution
        chain.doFilter(request, response);
    }

    private Authentication getUsernamePasswordAuthentication(HttpServletRequest request) {
        String token = request.getHeader("Authorization")
                .replace("Bearer","");

        if (token != null) {
        	Claims claims = jwtTokenProvider.getClaims(token);
        	Long id = Long.parseLong(claims.getSubject()); // getSubject ?????? users??? id???
        	
            if (id != null) {
                Optional<User> oUser = userRepository.findById(id);
                User user = oUser.get();
                UserPrincipal principal = UserPrincipal.create(user);
                
                // OAuth ?????? ?????? ??????????????? ????????? ????????? ??????. ???????????? password??? Authentication??? ?????? ????????? ?????????!!
                // JWT??? ????????? ??????????????? ???????????? ????????? ?????????. (OAuth2.0?????? ?????? ?????? ????????? ??????)

                UsernamePasswordAuthenticationToken authentication = 
                		new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication); // ????????? ??????
                return authentication;
            }
        }
        return null;
    }
    
}