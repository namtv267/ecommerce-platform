package com.shop.payment.application;

import com.shop.payment.adapter.out.gateway.MockPaymentGateway;
import com.shop.payment.application.port.out.PaymentGatewayPort.GatewayChargeRequest;
import com.shop.payment.application.port.out.PaymentGatewayPort.GatewayResult;
import com.shop.payment.domain.exception.GatewayTimeoutException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPaymentGatewayTest {

    @Test
    void sameKey_returnsSameResult_andChargesOnce() {
        MockPaymentGateway gateway = new MockPaymentGateway(0.0, 0.0, 0);
        GatewayChargeRequest req = new GatewayChargeRequest("k1", new BigDecimal("50.00"), "USD");

        GatewayResult first = gateway.charge(req);
        GatewayResult second = gateway.charge(req);

        assertThat(second).isEqualTo(first);
        assertThat(gateway.chargeCount()).isEqualTo(1);
    }

    @Test
    void centsFourteen_timesOutOnlyOnFirstCall_afterCharging() {
        MockPaymentGateway gateway = new MockPaymentGateway(0.0, 0.0, 0);
        GatewayChargeRequest req = new GatewayChargeRequest("k2", new BigDecimal("20.14"), "USD");

        assertThatThrownBy(() -> gateway.charge(req)).isInstanceOf(GatewayTimeoutException.class);
        assertThat(gateway.chargeCount()).isEqualTo(1); // tiền ĐÃ bị trừ dù client không nhận được phản hồi

        GatewayResult retry = gateway.charge(req);
        assertThat(retry.approved()).isTrue();
        assertThat(gateway.chargeCount()).isEqualTo(1);
    }

    @Test
    void centsThirteen_isDeclined() {
        MockPaymentGateway gateway = new MockPaymentGateway(0.0, 0.0, 0);

        GatewayResult result = gateway.charge(new GatewayChargeRequest("k3", new BigDecimal("10.13"), "USD"));

        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }
}
