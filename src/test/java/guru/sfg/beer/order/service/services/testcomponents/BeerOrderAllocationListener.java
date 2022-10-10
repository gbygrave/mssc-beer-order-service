package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import static guru.sfg.beer.order.service.services.TestConstants.FAIL_ALLOCATION;
import static guru.sfg.beer.order.service.services.TestConstants.PARTIAL_ALLOCATION;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(AllocateOrderRequest request) {
        BeerOrderDto beerOrderDto = request.getBeerOrderDto();

        // Simulate partial allocation.
        String customerRef = request.getBeerOrderDto().getCustomerRef();
        boolean allocationError = FAIL_ALLOCATION.equals(customerRef);
        boolean pendingInventory = PARTIAL_ALLOCATION.equals(customerRef);

        int shortfall = pendingInventory ? 1 : 0;
        beerOrderDto.getBeerOrderLines().forEach(line -> {
            line.setQuantityAllocated(line.getOrderQuantity() - shortfall);
        });
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                                   new AllocateOrderResponse(beerOrderDto,
                                                             allocationError,
                                                             pendingInventory));
    }
}
