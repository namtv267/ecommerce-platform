package com.shop.auth.domain.model;

import com.shop.auth.domain.exception.InvalidEmailException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Value object: luôn được chuẩn hóa (trim + lowercase) và đảm bảo hợp lệ ngay khi khởi tạo. */
public record Email(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null) {
            throw new InvalidEmailException("null");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > 255 || !PATTERN.matcher(value).matches()) {
            throw new InvalidEmailException(value);
        }
    }
}
