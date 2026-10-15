package com.shop.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Aggregate root của bounded context "Identity". Không phụ thuộc framework nào. */
public final class User {

    private final UUID id;
    private final Email email;
    private final String passwordHash;
    private final String fullName;
    private final Role role;
    private final Instant createdAt;

    private User(UUID id, Email email, String passwordHash, String fullName, Role role, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.fullName = Objects.requireNonNull(fullName);
        this.role = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Đăng ký mới: luôn là CUSTOMER. */
    public static User register(Email email, String passwordHash, String fullName) {
        return new User(UUID.randomUUID(), email, passwordHash, fullName.trim(), Role.CUSTOMER, Instant.now());
    }

    /** Dựng lại aggregate từ dữ liệu đã lưu (dùng bởi persistence adapter). */
    public static User reconstitute(UUID id, Email email, String passwordHash, String fullName,
                                    Role role, Instant createdAt) {
        return new User(id, email, passwordHash, fullName, role, createdAt);
    }

    public UUID id() { return id; }
    public Email email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String fullName() { return fullName; }
    public Role role() { return role; }
    public Instant createdAt() { return createdAt; }
}
