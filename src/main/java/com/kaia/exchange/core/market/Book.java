package com.kaia.exchange.core.market;

public abstract class Book {
    protected String symbol;
    protected String account;

    public Book(String symbol, String account) {
        this.symbol = symbol;
        this.account = account;
    }

    public abstract void display();
}
