package com.tungdt.microservices.customer.mapper;

import com.tungdt.microservices.customer.dto.CustomerRequest;
import com.tungdt.microservices.customer.dto.CustomerResponse;
import com.tungdt.microservices.customer.entity.CustomerEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {
    public void updateEntity(CustomerRequest request, CustomerEntity customer) {
        customer.setEmail(request.email());
        customer.setFullName(request.fullName());
        customer.setPhone(request.phone());
    }

    public CustomerResponse toResponse(CustomerEntity customer) {
        return new CustomerResponse(customer.getId(), customer.getEmail(), customer.getFullName(), customer.getPhone());
    }
}
