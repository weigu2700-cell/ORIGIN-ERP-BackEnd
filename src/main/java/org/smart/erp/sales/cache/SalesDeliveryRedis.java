package org.smart.erp.sales.cache;

import org.smart.erp.common.utils.redis.OperationString;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SalesDeliveryRedis {

    private static final String DELIVERY_KEY_PREFIX = "erp:sales:delivery:processing:";

    private final OperationString operationString;

    public SalesDeliveryRedis(OperationString operationString) {
        this.operationString = operationString;
    }

    public Boolean setDeliveryCacheIfAbsent(Long id, Object value) {
        return operationString.setIfAbsent(DELIVERY_KEY_PREFIX, id, value, Duration.ofSeconds(30));
    }

    public Boolean setDeliveryCacheIfAbsent(Long id, String token) {
        return operationString.setIfAbsent(DELIVERY_KEY_PREFIX, id, token, Duration.ofSeconds(30));
    }

    public void evictDeliveryCache(Long id) {
        operationString.delete(DELIVERY_KEY_PREFIX, id);
    }
}
