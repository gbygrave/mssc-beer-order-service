package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import static guru.sfg.beer.order.service.services.TestConstants.CANCELLED_WHILE_PENDING_VALIDATION;
import static guru.sfg.beer.order.service.services.TestConstants.FAIL_VALIDATION;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void listen(ValidateOrderRequest request) {
        // Condition to fail validation
        String custRef = request.getBeerOrderDto().getCustomerRef();
        boolean isInvalid = FAIL_VALIDATION.equals(custRef);
        boolean dontSend = CANCELLED_WHILE_PENDING_VALIDATION.equals(custRef);

        if (!dontSend) {
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                                       new ValidateOrderResponse(request.getBeerOrderDto().getId(), !isInvalid));
        }
    }
}
