package guru.sfg.brewery.model.events;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeallocateOrderRequest implements Serializable {
    private static final long serialVersionUID = 8695309424794727221L;

    private BeerOrderDto beerOrderDto;
}
