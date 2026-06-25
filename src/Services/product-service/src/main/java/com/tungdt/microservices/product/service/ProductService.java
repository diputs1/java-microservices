package com.tungdt.microservices.product.service;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.product.dto.ProductRequest;
import com.tungdt.microservices.product.dto.ProductResponse;
import com.tungdt.microservices.product.entity.ProductEntity;
import com.tungdt.microservices.product.mapper.ProductMapper;
import com.tungdt.microservices.product.repository.ProductRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new BusinessException("Product sku already exists", HttpStatus.CONFLICT);
        }
        log.info("Create product sku={}", request.sku());
        ProductEntity product = new ProductEntity();
        productMapper.updateEntity(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream().map(productMapper::toResponse).toList();
    }

    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findById(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        ProductEntity product = findById(id);
        log.info("Update product id={}", id);
        productMapper.updateEntity(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        ProductEntity product = findById(id);
        log.info("Delete product id={}", id);
        productRepository.delete(product);
    }

    private ProductEntity findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));
    }

}
