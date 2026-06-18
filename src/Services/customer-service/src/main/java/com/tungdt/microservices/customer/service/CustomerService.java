package com.tungdt.microservices.customer.service;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.customer.dto.CustomerRequest;
import com.tungdt.microservices.customer.dto.CustomerResponse;
import com.tungdt.microservices.customer.entity.CustomerEntity;
import com.tungdt.microservices.customer.repository.CustomerRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new BusinessException("Customer email already exists", HttpStatus.CONFLICT);
        }
        log.info("Create customer email={}", request.email());
        CustomerEntity customer = new CustomerEntity();
        apply(request, customer);
        return toResponse(customerRepository.save(customer));
    }

    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CustomerResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        CustomerEntity customer = findById(id);
        log.info("Update customer id={}", id);
        apply(request, customer);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        CustomerEntity customer = findById(id);
        log.info("Delete customer id={}", id);
        customerRepository.delete(customer);
    }

    private CustomerEntity findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found", HttpStatus.NOT_FOUND));
    }

    private void apply(CustomerRequest request, CustomerEntity customer) {
        customer.setEmail(request.email());
        customer.setFullName(request.fullName());
        customer.setPhone(request.phone());
    }

    private CustomerResponse toResponse(CustomerEntity customer) {
        return new CustomerResponse(customer.getId(), customer.getEmail(), customer.getFullName(), customer.getPhone());
    }
}
