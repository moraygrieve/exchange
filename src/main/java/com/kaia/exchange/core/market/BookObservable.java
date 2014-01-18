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
