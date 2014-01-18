package com.kaia.exchange.core.market;

import com.kaia.exchange.core.engine.Order;

import java.util.List;

public abstract class BookFactory {

    public abstract Book createBook(String symbol, List<Order> askOrders, List<Order> bidOrders, BookObserver observer);
}
