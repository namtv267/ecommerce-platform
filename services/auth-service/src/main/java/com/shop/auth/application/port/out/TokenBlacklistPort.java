package com.shop.auth.application.port.out;

import java.time.Instant;

public interface TokenBlacklistPort {

    /** Chặn access token (theo jti) cho đến khi nó tự hết hạn. */
    void blacklist(String tokenId, Instant expiresAt);

    boolean isBlacklisted(String tokenId);
}
