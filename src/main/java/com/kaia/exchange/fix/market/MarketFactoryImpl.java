package com.kaia.exchange.fix.market;

import com.kaia.exchange.core.market.BookFactory;
import com.kaia.exchange.core.market.Market;
import com.kaia.exchange.core.market.MarketFactory;

public class MarketFactoryImpl extends MarketFactory {

    public MarketFactoryImpl(BookFactory bookFactory) {
        super(bookFactory);
    }

    @Override
    public Market createMarket(String symbol) {
        return new MarketImpl(symbol, bookFactory);
    }
}

