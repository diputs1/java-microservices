package com.tungdt.microservices.customer.service;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.common.security.ResourceAccessGuard;
import com.tungdt.microservices.customer.dto.CustomerRequest;
import com.tungdt.microservices.customer.dto.CustomerResponse;
import com.tungdt.microservices.customer.entity.CustomerEntity;
import com.tungdt.microservices.customer.mapper.CustomerMapper;
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
    private final CustomerMapper customerMapper;
    private final ResourceAccessGuard resourceAccessGuard;

    public CustomerService(CustomerRepository customerRepository,
            CustomerMapper customerMapper,
            ResourceAccessGuard resourceAccessGuard) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.resourceAccessGuard = resourceAccessGuard;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new BusinessException("Customer email already exists", HttpStatus.CONFLICT);
        }
        log.info("Create customer email={}", request.email());
        CustomerEntity customer = new CustomerEntity();
        customerMapper.updateEntity(request, customer);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream().map(customerMapper::toResponse).toList();
    }

    public CustomerResponse getById(Long id) {
        assertCanAccessCustomer(id);
        return customerMapper.toResponse(findById(id));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        assertCanAccessCustomer(id);
        CustomerEntity customer = findById(id);
        log.info("Update customer id={}", id);
        customerMapper.updateEntity(request, customer);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        assertCanAccessCustomer(id);
        CustomerEntity customer = findById(id);
        log.info("Delete customer id={}", id);
        customerRepository.delete(customer);
    }

    private CustomerEntity findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Customer not found", HttpStatus.NOT_FOUND));
    }

    private void assertCanAccessCustomer(Long id) {
        if (!resourceAccessGuard.canAccessOwner(String.valueOf(id))) {
            throw new BusinessException("Access denied for customer resource", HttpStatus.FORBIDDEN);
        }
    }

}
