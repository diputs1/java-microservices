package com.tungdt.microservices.basket.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.basket.config.BasketProperties;
import com.tungdt.microservices.basket.dto.BasketItemResponse;
import com.tungdt.microservices.basket.dto.BasketResponse;
import com.tungdt.microservices.common.error.BusinessException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BasketRepositoryTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private BasketRepository basketRepository;

    @BeforeEach
    void setUp() {
        BasketProperties basketProperties = new BasketProperties();
        basketProperties.setTtl(Duration.ofMinutes(30));
        basketRepository = new BasketRepository(redisTemplate, new ObjectMapper(), basketProperties);
    }

    @Test
    void saveSerializesBasketUnderCustomerKey() throws Exception {
        useValueOperations();
        BasketResponse basket = basket();

        BasketResponse response = basketRepository.save(basket);

        verify(valueOperations).set("basket:customer-1", new ObjectMapper().writeValueAsString(basket), Duration.ofMinutes(30));
        assertThat(response).isSameAs(basket);
    }

    @Test
    void findByCustomerIdDeserializesStoredBasket() throws Exception {
        useValueOperations();
        BasketResponse basket = basket();
        when(valueOperations.get("basket:customer-1"))
                .thenReturn(new ObjectMapper().writeValueAsString(basket));

        assertThat(basketRepository.findByCustomerId("customer-1")).contains(basket);
    }

    @Test
    void findByCustomerIdReturnsEmptyWhenKeyDoesNotExist() {
        useValueOperations();
        when(valueOperations.get("basket:missing")).thenReturn(null);

        assertThat(basketRepository.findByCustomerId("missing")).isEmpty();
    }

    @Test
    void findByCustomerIdThrowsInternalServerErrorForMalformedJson() {
        useValueOperations();
        when(valueOperations.get("basket:customer-1")).thenReturn("not-json");

        assertThatThrownBy(() -> basketRepository.findByCustomerId("customer-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("Cannot read basket");
                });
    }

    @Test
    void deleteRemovesCustomerKey() {
        basketRepository.delete("customer-1");

        verify(redisTemplate).delete("basket:customer-1");
    }

    private BasketResponse basket() {
        return new BasketResponse(
                "customer-1",
                List.of(new BasketItemResponse("SKU-1", "Product one", 2, new BigDecimal("10.50"))),
                new BigDecimal("21.00")
        );
    }

    private void useValueOperations() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
}
