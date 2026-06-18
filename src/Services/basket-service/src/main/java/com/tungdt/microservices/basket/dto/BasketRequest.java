package com.tungdt.microservices.basket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BasketRequest(
        @NotBlank @Size(max = 80) String customerId,
        @NotEmpty List<@Valid BasketItemRequest> items
) {
}
