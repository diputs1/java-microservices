package com.tungdt.microservices.basket.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tungdt.microservices.basket.config.BasketProperties;
import com.tungdt.microservices.basket.entity.BasketEntity;
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
    private final BasketProperties basketProperties;

    public BasketRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, BasketProperties basketProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.basketProperties = basketProperties;
    }

    public BasketEntity save(BasketEntity basket) {
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + basket.getCustomerId(),
                    objectMapper.writeValueAsString(basket),
                    basketProperties.getTtl()
            );
            return basket;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Cannot save basket", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Optional<BasketEntity> findByCustomerId(String customerId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + customerId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, BasketEntity.class));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Cannot read basket", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void delete(String customerId) {
        redisTemplate.delete(KEY_PREFIX + customerId);
    }
}
