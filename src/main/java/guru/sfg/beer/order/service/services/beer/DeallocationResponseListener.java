package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.AllocateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeallocationResponseListener {
    private final BeerOrderManager beerOrderManager;

    @Transactional
    @JmsListener(destination = JmsConfig.DEALLOCATE_ORDER_RESPONSE_QUEUE)
    public void listen(AllocateOrderResponse event) {
        log.debug("Received DeallocateOrderResponse: " + event);
        beerOrderManager.processDeallocateOrderResponse(event.getBeerOrderDto());
    }
}
