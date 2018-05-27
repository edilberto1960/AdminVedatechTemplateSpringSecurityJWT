package com.vedatech.admin.service.UserServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vedatech.admin.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {


    private AuthenticationManager authenticationManager;
    public JWTAuthenticationFilter(AuthenticationManager authenticationManager) {
        setRequiresAuthenticationRequestMatcher( new AntPathRequestMatcher("/api/login", "POST"));
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        String username = this.obtainUsername(request);
        String password = this.obtainPassword(request);
        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = "";
        }

        if(username != null && password != null){
            logger.info("username desde JWT AUTH: " + username);
            logger.info("password desde JWT AUTH: " + password);
        }

        username = username.trim();
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username,password);
        return authenticationManager.authenticate(authenticationToken);
    }



    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {




        String username = ((User) authResult.getPrincipal()).getUsername();
        Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();
        Claims claims = Jwts.claims();
        claims.put("authorities", new ObjectMapper().writeValueAsString(authorities));
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .signWith(SignatureAlgorithm.HS512,"Alguna.clave.secreta.123456")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+3600L))
                .compact();
        response.setHeader("Authorization", "Bearer "+token);
        Map<String,Object> body = new HashMap<String, Object>();
        body.put("token",token);
        body.put("user",(User) authResult.getPrincipal());
        body.put("mensaje", String.format("Hola %s, has iniciado sesion exitosa",username));

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.setStatus(200);
        response.setContentType("application/json");


    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {

        Map<String,Object> body = new HashMap<String, Object>();
        body.put("mensaje","error authentication: password or username incorrect");
        body.put("failed", failed.getMessage());
        logger.info("usuario  con credenciales incorrectas " );
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.setStatus(403);
        response.setContentType("application/json");


    }

}
