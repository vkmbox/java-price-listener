package com.acme.mytrader.strategy;

import lombok.Getter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;
import com.acme.mytrader.price.PriceListener;
import com.acme.mytrader.execution.ExecutionService;

/**
 * <pre>
 * User Story: As a trader I want to be able to monitor stock prices such
 * that when they breach a trigger level orders can be executed automatically
 * </pre>
 */
@Slf4j
@Getter
@Builder
@AllArgsConstructor
public class TradingStrategy implements PriceListener {
    
    public static enum BreachType {MORE_EQ, LESS_EQ};
    public static enum ExecutionType {BUY, SELL};
    
    public static TradingStrategyBuilder builder() {
        return new CustomStrategyBuilder();
    }
    
    public static class CustomStrategyBuilder extends TradingStrategyBuilder {
        
        private void notBlank(Object value, String message) {
            if (value == null) {
                throw new IllegalArgumentException(message);
            }
        }
        
        private <T> T nvl(T ... args) {
            for (T arg : args) {
                if (arg != null) {
                    return arg;
                }
            }
            return null;
        }
        
        @Override
        public TradingStrategy build() {
            // Validates required fields
            notBlank(super.targetSecurity, "security cannot be null or empty!");
            notBlank(super.threshold, "threshold cannot be null or empty!");
            notBlank(super.executionType, "execution type cannot be null or empty!");
            notBlank(super.volume, "volume cannot be null or empty!");
            notBlank(super.executionService, "execution service cannot be null or empty!");
            
            int executionsLeft = 0;
            if (super.executionsNumber > 0) {
                executionsLeft = super.executionsNumber;
            }
            
            return new TradingStrategy(super.targetSecurity, super.threshold
                    , nvl(super.breachType, BreachType.LESS_EQ), super.executionType
                    , super.volume, super.executionService, super.executionsNumber
                    , executionsLeft);
        }
    }
    
    /**
    * Security code, not null.
    */
    private final String targetSecurity;
    /**
    * Threshold value, not null.
    */
    private final Double threshold;
    /**
    * Breach type, associated with the threshold value.
    */
    private final BreachType breachType;
    /**
    * Execution type (buy/sell), not null.
    */
    private final ExecutionType executionType;
    /**
    * Execution volume, not null.
    */
    private final Integer volume;
    /**
    * Reference to the execution service, not null.
    */
    private final ExecutionService executionService;
    /**
    * Number of executions for the TradingStrategy instance,
    * for -1 number of executions is unlimited.
    */
    private final int executionsNumber;

    private int executionsLeft;

    @Override
    public void priceUpdate(String security, double price) {
        log.debug("Price update with security:{}, price:{}", security, price);
        if (!(targetSecurity.equals(security) && isActive())) {
            return;
        }
        if ((breachType == BreachType.LESS_EQ && price <= threshold)
                || (breachType == BreachType.MORE_EQ && price >= threshold)) {
            switch (executionType) {
                case BUY:
                    executionService.buy(security, price, volume);
                    log.debug("Purchase order with security:{}, price:{}, volume:{}", security, price, volume);
                    break;
                case SELL:
                    executionService.sell(security, price, volume);
                    log.debug("Sell order with security:{}, price:{}, volume:{}", security, price, volume);
                    break;
                default:
                    log.error("Unprocessed executionType {} is oberved", executionType);
            }
            if (executionsLeft > 0) {
                executionsLeft--;
            }
        }
    }
    
    @Override
    public boolean isActive() {
        return executionsNumber < 0 || executionsLeft > 0;
    }

}
