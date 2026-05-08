package ru.mirea.repair.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.mirea.repair.dto.AuthResponse;
import ru.mirea.repair.dto.LoginRequest;
import ru.mirea.repair.dto.RegisterRequest;
import ru.mirea.repair.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
