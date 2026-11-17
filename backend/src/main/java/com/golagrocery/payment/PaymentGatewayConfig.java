package com.golagrocery.payment;

import com.golagrocery.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PaymentGatewayConfig {
    @Bean
    public PaymentGateway paymentGateway(AppProperties props) {
        if (props.payments().isRazorpay()) {
            log.info("Payments: Razorpay");
            return new RazorpayGateway(props.payments().razorpay());
        }
        log.warn("Payments: MOCK mode - no real money is collected. Set PAYMENTS_MODE=razorpay for production.");
        return new MockGateway();
    }
}
