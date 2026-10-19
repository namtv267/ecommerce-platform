package com.shop.auth.adapter.out.redis;

import com.shop.auth.application.port.out.RefreshTokenStorePort;
import com.shop.auth.config.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Key: auth:refresh:{sha256(token)} -> userId (có TTL).
 * Chỉ lưu hash của token: lộ Redis cũng không dùng được token.
 */
@Component
class RedisRefreshTokenStore implements RefreshTokenStorePort {

    private static final String PREFIX = "auth:refresh:";

    private final StringRedisTemplate redis;
    private final JwtProperties props;
    private final SecureRandom random = new SecureRandom();

    RedisRefreshTokenStore(StringRedisTemplate redis, JwtProperties props) {
        this.redis = redis;
        this.props = props;
    }

    @Override
    public String create(UUID userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(key(token), userId.toString(), props.refreshTokenTtl());
        return token;
    }

    @Override
    public Optional<UUID> consume(String refreshToken) {
        // GETDEL: nguyên tử -> hai request refresh đồng thời chỉ có 1 request thành công
        String value = redis.opsForValue().getAndDelete(key(refreshToken));
        return Optional.ofNullable(value).map(UUID::fromString);
    }

    @Override
    public void revoke(String refreshToken) {
        redis.delete(key(refreshToken));
    }

    private static String key(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
