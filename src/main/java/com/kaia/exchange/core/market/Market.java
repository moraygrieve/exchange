package com.kaia.exchange.core.market;

import com.kaia.exchange.core.engine.Order;
import com.kaia.exchange.core.stp.Trade;

import java.util.List;

public abstract class Market extends BookObservable {
    protected final String symbol;
    protected final BookFactory bookFactory;
    protected List<Order> askOrders;
    protected List<Order> bidOrders;

    public Market(String symbol, BookFactory bookFactory) {
        this.symbol = symbol;
        this.bookFactory = bookFactory;
    }

    public abstract Order find(String side, String id);

    public abstract boolean insert(Order order);

    public abstract boolean amend(Order order, double price, long quantity);

    public abstract boolean erase(Order order);

    public abstract void match(Order aggressor, List<Order> orders, List<Trade> trades);

    public abstract void updateObservers();

    public abstract void display();
}
