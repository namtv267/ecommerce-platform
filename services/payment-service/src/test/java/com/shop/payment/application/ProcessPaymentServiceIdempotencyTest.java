package com.shop.payment.application;

import com.shop.contracts.ProcessPaymentCommand;
import com.shop.payment.adapter.out.gateway.MockPaymentGateway;
import com.shop.payment.application.port.out.PaymentRepositoryPort;
import com.shop.payment.application.port.out.PaymentResultPublisherPort;
import com.shop.payment.application.port.out.ProcessedMessagePort;
import com.shop.payment.application.port.out.TransactionPort;
import com.shop.payment.application.service.ProcessPaymentService;
import com.shop.payment.domain.exception.DuplicatePaymentException;
import com.shop.payment.domain.exception.GatewayTimeoutException;
import com.shop.payment.domain.exception.IdempotencyKeyReuseException;
import com.shop.payment.domain.model.Payment;
import com.shop.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chứng minh idempotency của payment bằng fake in-memory (không cần Spring/DB/Kafka)
 * + MockPaymentGateway thật (đếm số lần "trừ tiền thật").
 */
class ProcessPaymentServiceIdempotencyTest {

    // ---------- fakes ----------
    static class InMemoryPayments implements PaymentRepositoryPort {
        final Map<String, Payment> byKey = new ConcurrentHashMap<>();

        public Optional<Payment> findByIdempotencyKey(String key) { return Optional.ofNullable(byKey.get(key)); }

        public Optional<Payment> findByOrderId(UUID orderId) {
            return byKey.values().stream().filter(p -> p.orderId().equals(orderId)).findFirst();
        }

        public Payment insert(Payment p) {
            boolean orderTaken = findByOrderId(p.orderId()).isPresent();
            if (orderTaken || byKey.putIfAbsent(p.idempotencyKey(), p) != null) {
                throw new DuplicatePaymentException(p.idempotencyKey(), null);
            }
            return p;
        }

        public boolean updateIfPending(Payment p) {
            Payment current = byKey.get(p.idempotencyKey());
            if (current == null || !current.isPending()) return false;
            byKey.put(p.idempotencyKey(), p);
            return true;
        }
    }

    static class RecordingPublisher implements PaymentResultPublisherPort {
        final List<Payment> published = new ArrayList<>();
        public void publish(Payment payment) { published.add(payment); }
    }

    static class InMemoryProcessed implements ProcessedMessagePort {
        final Set<String> ids = new HashSet<>();
        public boolean isProcessed(String id) { return ids.contains(id); }
        public boolean markProcessed(String id) { return ids.add(id); }
    }

    static class DirectTx implements TransactionPort {
        public <T> T execute(Supplier<T> work) { return work.get(); }
    }

    // ---------- fixture ----------
    InMemoryPayments payments;
    RecordingPublisher publisher;
    MockPaymentGateway gateway;
    ProcessPaymentService service;
    UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        payments = new InMemoryPayments();
        publisher = new RecordingPublisher();
        gateway = new MockPaymentGateway(0.0, 0.0, 0);
        service = new ProcessPaymentService(payments, gateway, publisher, new InMemoryProcessed(), new DirectTx());
    }

    private ProcessPaymentCommand command(String amount) {
        return new ProcessPaymentCommand(orderId, UUID.randomUUID(), new BigDecimal(amount), "USD", "payment-" + orderId);
    }

    // ---------- tests ----------
    @Test
    void sameMessageDeliveredTwice_chargesOnce_andPublishesOnce() {
        ProcessPaymentCommand cmd = command("89.00");

        service.process("msg-1", cmd);
        service.process("msg-1", cmd); // Kafka giao lại (at-least-once)

        assertThat(gateway.chargeCount()).isEqualTo(1);
        assertThat(publisher.published).hasSize(1);
        assertThat(publisher.published.get(0).status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void sameIdempotencyKeyWithNewMessageId_doesNotChargeAgain_butReplaysTheResult() {
        ProcessPaymentCommand cmd = command("89.00");

        service.process("msg-1", cmd);
        service.process("msg-2", cmd); // order-service gửi lại lệnh (saga timeout) -> message-id mới

        assertThat(gateway.chargeCount()).isEqualTo(1);
        assertThat(publisher.published).hasSize(2);
        assertThat(publisher.published.get(1).id()).isEqualTo(publisher.published.get(0).id());
        assertThat(publisher.published.get(1).status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void gatewayTimeoutAfterCharge_retryWithSameKey_neverChargesTwice() {
        ProcessPaymentCommand cmd = command("20.14"); // .14: cổng trừ tiền xong rồi mới timeout

        assertThatThrownBy(() -> service.process("msg-1", cmd)).isInstanceOf(GatewayTimeoutException.class);
        assertThat(publisher.published).isEmpty();
        assertThat(payments.byKey.get(cmd.idempotencyKey()).status()).isEqualTo(PaymentStatus.PENDING);

        service.process("msg-1", cmd); // error handler retry cùng message

        assertThat(gateway.chargeCount()).isEqualTo(1);
        assertThat(publisher.published).hasSize(1);
        assertThat(publisher.published.get(0).status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void declinedPayment_publishesFailure_andIsAlsoIdempotent() {
        ProcessPaymentCommand cmd = command("10.13"); // .13: bị từ chối

        service.process("msg-1", cmd);
        service.process("msg-2", cmd);

        assertThat(gateway.chargeCount()).isZero();
        assertThat(publisher.published).hasSize(2);
        assertThat(publisher.published).allSatisfy(p -> {
            assertThat(p.status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(p.failureReason()).isEqualTo("INSUFFICIENT_FUNDS");
        });
    }

    @Test
    void sameKeyWithDifferentAmount_isRejected_andNotCharged() {
        service.process("msg-1", command("89.00"));

        assertThatThrownBy(() -> service.process("msg-2", command("1.00")))
                .isInstanceOf(IdempotencyKeyReuseException.class);
        assertThat(gateway.chargeCount()).isEqualTo(1);
    }

    @Test
    void amountsWithDifferentScaleAreTheSameRequest() {
        service.process("msg-1", command("89"));
        service.process("msg-2", command("89.00"));

        assertThat(gateway.chargeCount()).isEqualTo(1);
    }
}
