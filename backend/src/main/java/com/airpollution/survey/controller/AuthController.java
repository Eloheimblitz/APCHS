package com.airpollution.survey.controller;

import com.airpollution.survey.dto.LoginRequest;
import com.airpollution.survey.dto.LoginResponse;
import com.airpollution.survey.security.JwtService;
import com.airpollution.survey.security.LoginAttemptService;
import com.airpollution.survey.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        loginAttemptService.assertNotLocked(request.username());
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            loginAttemptService.recordSuccess(request.username());
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            return new LoginResponse(jwtService.generateToken(principal), principal.getUsername(), principal.getRole());
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(request.username());
            throw e;
        }
    }
}
