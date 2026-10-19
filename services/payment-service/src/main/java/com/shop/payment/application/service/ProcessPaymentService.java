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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.payment.domain.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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

        boolean isEmailPayment = command.idempotencyKey() != null && command.idempotencyKey().contains("-email-");
        boolean decidedNow = false;
        if (payment.isPending()) {
            if (isEmailPayment) {
                log.info("Order {} requested payment via Dev Mail - sending approval email", command.orderId());
                sendDevMail(command.orderId(), payment.amount(), payment.currency());
                // Không trừ tiền tự động, giữ payment PENDING để người dùng ấn Accept qua mail
            } else {
                // Lớp 3: cổng thanh toán cũng idempotent theo key. Có thể ném GatewayTimeoutException -> retry.
                GatewayResult result = gateway.charge(
                        new GatewayChargeRequest(payment.idempotencyKey(), payment.amount(), payment.currency()));
                payment = result.approved() ? payment.complete(result.reference()) : payment.fail(result.declineReason());
                decidedNow = true;
            }
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
            if (changed) {
                Payment toPublish = outcome;
                // Lớp 4: nếu ta thua cuộc đua thì phát hành kết quả đã được lưu (nguồn sự thật là DB)
                if (!payments.updateIfPending(outcome)) {
                    toPublish = payments.findByIdempotencyKey(outcome.idempotencyKey()).orElseThrow();
                }
                publisher.publish(toPublish);
            }
        });
    }

    public Payment approve(UUID orderId) {
        Payment payment = payments.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
        if (payment.status() == PaymentStatus.COMPLETED) {
            log.info("Payment for order {} already COMPLETED", orderId);
            return payment;
        }
        Payment completed = payment.complete("gw_email_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        tx.run(() -> {
            if (payments.updateIfPending(completed)) {
                publisher.publish(completed);
                log.info("Payment for order {} approved via Dev Mail, event published to Kafka", orderId);
            }
        });
        return completed;
    }

    @Override
    public Payment getByOrderId(UUID orderId, UUID customerId) {
        return payments.findByOrderId(orderId)
                .filter(p -> p.customerId().equals(customerId))
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }

    public Payment getByOrderId(UUID orderId) {
        return payments.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }

    private void sendDevMail(UUID orderId, BigDecimal amount, String currency) {
        try {
            String mailpitHost = System.getenv().getOrDefault("MAILPIT_HOST", "mailpit");
            String mailpitPort = System.getenv().getOrDefault("MAILPIT_PORT", "8025");
            String confirmUrl = "http://localhost/payment-confirm?orderId=" + orderId;

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 600px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);">
                  <div style="background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%); padding: 32px 24px; text-align: center; color: #ffffff;">
                    <h1 style="margin: 0; font-size: 24px; font-weight: 800;">🛍️ ShopFlow Payment</h1>
                    <p style="margin: 8px 0 0; opacity: 0.9; font-size: 15px;">Yêu cầu xác nhận thanh toán đơn hàng</p>
                  </div>
                  <div style="padding: 32px 24px; color: #1e293b;">
                    <p style="font-size: 16px; margin-top: 0;">Xin chào <strong>Quý khách</strong>,</p>
                    <p style="font-size: 15px; color: #475569; line-height: 1.6;">
                      Bạn vừa tạo đơn hàng với hình thức <strong>Thanh toán qua Email (Dev Mail)</strong>. Vui lòng bấm vào nút bên dưới để hoàn tất thanh toán:
                    </p>
                    <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 24px 0;">
                      <table style="width: 100%; border-collapse: collapse; font-size: 14px;">
                        <tr>
                          <td style="padding: 6px 0; color: #64748b;">Mã đơn hàng:</td>
                          <td style="padding: 6px 0; font-weight: 600; text-align: right; font-family: monospace;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding: 6px 0; color: #64748b;">Số tiền thanh toán:</td>
                          <td style="padding: 6px 0; font-weight: 700; color: #4f46e5; text-align: right; font-size: 16px;">%s %s</td>
                        </tr>
                        <tr>
                          <td style="padding: 6px 0; color: #64748b;">Trạng thái:</td>
                          <td style="padding: 6px 0; text-align: right;"><span style="background: #fef3c7; color: #b45309; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600;">CHỜ XÁC NHẬN</span></td>
                        </tr>
                      </table>
                    </div>
                    <div style="text-align: center; margin: 32px 0;">
                      <a href="%s" style="background: #10b981; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 700; font-size: 16px; display: inline-block;">
                        👉 BẤM VÀO ĐÂY ĐỂ XÁC NHẬN THANH TOÁN (ACCEPT)
                      </a>
                    </div>
                    <p style="font-size: 13px; color: #94a3b8; text-align: center; margin-bottom: 0;">
                      Khi bạn bấm Accept, Kafka Saga sẽ tự động cập nhật đơn hàng thành PAID.
                    </p>
                  </div>
                </div>
                """.formatted(orderId, amount, currency, confirmUrl);

            Map<String, Object> payload = Map.of(
                    "From", Map.of("Email", "payment@shopflow.local", "Name", "ShopFlow Payment Gateway"),
                    "To", List.of(Map.of("Email", "customer@shopflow.local", "Name", "Customer")),
                    "Subject", "[ShopFlow] Xác nhận thanh toán cho đơn hàng #" + orderId,
                    "HTML", html
            );

            String jsonPayload = new ObjectMapper().writeValueAsString(payload);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + mailpitHost + ":" + mailpitPort + "/api/v1/send"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(res -> log.info("Dev Mail sent to Mailpit for order {}, HTTP status {}", orderId, res.statusCode()))
                    .exceptionally(ex -> {
                        log.warn("Failed to send Dev Mail to Mailpit: {}", ex.getMessage());
                        return null;
                    });
        } catch (Exception ex) {
            log.warn("Error preparing Dev Mail: {}", ex.getMessage());
        }
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
