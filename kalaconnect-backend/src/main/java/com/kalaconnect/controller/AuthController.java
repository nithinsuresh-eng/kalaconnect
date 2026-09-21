package com.kalaconnect.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.kalaconnect.model.User;
import com.kalaconnect.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // JWT service would be injected here in a full implementation

    /**
     * POST /api/auth/register
     * Registers a new Customer or Artist.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Email already registered"));
        }

        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setPhone(req.phone());
        user.setRole(User.Role.valueOf(req.role().toUpperCase()));
        user.setDeviceType(
                "BUTTON_PHONE".equalsIgnoreCase(req.deviceType())
                ? User.DeviceType.BUTTON_PHONE
                : User.DeviceType.SMARTPHONE
        );

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "userId",  user.getId(),
                "role",    user.getRole()
        ));
    }

    /**
     * POST /api/auth/login
     * Authenticates user and returns a JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.email());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        // In production: generate and return JWT token
        // String token = jwtService.generateToken(user);
        return ResponseEntity.ok(Map.of(
                "message",  "Login successful",
                "userId",   user.getId(),
                "name",     user.getName(),
                "role",     user.getRole(),
                "deviceType", user.getDeviceType(),
                "token",    "JWT_TOKEN_PLACEHOLDER"   // replace with real JWT
        ));
    }

    // ─── DTOs ────────────────────────────────────────────────
    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6) String password,
            @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
            String phone,
            @NotBlank String role,         // CUSTOMER | ARTIST
            String deviceType              // SMARTPHONE | BUTTON_PHONE
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}
}
