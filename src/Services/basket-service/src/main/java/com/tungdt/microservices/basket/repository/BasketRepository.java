package com.tungdt.microservices.basket.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.basket.dto.BasketResponse;
import com.tungdt.microservices.common.error.BusinessException;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class BasketRepository {
    private static final String KEY_PREFIX = "basket:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public BasketRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public BasketResponse save(BasketResponse basket) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + basket.customerId(), objectMapper.writeValueAsString(basket));
            return basket;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Cannot save basket", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Optional<BasketResponse> findByCustomerId(String customerId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + customerId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, BasketResponse.class));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Cannot read basket", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void delete(String customerId) {
        redisTemplate.delete(KEY_PREFIX + customerId);
    }
}
