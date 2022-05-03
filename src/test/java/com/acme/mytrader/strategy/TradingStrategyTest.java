package com.acme.mytrader.strategy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyDouble;

import org.junit.Test;
import org.mockito.Mockito;
import com.acme.mytrader.price.PriceSource;
import com.acme.mytrader.price.PriceListener;
import com.acme.mytrader.execution.ExecutionService;
import com.acme.mytrader.strategy.TradingStrategy.BreachType;
import com.acme.mytrader.strategy.TradingStrategy.ExecutionType;
import com.acme.mytrader.strategy.TradingStrategy.TradingStrategyBuilder;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

public class TradingStrategyTest {
    
    static class DummyPriceSource implements PriceSource {
        
        private final List<PriceListener> listeners = new ArrayList<>();

        @Override
        public void addPriceListener(PriceListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removePriceListener(PriceListener listener) {
            listeners.remove(listener);
        }
        
        void priceUpdate(String security, double price) {
            if (listeners.isEmpty()) {
                return;
            }
            Set<PriceListener> extras  = new HashSet<>();
            for (PriceListener listener: listeners) {
                if (listener.isActive()) {
                    listener.priceUpdate(security, price);
                } else {
                    extras.add(listener);
                }
            }
            listeners.removeAll(extras);
        }
    }
    
    private TradingStrategyBuilder getDefaultStrategy() {
        return TradingStrategy.builder().executionsNumber(1)
                .targetSecurity("IBM").threshold(55.7)
                .breachType(BreachType.LESS_EQ).executionType(ExecutionType.BUY)
                .volume(10);
    }
    
    @Test
    public void testOneBuy() {
        ExecutionService mockService = mock(ExecutionService.class);
        
        TradingStrategy strategy = getDefaultStrategy()
                .executionService(mockService).build(); 
        
        DummyPriceSource prices = new DummyPriceSource();
        prices.addPriceListener(strategy);
        prices.priceUpdate("IBM", 55.8);
        prices.priceUpdate("ORACLE", 55.7);
        prices.priceUpdate("IBM", 55.6);
        prices.priceUpdate("IBM", 55.5);
        
        Mockito.verify(mockService, times(1)).buy(any(), anyDouble(), anyInt());
        Mockito.verify(mockService, times(1)).buy("IBM", 55.6, 10);
        assertFalse("Strategy should be closed", strategy.isActive());
    }
    
    @Test
    public void testMultipleBuys() {
        ExecutionService mockService = mock(ExecutionService.class);
        
        TradingStrategy strategy = getDefaultStrategy().executionsNumber(2)
                .executionService(mockService).volume(15).build(); 
        
        DummyPriceSource prices = new DummyPriceSource();
        prices.addPriceListener(strategy);
        prices.priceUpdate("IBM", 55.8);
        prices.priceUpdate("ORACLE", 55.7);
        prices.priceUpdate("IBM", 55.6);
        prices.priceUpdate("IBM", 55.5);
        prices.priceUpdate("ORACLE", 55.4);
        prices.priceUpdate("IBM", 49.9);
        
        Mockito.verify(mockService, times(2)).buy(any(), anyDouble(), anyInt());
        Mockito.verify(mockService, times(1)).buy("IBM", 55.6, 15);
        Mockito.verify(mockService, times(1)).buy("IBM", 55.5, 15);
        assertFalse("Strategy should be closed", strategy.isActive());
    }
    
    @Test
    public void testMultipleSells() {
        ExecutionService mockService = mock(ExecutionService.class);
        
        TradingStrategy strategy = getDefaultStrategy().executionsNumber(-1)
                .executionService(mockService).volume(11).targetSecurity("ORACLE")
                .threshold(64.28).breachType(BreachType.MORE_EQ)
                .executionType(ExecutionType.SELL).build(); 
        
        DummyPriceSource prices = new DummyPriceSource();
        prices.addPriceListener(strategy);
        prices.priceUpdate("IBM", 65.8);
        prices.priceUpdate("ORACLE", 64.2);
        prices.priceUpdate("IBM", 63.6);
        prices.priceUpdate("ORACLE", 64.3);
        prices.priceUpdate("DXC", 164.77);
        prices.priceUpdate("ORACLE", 65.7);
        prices.priceUpdate("ORACLE", 64.1);
        prices.priceUpdate("ORACLE", 67.1);
        
        Mockito.verify(mockService, times(3)).sell(any(), anyDouble(), anyInt());
        Mockito.verify(mockService, times(1)).sell("ORACLE", 64.3, 11);
        Mockito.verify(mockService, times(1)).sell("ORACLE", 65.7, 11);
        Mockito.verify(mockService, times(1)).sell("ORACLE", 67.1, 11);
        assertTrue("Active strategy is expected", strategy.isActive());
    }
}
