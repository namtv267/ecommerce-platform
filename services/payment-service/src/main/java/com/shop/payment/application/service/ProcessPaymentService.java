package com.shop.payment.application.service;

import com.shop.contracts.ProcessPaymentCommand;
import com.shop.payment.application.port.in.GetPaymentUseCase;
import com.shop.payment.application.port.in.ProcessPaymentUseCase;
import com.shop.payment.application.port.out.PaymentGatewayPort;
import com.shop.payment.application.port.out.PaymentGatewayPort.GatewayChargeRequest;
import com.shop.payment.application.port.out.PaymentGatewayPort.GatewayResult;
import com.shop.payment.application.port.out.PaymentRepositoryPort;
import com.shop.payment.application.port.out.PaymentResultPublisherPort;
import com.shop.payment.application.port.out.ProcessedMessagePort;
import com.shop.payment.application.port.out.TransactionPort;
import com.shop.payment.domain.exception.DuplicatePaymentException;
import com.shop.payment.domain.exception.IdempotencyKeyReuseException;
import com.shop.payment.domain.exception.PaymentNotFoundException;
import com.shop.payment.domain.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Xử lý thanh toán với idempotency 4 lớp:
 *  1. Inbox (processed_messages)      : cùng message-id Kafka giao lại  -> bỏ qua.
 *  2. Unique idempotency_key / order  : lệnh gửi lại với message-id mới -> KHÔNG gọi cổng lần nữa nếu đã có kết quả,
 *                                       chỉ phát lại kết quả cũ.
 *  3. Idempotency key gửi sang cổng   : crash/timeout giữa chừng, retry vẫn không trừ tiền 2 lần.
 *  4. UPDATE ... WHERE status=PENDING : hai luồng cùng quyết định kết quả thì chỉ một bên thắng.
 *
 * Không giữ transaction DB trong lúc gọi cổng thanh toán (I/O chậm): payment PENDING được commit trước,
 * kết quả + outbox + inbox được commit cùng nhau sau.
 */
public class ProcessPaymentService implements ProcessPaymentUseCase, GetPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentService.class);

    private final PaymentRepositoryPort payments;
    private final PaymentGatewayPort gateway;
    private final PaymentResultPublisherPort publisher;
    private final ProcessedMessagePort processedMessages;
    private final TransactionPort tx;

    public ProcessPaymentService(PaymentRepositoryPort payments, PaymentGatewayPort gateway,
                                 PaymentResultPublisherPort publisher, ProcessedMessagePort processedMessages,
                                 TransactionPort tx) {
        this.payments = payments;
        this.gateway = gateway;
        this.publisher = publisher;
        this.processedMessages = processedMessages;
        this.tx = tx;
    }

    @Override
    public void process(String messageId, ProcessPaymentCommand command) {
        // Lớp 1: message đã xử lý xong
        if (processedMessages.isProcessed(messageId)) {
            log.info("Duplicate message {} ignored", messageId);
            return;
        }

        // Lớp 2: tìm/tạo bản ghi theo idempotency key
        Payment payment = loadOrCreate(command);

        boolean decidedNow = false;
        if (payment.isPending()) {
            // Lớp 3: cổng thanh toán cũng idempotent theo key. Có thể ném GatewayTimeoutException -> retry.
            GatewayResult result = gateway.charge(
                    new GatewayChargeRequest(payment.idempotencyKey(), payment.amount(), payment.currency()));
            payment = result.approved() ? payment.complete(result.reference()) : payment.fail(result.declineReason());
            decidedNow = true;
        } else {
            log.info("Payment {} for order {} already {} - replaying result without charging",
                    payment.id(), payment.orderId(), payment.status());
        }

        Payment outcome = payment;
        boolean changed = decidedNow;
        tx.run(() -> {
            if (!processedMessages.markProcessed(messageId)) {
                return; // luồng khác vừa xử lý xong cùng message
            }
            Payment toPublish = outcome;
            // Lớp 4: nếu ta thua cuộc đua thì phát hành kết quả đã được lưu (nguồn sự thật là DB)
            if (changed && !payments.updateIfPending(outcome)) {
                toPublish = payments.findByIdempotencyKey(outcome.idempotencyKey()).orElseThrow();
            }
            publisher.publish(toPublish);
        });
    }

    @Override
    public Payment getByOrderId(UUID orderId, UUID customerId) {
        return payments.findByOrderId(orderId)
                .filter(p -> p.customerId().equals(customerId))
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }

    private Payment loadOrCreate(ProcessPaymentCommand command) {
        String key = command.idempotencyKey();
        Payment existing = payments.findByIdempotencyKey(key).orElse(null);
        if (existing != null) {
            return verifySameRequest(existing, command);
        }
        try {
            return payments.insert(Payment.newPending(command.orderId(), command.customerId(),
                    command.amount(), command.currency(), key));
        } catch (DuplicatePaymentException race) {
            // Hoặc luồng khác vừa insert cùng key, hoặc đơn này đã có payment với key khác
            Payment winner = payments.findByIdempotencyKey(key).orElseThrow(() -> new IdempotencyKeyReuseException(key));
            return verifySameRequest(winner, command);
        }
    }

    private Payment verifySameRequest(Payment payment, ProcessPaymentCommand command) {
        if (!payment.sameRequest(command.orderId(), command.amount(), command.currency())) {
            throw new IdempotencyKeyReuseException(command.idempotencyKey());
        }
        return payment;
    }
}
