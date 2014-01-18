package com.kaia.exchange.core.engine;

import com.kaia.exchange.core.market.BookObserver;
import com.kaia.exchange.core.market.Market;
import com.kaia.exchange.core.market.MarketFactory;
import com.kaia.exchange.core.stp.Trade;
import com.kaia.exchange.core.stp.TradeCache;
import com.kaia.exchange.core.utils.Observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Engine {
    private final MarketFactory marketFactory;
    private final TradeCache tradeCache = TradeCache.getInstance();
    private Map<String, Market> markets = new HashMap<String, Market>();
    private final Object lock = new Object();

    public Engine(MarketFactory marketFactory) {
        this.marketFactory = marketFactory;
    }

    public void subscribeBook(String symbol, BookObserver subscriber) {
        getMarket(symbol).addObserver(subscriber);
    }

    public void unsubscribeBook(String symbol, BookObserver subscriber) {
        getMarket(symbol).deleteObserver(subscriber);
    }

    public void subscribeTradeNotifications(Observer<Trade> subscriber) {
        tradeCache.getTradesForSubscriber(subscriber);
        tradeCache.addObserver(subscriber);
    }

    public void unsubscribeTradeNotifications(Observer<Trade> subscriber) {
        tradeCache.deleteObserver(subscriber);
    }

    public void insertOrder(OrderGenerator supplier, Order order, boolean update) {
        synchronized (lock) {
            Market market = getMarket(order.getSymbol());
            if (market.insert(order)) {
                supplier.orderAccepted(order, "Order accepted by market");
                matchOrder(supplier, order);
            } else {
                order.cancel();
                supplier.orderRejected(order, "Order rejected by market");
            }
            if ( update ) { market.updateObservers(); }
        }
    }

    public boolean amendOrder(OrderGenerator supplier, String symbol, String side, String id, double price, long quantity, boolean update) {
        synchronized (lock) {
            Market market = getMarket(symbol);
            Order order = market.find(side, id);
            if (order==null) return false;
            if (market.amend(order, price, quantity)) {
                supplier.amendAccepted(order, "Amend accepted by market");
                matchOrder(supplier, order);
            } else {
                supplier.amendRejected(order, "Amend rejected by market");
            }
            if ( update ) { market.updateObservers(); }
            return true;
        }
    }

    public boolean deleteOrder(OrderGenerator supplier, String symbol, String side, String id, boolean update) {
        synchronized (lock) {
            Market market = getMarket(symbol);
            Order order = market.find(side, id);
            if (order==null) return false;
            if (market.erase(order)) {
                order.cancel();
                supplier.cancelAccepted(order, "Cancel accepted by market");
            } else {
                supplier.cancelRejected(order, "Cancel rejected by market");
            }
            if ( update ) { market.updateObservers(); }
            return true;
        }
    }

    public void updateMarketObservers(String symbol) {
        synchronized (lock) {
            Market market = getMarket(symbol);
            market.updateObservers();
        }
    }

    private Market getMarket(String symbol) {
        Market m = markets.get(symbol);
        if (m == null) {
            m = marketFactory.createMarket(symbol);
            markets.put(symbol, m);
        }
        return m;
    }

    private void matchOrder(OrderGenerator supplier, Order order) {
        List<Order> orders = new ArrayList<Order>();
        List<Trade> trades = new ArrayList<Trade>();
        getMarket(order.getSymbol()).match(order, orders, trades);
        for (Order o : orders) {
            supplier.orderMatched(o, "Order matched");
        }
        for (Trade t : trades) {
            tradeCache.add(t, t.getSymbol(), true);
        }
    }
}