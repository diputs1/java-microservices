package com.tungdt.microservices.identity.mapper;

import com.tungdt.microservices.identity.dto.AccountResponse;
import com.tungdt.microservices.identity.entity.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
    public AccountResponse toResponse(AccountEntity account) {
        return new AccountResponse(account.getId(), account.getUsername(), account.getEmail(), account.getRole());
    }
}
