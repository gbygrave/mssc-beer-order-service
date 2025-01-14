package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeerOrderStateChangeInterceptor
        extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;

    @Override
    public void preStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state,
                               Message<BeerOrderEventEnum> message,
                               Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition,
                               StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine) {
        Optional.ofNullable(message)
                .map(msg -> (String) msg.getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER))
                .ifPresent(orderId -> {
                    beerOrderRepository.findById(UUID.fromString(orderId)).ifPresentOrElse(
                            beerOrder -> {
                                beerOrder.setOrderStatus(state.getId());
                                beerOrderRepository.saveAndFlush(beerOrder);
                                log.debug("Saved state for order id: " + orderId + " Status: " + state.getId());

                            },
                            () -> {
                                log.error("Order not found: " + orderId);
                            });
                });
    }
}
