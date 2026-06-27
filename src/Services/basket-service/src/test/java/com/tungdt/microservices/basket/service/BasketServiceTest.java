package com.tungdt.microservices.basket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tungdt.microservices.basket.dto.BasketItemRequest;
import com.tungdt.microservices.basket.dto.BasketItemResponse;
import com.tungdt.microservices.basket.dto.BasketRequest;
import com.tungdt.microservices.basket.dto.BasketResponse;
import com.tungdt.microservices.basket.entity.BasketEntity;
import com.tungdt.microservices.basket.entity.BasketItemEntity;
import com.tungdt.microservices.basket.repository.BasketRepository;
import com.tungdt.microservices.common.error.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {
    @Mock
    private BasketRepository basketRepository;

    @InjectMocks
    private BasketService basketService;

    @Test
    void saveCalculatesTotalAndPersistsMappedItems() {
        BasketRequest request = new BasketRequest("customer-1", List.of(
                new BasketItemRequest("SKU-1", "Product one", 2, new BigDecimal("10.50")),
                new BasketItemRequest("SKU-2", "Product two", 1, new BigDecimal("5.25"))
        ));
        when(basketRepository.save(any(BasketEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BasketResponse response = basketService.save(request);

        ArgumentCaptor<BasketEntity> captor = ArgumentCaptor.forClass(BasketEntity.class);
        verify(basketRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(response.customerId());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(response.totalAmount());
        assertThat(response.customerId()).isEqualTo("customer-1");
        assertThat(response.items()).containsExactly(
                new BasketItemResponse("SKU-1", "Product one", 2, new BigDecimal("10.50")),
                new BasketItemResponse("SKU-2", "Product two", 1, new BigDecimal("5.25"))
        );
        assertThat(response.totalAmount()).isEqualByComparingTo("26.25");
    }

    @Test
    void getByCustomerIdReturnsExistingBasket() {
        BasketEntity basket = basket("customer-1", List.of(), BigDecimal.ZERO);
        when(basketRepository.findByCustomerId("customer-1")).thenReturn(Optional.of(basket));

        BasketResponse response = basketService.getByCustomerId("customer-1");

        assertThat(response.customerId()).isEqualTo("customer-1");
        assertThat(response.items()).isEmpty();
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getByCustomerIdThrowsNotFoundWhenBasketDoesNotExist() {
        when(basketRepository.findByCustomerId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> basketService.getByCustomerId("missing"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("Basket not found");
                });
    }

    @Test
    void deleteDelegatesToRepository() {
        basketService.delete("customer-1");

        verify(basketRepository).delete("customer-1");
    }

    private BasketEntity basket(String customerId, List<BasketItemEntity> items, BigDecimal totalAmount) {
        BasketEntity basket = new BasketEntity();
        basket.setCustomerId(customerId);
        basket.setItems(items);
        basket.setTotalAmount(totalAmount);
        return basket;
    }
}
