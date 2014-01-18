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

package com.kaia.exchange.core.engine;

import com.kaia.exchange.core.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Order implements Cloneable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Order.class);
    private final long entryTime;
    private final String orderId;
    private String clientOrderId;
    private final String account;
    private final String traderId;
    private final String symbol;
    private final String side;
    private final String type;
    private double price;
    private long quantity;
    private List<String> counterParties;
    private long executedQuantity;
    private long remainingQuantity;
    private long lockedQuantity;
    private double avgExecutedPrice;
    private double lastExecutedPrice;
    private long lastExecutedQuantity;
    private String lastExecutionId;
    private String lastCounterParty;

    public Order(String orderId, String clientOrderId, String account, String traderId, String symbol, String side, String type,
                 double price, long quantity, List<String> counterParties) {
        super();
        this.orderId = orderId;
        this.clientOrderId = clientOrderId;
        this.account = account;
        this.traderId = traderId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.counterParties = counterParties;
        this.remainingQuantity = quantity;
        this.lockedQuantity = 0;
        this.entryTime = System.currentTimeMillis();
        display("CREATED");
    }

    public Order copy() {
        Order order = null;
        try {
            order = (Order) this.clone();
        } catch (CloneNotSupportedException e) {
            LOGGER.error("Unable to clone order container",e);
        }
        return order;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public long getEntryTime() {
        return entryTime;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getAccount() {
        return account;
    }

    public String getTraderId() {
        return traderId;
    }

    public String getSide() {
        return side;
    }

    public void setClientOrderId(String id) {
        clientOrderId = id;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public void setRemainingQuantity(long quantity) {
        this.remainingQuantity = quantity;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public String getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public List<String> getCounterParties() {
        return counterParties;
    }

    public long getRemainingQuantity() {
        return remainingQuantity;
    }

    public long getOpenQuantity() {
        return remainingQuantity-lockedQuantity;
    }

    public long getExecutedQuantity() {
        return executedQuantity;
    }

    public double getAvgExecutedPrice() {
        return avgExecutedPrice;
    }

    public long getLastExecutedQuantity() {
        return lastExecutedQuantity;
    }

    public double getLastExecutedPrice() {
        return lastExecutedPrice;
    }

    public String getLastExecutionId() {
        return lastExecutionId;
    }

    public String getLastCounterParty() {
        return lastCounterParty;
    }

    public boolean isFilled() {
        return quantity == executedQuantity;
    }

    public void cancel() {
        remainingQuantity = 0;
        display("CANCELLED");
    }

    public void amend(double price, long quantity) {
        setPrice(price);
        setQuantity(quantity);
        setRemainingQuantity(quantity - getExecutedQuantity());
        display("AMENDED");
    }

    public boolean isClosed() {
        return remainingQuantity == 0;
    }

    public boolean isBuy() {
        return (side.equals(Constants.BUY));
    }

    public boolean isSell() {
        return (side.equals(Constants.SELL));
    }

    public boolean canTradeWithCounterParty(Order order) {
        return order.getCounterParties().indexOf(account) >=0 && counterParties.indexOf(order.getAccount()) >= 0;
    }

    public boolean matchesPrice(double price) {
        return (isBuy() ? getPrice() >= price : getPrice() <= price);
    }

    public void execute(double price, long quantity, String executionID, String counterParty) {
        avgExecutedPrice = ((quantity * price) + (avgExecutedPrice * executedQuantity)) / (quantity + executedQuantity);
        remainingQuantity -= quantity;
        executedQuantity += quantity;
        lastExecutedPrice = price;
        lastExecutedQuantity = quantity;
        lastExecutionId = executionID;
        lastCounterParty = counterParty;
        display(isFilled() ? "FILLED":"PARTIAL");
    }

    public void display(String msg) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Order[" + getOrderId() + "]: " + msg + "|" +
                    getAccount() + "|" +
                    getTraderId() + "|" +
                    getSymbol() + "|" +
                    getSide() + "|" +
                    getPrice() + "|" +
                    getQuantity() + "|" +
                    getExecutedQuantity() + "|" +
                    getOpenQuantity());
        }
    }
}