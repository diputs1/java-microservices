package com.tungdt.microservices.product.mapper;

import com.tungdt.microservices.product.dto.ProductRequest;
import com.tungdt.microservices.product.dto.ProductResponse;
import com.tungdt.microservices.product.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public void updateEntity(ProductRequest request, ProductEntity product) {
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
    }

    public ProductResponse toResponse(ProductEntity product) {
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
