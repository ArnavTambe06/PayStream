package com.paystream.api.service;

import com.paystream.api.dto.request.LoginRequest;
import com.paystream.api.dto.request.RegisterRequest;
import com.paystream.api.dto.response.AuthResponse;
import com.paystream.api.entity.AccountType;
import com.paystream.api.entity.Role;
import com.paystream.api.entity.User;
import com.paystream.api.entity.Wallet;
import com.paystream.api.exception.PayStreamException;
import com.paystream.api.repository.UserRepository;
import com.paystream.api.repository.WalletRepository;
import com.paystream.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new PayStreamException("Email already registered", HttpStatus.CONFLICT);
        }

        // Create user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .isActive(true)
                .build();
        userRepository.save(user);

        // Auto-create a default SAVINGS wallet for every new user
        Wallet wallet = Wallet.builder()
                .user(user)
                .accountType(AccountType.SAVINGS)
                .currency("INR")
                .isActive(true)
                .build();
        walletRepository.save(wallet);

        String accessToken  = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new PayStreamException(
                        "Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new PayStreamException("Account is deactivated", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new PayStreamException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        String accessToken  = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}