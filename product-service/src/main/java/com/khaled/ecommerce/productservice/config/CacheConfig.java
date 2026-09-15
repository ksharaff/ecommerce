package com.khaled.ecommerce.productservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

@Configuration
@EnableCaching // without this, every @Cacheable annotation below is silently ignored - easy to forget, hard to debug
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        // Default Java serialization writes unreadable binary into Redis. JSON means you can
        // actually inspect cached values with redis-cli while debugging.
        // Spring Boot 4 uses Jackson 3 (tools.jackson), which handles Instant/LocalDateTime natively -
        // no JavaTimeModule needed.

        // Default typing writes an "@class" property into the JSON so it deserializes back into a
        // Product instead of a LinkedHashMap. The validator restricts which classes may be named
        // there - never allow arbitrary types, that's a deserialization-attack vector.
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.khaled.ecommerce.productservice.")
                .allowIfSubType("java.")
                .build();

        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues() // don't cache "not found" - otherwise a product created a second later stays invisible
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
