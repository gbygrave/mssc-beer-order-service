package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.BEER_ORDER_PICKED_UP;
import static guru.sfg.beer.order.service.domain.BeerOrderEventEnum.CANCEL_ORDER;
import static guru.sfg.beer.order.service.domain.BeerOrderStatusEnum.ALLOCATED;

@RequiredArgsConstructor
@Service
@Slf4j
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";
    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        log.info("Beer order saved: " + savedBeerOrder.getId());

        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void processValidationResult(UUID beerOrderId, boolean isValid) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(
                beerOrder -> {
                    if (isValid) {
                        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);
                        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(
                                validatedOrder -> sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER),
                                () -> {
                                    throw new RuntimeException("Beer orders are disappearing from the repository.");
                                });
                    } else {
                        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
                    }
                },
                () -> {
                    log.error("Beer order not found: " + beerOrderId);
                });
    }


    @Override
    public void processAllocateOrderResponse(BeerOrderDto beerOrderDto,
                                             Boolean allocationError,
                                             Boolean pendingInventory) {
        UUID beerOrderId = beerOrderDto.getId();
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(
                beerOrder -> {
                    if (allocationError) {
                        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
                    } else if (pendingInventory) {
                        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
                        updateAllocatedQty(beerOrderDto);
                    } else {
                        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
                        updateAllocatedQty(beerOrderDto);
                    }
                },
                () -> {
                    log.error("Beer order not found: " + beerOrderId);
                }
        );
    }

    @Override
    public void pickupBeerOrder(UUID beerOrderId) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(
                beerOrder -> {
                    if (beerOrder.getOrderStatus() != ALLOCATED)
                        throw new IllegalStateException(
                                "Order ["+beerOrderId+"] must be in the " + ALLOCATED + " state in order to be picked up.");
                    sendBeerOrderEvent(beerOrder, BEER_ORDER_PICKED_UP);
                },
                () -> log.error("Beer order not found: " + beerOrderId)
        );
    }

    @Override
    public void cancelBeerOrder(UUID beerOrderId) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(
                beerOrder -> {
                    sendBeerOrderEvent(beerOrder, CANCEL_ORDER);
                },
                () -> log.error("Beer order not found: " + beerOrderId)
        );
    }


    private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        UUID beerOrderId = beerOrderDto.getId();
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(
                beerOrder -> {
                    beerOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                        beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto ->  {
                            if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
                                beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                            }
                        });
                    });
                    log.debug("Saving updated allocated quantity on lines for order id: " + beerOrderId);
                    beerOrderRepository.saveAndFlush(beerOrder);
                },
                () -> log.error("Beer order not found: " + beerOrderId)
        );
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);
        Message<BeerOrderEventEnum> msg = MessageBuilder.withPayload(eventEnum)
                .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();
        sm.sendEvent(msg);
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());

        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(),
                                                                           null,
                                                                           null,
                                                                           null));
                    sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
                });

        sm.start();

        return sm;
    }
}
