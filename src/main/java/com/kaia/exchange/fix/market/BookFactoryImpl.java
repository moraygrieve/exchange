package com.kaia.exchange.fix.market;

import com.kaia.exchange.core.engine.Order;
import com.kaia.exchange.core.market.BookFactory;
import com.kaia.exchange.core.market.BookObserver;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BookFactoryImpl extends BookFactory {

    public BookImpl createBook(String symbol, List<Order> askOrders, List<Order> bidOrders, BookObserver observer) {
        Comparator<Double> descending = new Comparator<Double>() {
            public int compare(Double d1, Double d2) {
                return d2.compareTo(d1);
            }
        };
        Map<Double, Long> asks = collapseOrders(askOrders, new TreeMap<Double, Long>(descending), observer);
        Map<Double, Long> bids = collapseOrders(bidOrders, new TreeMap<Double, Long>(descending), observer);
        return new BookImpl(symbol, observer.getAccount(), asks, bids);
    }

    protected Map<Double, Long> collapseOrders(List<Order> orders, Map<Double, Long> map, BookObserver observer) {
        for (Order o : orders) {
            if ( o.getCounterParties().contains(observer.getAccount()) && observer.getCounterParties().contains(o.getAccount()) )  {
                if (map.containsKey(o.getPrice())) {
                    Long openQty = map.get(o.getPrice());
                    map.put(o.getPrice(), o.getOpenQuantity() + openQty);
                } else {
                    map.put(o.getPrice(), o.getOpenQuantity());
                }
            }
        }
        return map;
    }
}
