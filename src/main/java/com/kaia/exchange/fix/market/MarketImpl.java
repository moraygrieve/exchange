package com.kaia.exchange.fix.market;

import com.kaia.exchange.core.engine.Order;
import com.kaia.exchange.core.market.BookFactory;
import com.kaia.exchange.core.market.Market;
import com.kaia.exchange.core.stp.Trade;
import com.kaia.exchange.core.utils.Constants;
import com.kaia.exchange.core.utils.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MarketImpl extends Market {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketImpl.class);
    private IdGenerator generator = new IdGenerator();

    public MarketImpl(String symbol, BookFactory bookFactory) {
        super(symbol, bookFactory);
        this.askOrders = new ArrayList<Order>();
        this.bidOrders = new ArrayList<Order>();
    }

    @Override
    public Order find(String side, String id) {
        List<Order> orders = (Constants.BUY.equals(side)) ? bidOrders : askOrders;
        return find(orders, id);
    }

    @Override
    public boolean insert(Order order) {
        if (order.getQuantity() <= 0.0) { return false; }
        List<Order> orders = order.isBuy() ? bidOrders : askOrders;
        insert(order, orders, false);
        setChanged();
        return true;
    }

    @Override
    public boolean amend(Order order, double price, long quantity) {
        if (quantity <= 0.0 || order.getExecutedQuantity() >= quantity ) { return false; }
        List<Order> orders = order.isBuy() ? bidOrders : askOrders;
        orders.remove(order);
        order.amend(price, quantity);
        insert(order, orders, true);
        setChanged();
        return true;
    }

    @Override
    public boolean erase(Order order) {
        List<Order> orders = order.isBuy() ? bidOrders : askOrders;
        orders.remove(order);
        setChanged();
        return true;
    }

    @Override
    public void match(Order aggressor, List<Order> orders, List<Trade> trades) {
        List<Order> oppositeSide = aggressor.isBuy() ? askOrders : bidOrders;

        while (true) {
            if (oppositeSide.size() == 0) {
                break;
            }

            Order order = oppositeSide.get(0);
            if (aggressor.canTradeWithCounterParty(order) && aggressor.matchesPrice(order.getPrice())) {
                setChanged();
                String executionID = generator.genTradeID();

                match(aggressor, order, executionID);
                orders.add(order.copy());
                orders.add(aggressor.copy());

                Trade trade = new Trade(executionID, symbol, order.getLastExecutedPrice(), order.getLastExecutedQuantity(),
                        (order.isBuy() ? order : aggressor), (order.isSell() ? order : aggressor), aggressor.getOrderId());
                trades.add(trade);

                if (order.isClosed()) {
                    erase(order);
                }
                if (aggressor.isClosed()) {
                    erase(aggressor);
                    break;
                }
            }
            else {
                break;
            }
        }
    }

    @Override
    public void updateObservers() {
        if (!hasChanged()) {
            return;
        }
        display();
        notifyObservers(bookFactory, symbol, askOrders, bidOrders);
    }

    @Override
    public void display() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Market[" + symbol + "]: ");
            displaySide(askOrders, "  ASK");
            displaySide(bidOrders, "  BID");
        }
    }

    private void insert(Order order, List<Order> orders, boolean onAmend) {
        boolean added = false;
        if (orders.size() == 0) {
            orders.add(order);
        } else {
            for (int i = 0; i < orders.size(); i++) {
                Order o = orders.get(i);
                if ( ( onAmend && order.getPrice() == o.getPrice() && order.getEntryTime() < o.getEntryTime() ) ||
                        ( order.isBuy() ? order.getPrice() > o.getPrice() : order.getPrice() < o.getPrice()) ) {
                    orders.add(i, order);
                    added = true;
                    break;
                }
            }
            if (!added) orders.add(order);
        }
    }

    private void match(Order aggressor, Order order, String executionID) {
        long quantity;
        if (aggressor.getOpenQuantity() >= order.getOpenQuantity()) {
            quantity = order.getOpenQuantity();
        } else {
            quantity = aggressor.getOpenQuantity();
        }
        aggressor.execute(order.getPrice(), quantity, executionID, order.getAccount());
        order.execute(order.getPrice(), quantity, executionID, aggressor.getAccount());
    }

    private Order find(List<Order> orders, String id) {
        for (Order order : orders) {
            if (order.getOrderId().equals(id)) return order;
        }
        return null;
    }

    private void displaySide(List<Order> orders, String side) {
        DecimalFormat priceFormat = new DecimalFormat("###.#####");
        DecimalFormat qtyFormat = new DecimalFormat("########");
        for (Order order : orders) {
            LOGGER.info(side + "|" +
                    order.getOrderId() + "|" +
                    order.getAccount() + "|" +
                    priceFormat.format(order.getPrice()) + "|" +
                    qtyFormat.format(order.getOpenQuantity()));
        }
    }
}
