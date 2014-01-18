package com.kaia.exchange.core.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Observable<T> {
    protected List<Observer<T>> observers = new CopyOnWriteArrayList<Observer<T>>();
    private boolean changed = true;

    protected void setChanged() {
        this.changed = true;
    }

    protected boolean hasChanged() {
        return this.changed;
    }

    public void addObserver(Observer<T> observer) {
        if (observers.indexOf(observer) < 0) {
            observers.add(observer);
        }
    }

    public void deleteObserver(Observer<T> observer) {
        if (observers.indexOf(observer) >= 0) {
            observers.remove(observer);
        }
    }

    protected void notifyObservers(T t) {
        for (Observer<T> observer : observers) {
            observer.update(t);
        }
        this.changed = false;
    }

    protected void notifyObserver(Observer<T> observer, T t) {
        observer.update(t);
    }
}

