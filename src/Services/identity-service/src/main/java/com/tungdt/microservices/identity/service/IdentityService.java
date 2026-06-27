package com.tungdt.microservices.identity.service;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.identity.dto.LoginRequest;
import com.tungdt.microservices.identity.dto.LoginResponse;
import com.tungdt.microservices.identity.dto.RegisterRequest;
import com.tungdt.microservices.identity.entity.AccountEntity;
import com.tungdt.microservices.identity.dto.AccountResponse;
import com.tungdt.microservices.identity.mapper.AccountMapper;
import com.tungdt.microservices.identity.repository.AccountRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountMapper accountMapper;

    public IdentityService(AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountMapper = accountMapper;
    }

    @Transactional
    public AccountResponse register(RegisterRequest request) {
        if (accountRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
        }
        if (accountRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already exists", HttpStatus.CONFLICT);
        }
        log.info("Register account username={}", request.username());
        AccountEntity account = new AccountEntity();
        account.setUsername(request.username());
        account.setEmail(request.email());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(request.role() == null || request.role().isBlank() ? "CUSTOMER" : request.role());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    public LoginResponse login(LoginRequest request) {
        AccountEntity account = accountRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("Invalid username or password", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        log.info("Login account username={}", request.username());
        return new LoginResponse(createDemoToken(account), "Bearer", accountMapper.toResponse(account));
    }

    public List<AccountResponse> getAll() {
        return accountRepository.findAll().stream().map(accountMapper::toResponse).toList();
    }

    private String createDemoToken(AccountEntity account) {
        String payload = account.getUsername() + ":" + account.getRole() + ":" + Instant.now();
        return Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

}
