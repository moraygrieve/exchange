/*
 * Copyright (C) 2013-2014  M.B.Grieve, Kaia Consulting Ltd.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Contact: moray.grieve@me.com
 */

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
