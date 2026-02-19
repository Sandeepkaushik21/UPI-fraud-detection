package com.upi.fraud.auth.service;

import com.upi.fraud.auth.dto.AuthResponse;
import com.upi.fraud.auth.dto.LoginRequest;
import com.upi.fraud.auth.dto.RegisterRequest;
import com.upi.fraud.auth.entity.User;
import com.upi.fraud.auth.repository.UserRepository;
import com.upi.fraud.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${account.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${account.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUpiId(request.getUpiId())) {
            throw new IllegalArgumentException("UPI ID already registered");
        }
        User user = User.builder()
                .name(request.getName())
                .upiId(request.getUpiId())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getUpiId(), user.getRole().name(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .upiId(user.getUpiId())
                .name(user.getName())
                .role(user.getRole().name())
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUpiId(request.getUpiId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid UPI ID or password"));

        if (user.getAccountStatus() == User.AccountStatus.LOCKED) {
            if (user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil())) {
                throw new IllegalStateException("Account locked. Try again after " + user.getLockedUntil());
            }
            user.setAccountStatus(User.AccountStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.setAccountStatus(User.AccountStatus.LOCKED);
                user.setLockedUntil(Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(lockDurationMinutes)));
            }
            userRepository.save(user);
            throw new IllegalArgumentException("Invalid UPI ID or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUpiId(), user.getRole().name(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .upiId(user.getUpiId())
                .name(user.getName())
                .role(user.getRole().name())
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }
}
