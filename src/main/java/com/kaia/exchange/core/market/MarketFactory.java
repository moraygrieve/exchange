package com.kaia.exchange.core.market;

public abstract class MarketFactory {
    protected BookFactory bookFactory;

    public MarketFactory(BookFactory bookFactory){
        this.bookFactory = bookFactory;
    }

    public abstract Market createMarket(String symbol);
}

