package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.brewery.model.events.AllocationFailureEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class AllocationFailureAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        Optional.ofNullable((String)context.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER)).ifPresentOrElse(
                beerOrderId -> {
                    jmsTemplate.convertAndSend(
                            JmsConfig.ALLOCATE_FAILURE_QUEUE,
                            new AllocationFailureEvent(UUID.fromString(beerOrderId)));
                    log.error("Sent Allocation Failure Message to queue for order id " + beerOrderId);
                },
                () -> {
                    log.error("No beer order id could be found in message header.");
                }
        );
    }
}
