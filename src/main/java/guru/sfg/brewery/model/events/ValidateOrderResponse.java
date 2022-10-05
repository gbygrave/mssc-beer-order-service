package guru.sfg.brewery.model.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ValidateOrderResponse implements Serializable {
    private static final long serialVersionUID = 8695309424794727221L;

    private UUID orderId;
    private boolean isValid;
}
