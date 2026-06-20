package ru.mirea.repair.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mirea.repair.dto.AuthResponse;
import ru.mirea.repair.dto.LoginRequest;
import ru.mirea.repair.dto.RegisterRequest;
import ru.mirea.repair.entity.Role;
import ru.mirea.repair.entity.User;
import ru.mirea.repair.exception.ApiException;
import ru.mirea.repair.repository.UserRepository;
import ru.mirea.repair.security.JwtService;
import ru.mirea.repair.security.UserPrincipal;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("user@example.com", "Иван Петров", "password123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_createsUserWithUserRoleAndReturnsToken() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        RegisterRequest request = new RegisterRequest("new@example.com", "Новый Пользователь", "password123");
        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void login_propagatesAuthenticationFailure() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        User user = new User();
        user.setId(2L);
        user.setEmail("user@example.com");
        user.setFullName("Иван Петров");
        user.setRole(Role.USER);
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest("user@example.com", "password123");
        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("user@example.com");
    }
}
