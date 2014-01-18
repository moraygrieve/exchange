package com.kaia.exchange.core.market;

import java.util.List;

public abstract class BookObserver implements Comparable {
    private String account;
    private List<String> counterParties;

    public BookObserver(String account, List<String> counterParties) {
        this.account = account;
        this.counterParties = counterParties;
    }

    public abstract void update(Book book);

    public String getAccount() {
        return this.account;
    }

    public List<String> getCounterParties() {
        return this.counterParties;
    }
}

