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

package com.kaia.exchange.core.market;

import com.kaia.exchange.core.engine.Order;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BookObservable {
    protected List<BookObserver> observers = new CopyOnWriteArrayList<BookObserver>();
    protected boolean changed = true;

    protected void setChanged() {
        this.changed = true;
    }

    protected boolean hasChanged() {
        return this.changed;
    }

    public void addObserver(BookObserver observer) {
        if (observers.indexOf(observer) < 0) {
            observers.add(observer);
        }
    }

    public void deleteObserver(BookObserver observer) {
        if (observers.indexOf(observer) >= 0) {
            observers.remove(observer);
        }
    }

    protected void notifyObservers(BookFactory bookFactory, String symbol, List<Order> bidOrders, List<Order> askOrders) {
        for (BookObserver observer : observers) {
            Book book = bookFactory.createBook(symbol, bidOrders, askOrders, observer);
            book.display();
            observer.update(book);
        }
        this.changed = false;
    }
}
