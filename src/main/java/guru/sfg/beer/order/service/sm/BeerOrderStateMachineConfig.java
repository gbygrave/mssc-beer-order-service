package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.sm.actions.AllocateOrderAction;
import guru.sfg.beer.order.service.sm.actions.ValidateOrderAction;
import guru.sfg.beer.order.service.sm.actions.ValidationFailureAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;

import java.util.EnumSet;

import static guru.sfg.beer.order.service.services.BeerOrderManagerImpl.ORDER_ID_HEADER;

@Configuration
@EnableStateMachineFactory
@RequiredArgsConstructor
@Slf4j
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {
    private final ValidateOrderAction     validateOrderAction;
    private final ValidationFailureAction validationFailureAction;
    private final AllocateOrderAction     allocateOrderAction;

    @Override
    public void configure(StateMachineConfigurationConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> config)
            throws Exception {
        config.withConfiguration().listener(new StateMachineListenerAdapter() {
            @Override
            public void eventNotAccepted(Message event) {
                val orderId = event.getHeaders().getOrDefault(ORDER_ID_HEADER, "???");
                log.error("Event not accepted ["+event.getPayload() + "] for order [" + orderId + "]");
            }
        });
    }

    @Override
    public void configure(StateMachineStateConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> states) throws Exception {
        states.withStates()
                .initial(BeerOrderStatusEnum.NEW)
                .states(EnumSet.allOf(BeerOrderStatusEnum.class))
                .end(BeerOrderStatusEnum.PICKED_UP)
                .end(BeerOrderStatusEnum.DELIVERED)
                .end(BeerOrderStatusEnum.DELIVERY_EXCEPTION)
                .end(BeerOrderStatusEnum.VALIDATION_EXCEPTION)
                .end(BeerOrderStatusEnum.ALLOCATION_EXCEPTION);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> transitions)
            throws Exception {
        transitions
                .withExternal()
                .event(BeerOrderEventEnum.VALIDATE_ORDER)
                .source(BeerOrderStatusEnum.NEW).target(BeerOrderStatusEnum.VALIDATION_PENDING)
                .action(validateOrderAction)
                .and()
                .withExternal()
                .event(BeerOrderEventEnum.VALIDATION_PASSED)
                .source(BeerOrderStatusEnum.VALIDATION_PENDING).target(BeerOrderStatusEnum.VALIDATED)
                .and()
                .withExternal()
                .event(BeerOrderEventEnum.VALIDATION_FAILED)
                .source(BeerOrderStatusEnum.VALIDATION_PENDING).target(BeerOrderStatusEnum.VALIDATION_EXCEPTION)
                .action(validationFailureAction)
                .and()
                .withExternal()
                .event(BeerOrderEventEnum.ALLOCATE_ORDER)
                .source(BeerOrderStatusEnum.VALIDATED).target(BeerOrderStatusEnum.ALLOCATION_PENDING)
                .action(allocateOrderAction)
                .and()
                .withExternal()
                .event(BeerOrderEventEnum.ALLOCATION_SUCCESS)
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.ALLOCATED)
                .and()
                .withExternal()
                .event(BeerOrderEventEnum.ALLOCATION_FAILED)
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.ALLOCATION_EXCEPTION)
                .and()
                .withExternal()
                .event(BeerOrderEventEnum.ALLOCATION_NO_INVENTORY)
                .source(BeerOrderStatusEnum.ALLOCATION_PENDING).target(BeerOrderStatusEnum.PENDING_INVENTORY)
                .and()
                .withExternal()
                .event(BeerOrderEventEnum.BEER_ORDER_PICKED_UP)
                .source(BeerOrderStatusEnum.ALLOCATED).target(BeerOrderStatusEnum.PICKED_UP);
    }
}
