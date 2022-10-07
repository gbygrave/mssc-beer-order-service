package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.ValidateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderValidationResponseListener {
    private final BeerOrderManager beerOrderManager;

    @Transactional
    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listen(ValidateOrderResponse event) {
        log.debug("Received order validation response: " + event.getOrderId());
        beerOrderManager.processValidationResult(event.getOrderId(), event.isValid());
    }
}
