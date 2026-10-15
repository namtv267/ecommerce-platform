package com.shop.order.adapter.in.web;

import com.shop.order.adapter.in.web.dto.PlaceOrderRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** Băm nội dung request đã chuẩn hoá (gộp trùng, sắp xếp) để phát hiện "cùng key nhưng khác nội dung". */
final class RequestFingerprint {
    private RequestFingerprint() {}

    static String of(PlaceOrderRequest request) {
        Map<String, Integer> normalized = new TreeMap<>();
        for (PlaceOrderRequest.ItemRequest item : request.items()) {
            normalized.merge(item.productId(), item.quantity(), Integer::sum);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
