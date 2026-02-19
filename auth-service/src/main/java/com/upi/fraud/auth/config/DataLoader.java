package com.upi.fraud.auth.config;

import com.upi.fraud.auth.entity.User;
import com.upi.fraud.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedAdmin() {
        return args -> {
            if (userRepository.findByUpiId("admin@upi").isEmpty()) {
                User admin = User.builder()
                        .name("Admin User")
                        .upiId("admin@upi")
                        .password(passwordEncoder.encode("admin123"))
                        .role(User.Role.ADMIN)
                        .accountStatus(User.AccountStatus.ACTIVE)
                        .build();
                userRepository.save(admin);
            }
        };
    }
}
