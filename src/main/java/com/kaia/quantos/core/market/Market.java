/*
 * Copyright (C) 2013-2014  M.B.Grieve.
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

package com.kaia.quantos.core.market;

import com.kaia.quantos.core.engine.Order;
import com.kaia.quantos.core.stp.Trade;

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
