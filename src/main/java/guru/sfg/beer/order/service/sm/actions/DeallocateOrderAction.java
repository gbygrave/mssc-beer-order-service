package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.events.DeallocateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeallocateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        Optional.ofNullable(context.getMessage())
                .map(msg -> (String) msg.getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER))
                .ifPresent(orderId -> {
                    beerOrderRepository.findById(UUID.fromString(orderId)).ifPresentOrElse(
                            beerOrder -> {
                                jmsTemplate.convertAndSend(
                                        JmsConfig.DEALLOCATE_ORDER_QUEUE,
                                        new DeallocateOrderRequest(beerOrderMapper.beerOrderToDto(beerOrder)));
                                log.debug("Sent Deallocation Request for order id: " + orderId);
                            },
                            () -> {
                                log.error("Beer order not found: " + orderId);
                            }
                    );
                });
    }
}
