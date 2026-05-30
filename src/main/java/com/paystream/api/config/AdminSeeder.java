package com.paystream.api.config;

import com.paystream.api.entity.AccountType;
import com.paystream.api.entity.Role;
import com.paystream.api.entity.User;
import com.paystream.api.entity.Wallet;
import com.paystream.api.repository.UserRepository;
import com.paystream.api.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository    userRepository;
    private final WalletRepository  walletRepository;
    private final PasswordEncoder   passwordEncoder;

    @Value("${app.admin.email}")    private String adminEmail;
    @Value("${app.admin.password}") private String adminPassword;
    @Value("${app.admin.name}")     private String adminName;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists — skipping seed");
            return;
        }

        User admin = User.builder()
                .fullName(adminName)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        userRepository.save(admin);

        Wallet adminWallet = Wallet.builder()
                .user(admin)
                .accountType(AccountType.SAVINGS)
                .currency("INR")
                .isActive(true)
                .build();
        walletRepository.save(adminWallet);

        log.info("Admin user seeded: {}", adminEmail);
    }
}