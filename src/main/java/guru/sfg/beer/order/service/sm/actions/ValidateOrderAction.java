package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.ValidateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Send a validate order request message, typically when transitioning to the validate pending state.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;
    private final JmsTemplate jmsTemplate;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        // Get/create the BeerOrderDTO
        Optional.ofNullable(context.getMessage())
                .map(msg -> (String) msg.getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER))
                .ifPresent(orderId -> {
                    beerOrderRepository.findById(UUID.fromString(orderId)).ifPresentOrElse(
                            beerOrder -> {
                                BeerOrderDto beerOrderDto = beerOrderMapper.beerOrderToDto(beerOrder);
                                jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_QUEUE, new ValidateOrderRequest(beerOrderDto));
                                log.debug("Sent Validation request to queue for order id " + orderId);
                            },
                            () -> {
                                log.error("Beer order not found: " + orderId);
                            }
                    );
                });
    }
}
