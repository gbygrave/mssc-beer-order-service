package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.brewery.model.BeerOrderDto;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void processValidationResult(UUID beerOrderId, boolean isValid);

    void processAllocateOrderResponse(BeerOrderDto beerOrderDto, Boolean allocationError, Boolean pendingInventory);

    void processDeallocateOrderResponse(BeerOrderDto beerOrderDto);

    void pickupBeerOrder(UUID beerOrderId);

    void cancelBeerOrder(UUID beerOrderId);
}
