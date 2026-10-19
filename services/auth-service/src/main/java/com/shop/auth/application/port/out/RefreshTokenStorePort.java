package com.shop.auth.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStorePort {

    /** Tạo refresh token opaque, lưu kèm TTL, trả về giá trị thô để gửi cho client. */
    String create(UUID userId);

    /** Lấy và xóa token trong 1 thao tác nguyên tử -> token chỉ dùng được đúng 1 lần. */
    Optional<UUID> consume(String refreshToken);

    void revoke(String refreshToken);
}
