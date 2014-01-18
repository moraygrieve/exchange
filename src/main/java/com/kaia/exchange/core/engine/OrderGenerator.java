package com.kaia.exchange.core.engine;

public interface OrderGenerator {
    public void orderAccepted(Order order, String text);

    public void orderRejected(Order order, String text);

    public void amendAccepted(Order order, String text);

    public void amendRejected(Order order, String text);

    public void cancelAccepted(Order order, String text);

    public void cancelRejected(Order order, String text);

    public void orderMatched(Order order, String text);

    public void orderLocked(String tradeId, Order order, float price, long quantity);
}
