package guru.sfg.beer.order.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
public class JmsConfig {

    public static final String VALIDATE_ORDER_QUEUE            = "validate-order";
    public static final String VALIDATE_ORDER_RESPONSE_QUEUE   = "validate-order-result";
    public static final String ALLOCATE_ORDER_QUEUE            = "allocate-order";
    public static final String ALLOCATE_ORDER_RESPONSE_QUEUE   = "allocate-order-response";
    public static final String ALLOCATE_FAILURE_QUEUE          = "allocate-failure";
    public static final String DEALLOCATE_ORDER_QUEUE          = "deallocate-order";
    public static final String DEALLOCATE_ORDER_RESPONSE_QUEUE = "deallocate-order-response";


    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
