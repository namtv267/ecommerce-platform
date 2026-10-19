package com.shop.auth.adapter.out.redis;

import com.shop.auth.application.port.out.TokenBlacklistPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Key: auth:blacklist:{jti} với TTL = thời gian còn lại của access token -> tự dọn, không phình Redis. */
@Component
class RedisTokenBlacklist implements TokenBlacklistPort {

    private static final String PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redis;

    RedisTokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void blacklist(String tokenId, Instant expiresAt) {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (!remaining.isNegative() && !remaining.isZero()) {
            redis.opsForValue().set(PREFIX + tokenId, "1", remaining);
        }
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + tokenId));
    }
}
