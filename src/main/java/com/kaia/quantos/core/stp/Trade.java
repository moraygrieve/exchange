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

package com.kaia.quantos.core.stp;

import com.kaia.quantos.core.engine.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trade {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(Trade.class);
    private final String tradeID;
    private final long tradeTime;
    private final String symbol;
    private final double price;
    private final long quantity;
    private final String buyOrderID;
    private final String buyClientOrderID;
    private final String buyAccount;
    private final String buyTraderID;
    private final String buyType;
    private final String sellOrderID;
    private final String sellClientOrderID;
    private final String sellAccount;
    private final String sellTraderID;
    private final String sellType;
    private final String aggressorID;

    public Trade(String tradeID, long tradeTime, String symbol, double price, long quantity,
                 String buyOrderID, String buyClientOrderID, String buyAccount, String buyTraderID, String buyType,
                 String sellOrderID, String sellClientOrderID, String sellAccount, String sellTraderID, String sellType,
                 String aggressorID) {
        this.tradeID = tradeID;
        this.tradeTime = tradeTime;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.buyOrderID = buyOrderID;
        this.buyClientOrderID = buyClientOrderID;
        this.buyAccount = buyAccount;
        this.buyTraderID = buyTraderID;
        this.buyType = buyType;
        this.sellOrderID = sellOrderID;
        this.sellClientOrderID = sellClientOrderID;
        this.sellAccount = sellAccount;
        this.sellTraderID = sellTraderID;
        this.sellType = sellType;
        this.aggressorID = aggressorID;
    }

    public Trade(String tradeID, String symbol, double price, long quantity, Order buy, Order sell, String aggressorID) {
        this.tradeID = tradeID;
        this.tradeTime = System.currentTimeMillis();
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.buyOrderID = buy.getOrderId();
        this.buyClientOrderID = buy.getClientOrderId();
        this.buyAccount = buy.getAccount();
        this.buyTraderID = buy.getTraderId();
        this.buyType = buy.getType();
        this.sellOrderID = sell.getOrderId();
        this.sellClientOrderID = sell.getClientOrderId();
        this.sellAccount = sell.getAccount();
        this.sellTraderID = sell.getTraderId();
        this.sellType = sell.getType();
        this.aggressorID = aggressorID;
    }

    public String getTradeID() {
        return tradeID;
    }

    public long getTradeTime() {
        return tradeTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public String getBuyOrderID() {
        return buyOrderID;
    }

    public String getBuyClientOrderID() {
        return buyClientOrderID;
    }

    public String getBuyAccount() {
        return buyAccount;
    }

    public String getBuyTraderID() {
        return buyTraderID;
    }

    public String getBuyType() {
        return buyType;
    }

    public String getSellOrderID() {
        return sellOrderID;
    }

    public String getSellClientOrderID() {
        return sellClientOrderID;
    }

    public String getSellAccount() {
        return sellAccount;
    }

    public String getSellTraderID() {
        return sellTraderID;
    }

    public String getSellType() {
        return sellType;
    }

    public String getAggressorID() {
        return aggressorID;
    }

    public void display(String msg) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("TradeReport[" + msg + "]: " + tradeID + "|" +
                    tradeTime + "|" +
                    symbol + "|" +
                    price + "|" +
                    quantity + "|" +
                    "(BUY |" + buyOrderID + "|" + buyClientOrderID + "|" + buyAccount + "|" + buyTraderID + "|" + buyType + ") | " +
                    "(SELL|" + sellOrderID + "|" + sellClientOrderID + "|" + sellAccount + "|" + sellTraderID + "|" + sellType + ")");
        }
    }
}

