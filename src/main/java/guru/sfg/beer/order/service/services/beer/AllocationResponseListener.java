package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.AllocateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationResponseListener {
    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE)
    public void listen(AllocateOrderResponse event) {
        log.debug("Received AllocateOrderResponse: " + event);
        beerOrderManager.processAllocateOrderResponse(event.getBeerOrderDto(),
                                                      event.getAllocationError(),
                                                      event.getPendingInventory());
    }
}
