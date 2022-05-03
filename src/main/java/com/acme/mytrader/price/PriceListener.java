package com.acme.mytrader.price;

public interface PriceListener {
    void priceUpdate(String security, double price);
    /**
    * The following method is strictly required in the proposed class model,
    * PriceSource should remove the listener on its side if it is not active.
    */
    boolean isActive();
}
