package com.tungdt.microservices.ordering.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tungdt.microservices.common.error.BusinessException;
import com.tungdt.microservices.ordering.client.BasketClient;
import com.tungdt.microservices.ordering.client.BasketItemResponse;
import com.tungdt.microservices.ordering.client.BasketResponse;
import com.tungdt.microservices.ordering.client.InventoryClient;
import com.tungdt.microservices.ordering.client.InventoryReservationItemRequest;
import com.tungdt.microservices.ordering.dto.OrderRequest;
import com.tungdt.microservices.ordering.dto.OrderResponse;
import com.tungdt.microservices.ordering.entity.OrderEntity;
import com.tungdt.microservices.ordering.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderSagaServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BasketClient basketClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderSagaService orderSagaService;

    @BeforeEach
    void setUp() {
        orderSagaService = new OrderSagaService(orderRepository, basketClient, inventoryClient, rabbitTemplate);
    }

    @Test
    void createOrderCompletesSagaInExpectedOrder() {
        BasketResponse basket = basket(
                List.of(new BasketItemResponse("SKU-1", "Product one", 2, new BigDecimal("10.50"))),
                "21.00"
        );
        when(basketClient.getBasket("42")).thenReturn(basket);
        stubSavedOrderWithId(100L);

        OrderResponse response = orderSagaService.createOrder(new OrderRequest(42L, new BigDecimal("21.00")));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo("COMPLETED");
        InOrder calls = inOrder(basketClient, inventoryClient, orderRepository, rabbitTemplate);
        calls.verify(basketClient).getBasket("42");
        calls.verify(inventoryClient).reserve(List.of(new InventoryReservationItemRequest("SKU-1", 2)));
        calls.verify(orderRepository).save(any(OrderEntity.class));
        calls.verify(basketClient).deleteBasket("42");
        calls.verify(rabbitTemplate).convertAndSend("order.created", "order:100");
        verify(inventoryClient, never()).release(any());
    }

    @Test
    void createOrderRejectsMismatchedTotalBeforeCallingInventory() {
        when(basketClient.getBasket("42")).thenReturn(basket(List.of(
                new BasketItemResponse("SKU-1", "Product one", 1, new BigDecimal("10.00"))
        ), "10.00"));

        assertThatThrownBy(() -> orderSagaService.createOrder(new OrderRequest(42L, new BigDecimal("9.00"))))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("Order total amount does not match basket");
                });
        verifyNoInteractions(inventoryClient, orderRepository, rabbitTemplate);
        verify(basketClient, never()).deleteBasket(any());
    }

    @Test
    void createOrderDoesNotCompensateWhenReservationFails() {
        BasketResponse basket = basket(List.of(
                new BasketItemResponse("SKU-1", "Product one", 1, new BigDecimal("10.00"))
        ), "10.00");
        when(basketClient.getBasket("42")).thenReturn(basket);
        RuntimeException reservationFailure = new RuntimeException("inventory unavailable");
        doThrow(reservationFailure).when(inventoryClient)
                .reserve(List.of(new InventoryReservationItemRequest("SKU-1", 1)));

        assertThatThrownBy(() -> orderSagaService.createOrder(new OrderRequest(42L, new BigDecimal("10.00"))))
                .isSameAs(reservationFailure);
        verify(inventoryClient, never()).release(any());
        verifyNoInteractions(orderRepository, rabbitTemplate);
        verify(basketClient, never()).deleteBasket(any());
    }

    @Test
    void createOrderReleasesInventoryAndMarksOrderFailedWhenBasketDeletionFails() {
        BasketResponse basket = basket(List.of(
                new BasketItemResponse("SKU-1", "Product one", 1, new BigDecimal("10.00"))
        ), "10.00");
        when(basketClient.getBasket("42")).thenReturn(basket);
        stubSavedOrderWithId(101L);
        RuntimeException deletionFailure = new RuntimeException("basket unavailable");
        doThrow(deletionFailure).when(basketClient).deleteBasket("42");

        assertThatThrownBy(() -> orderSagaService.createOrder(new OrderRequest(42L, new BigDecimal("10.00"))))
                .isSameAs(deletionFailure);

        List<InventoryReservationItemRequest> items = List.of(new InventoryReservationItemRequest("SKU-1", 1));
        verify(inventoryClient).release(items);
        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        assertThat(orderCaptor.getAllValues().get(1).getStatus()).isEqualTo("FAILED");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void createOrderCompensatesWhenEventPublicationFails() {
        BasketResponse basket = basket(List.of(
                new BasketItemResponse("SKU-1", "Product one", 1, new BigDecimal("10.00"))
        ), "10.00");
        when(basketClient.getBasket("42")).thenReturn(basket);
        stubSavedOrderWithId(102L);
        RuntimeException publicationFailure = new RuntimeException("rabbit unavailable");
        doThrow(publicationFailure).when(rabbitTemplate).convertAndSend("order.created", "order:102");

        assertThatThrownBy(() -> orderSagaService.createOrder(new OrderRequest(42L, new BigDecimal("10.00"))))
                .isSameAs(publicationFailure);

        verify(inventoryClient).release(List.of(new InventoryReservationItemRequest("SKU-1", 1)));
        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        assertThat(orderCaptor.getAllValues().get(1).getStatus()).isEqualTo("FAILED");
    }

    @Test
    void createOrderCombinesDuplicateSkusBeforeReservation() {
        BasketResponse basket = basket(List.of(
                new BasketItemResponse("SKU-1", "Product one", 2, new BigDecimal("5.00")),
                new BasketItemResponse("SKU-1", "Product one", 3, new BigDecimal("5.00"))
        ), "25.00");
        when(basketClient.getBasket("42")).thenReturn(basket);
        stubSavedOrderWithId(103L);

        orderSagaService.createOrder(new OrderRequest(42L, new BigDecimal("25.00")));

        verify(inventoryClient).reserve(List.of(new InventoryReservationItemRequest("SKU-1", 5)));
    }

    private BasketResponse basket(List<BasketItemResponse> items, String total) {
        return new BasketResponse("42", items, new BigDecimal(total));
    }

    private void stubSavedOrderWithId(long id) {
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", id);
            return order;
        });
    }
}
