package com.tungdt.microservices.product.service;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.product.dto.ProductRequest;
import com.tungdt.microservices.product.dto.ProductResponse;
import com.tungdt.microservices.product.entity.ProductEntity;
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

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new BusinessException("Product sku already exists", HttpStatus.CONFLICT);
        }
        log.info("Create product sku={}", request.sku());
        ProductEntity product = new ProductEntity();
        apply(request, product);
        return toResponse(productRepository.save(product));
    }

    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ProductResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        ProductEntity product = findById(id);
        log.info("Update product id={}", id);
        apply(request, product);
        return toResponse(productRepository.save(product));
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

    private void apply(ProductRequest request, ProductEntity product) {
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
    }

    private ProductResponse toResponse(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity()
        );
    }
}
