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

package com.kaia.exchange.fix.stp;

import com.kaia.exchange.core.engine.Engine;
import com.kaia.exchange.core.stp.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.SessionID;
import quickfix.field.TradeReportID;
import quickfix.field.TradeRequestID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TradeSubscriberManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeSubscriberManager.class);
    private final TradeMessageHandler messageHandler;
    private final Engine engine;
    private Map<SessionID, TradeSubscriber> subscriptions;
    private Map<SessionID, Set<String>> bookedTrades;

    public TradeSubscriberManager(TradeMessageHandler handler, Engine engine) {
        this.messageHandler = handler;
        this.engine = engine;
        subscriptions = new HashMap<SessionID, TradeSubscriber>();
        bookedTrades = new HashMap<SessionID, Set<String>>();
    }

    public void add(SessionID session, TradeRequestID requestID, Set<String> filterSet) {
        if (!bookedTrades.containsKey(session)) {
            bookedTrades.put(session, new HashSet<String>());
        }

        if (!subscriptions.containsKey(session)) {
            LOGGER.info("Adding STP subscription: " + session.getSenderCompID() + "|" + session.getTargetCompID() + "|" + filterSet.toString() + "|" + requestID.getValue());
            TradeSubscriber subscriber = new TradeSubscriber(this, session, filterSet);
            engine.subscribeTradeNotifications(subscriber);
            subscriptions.put(session, subscriber);
        }
    }

    public void remove(SessionID session, TradeRequestID requestID) {
        if (subscriptions.containsKey(session)) {
            LOGGER.info("Removing STP subscription: " + session.getSenderCompID() + "|" + session.getTargetCompID() + "|" + requestID.getValue());
            engine.unsubscribeTradeNotifications(subscriptions.get(session));
            subscriptions.remove(session);
        }
    }

    public void remove(SessionID session) {
        if (subscriptions.containsKey(session)) {
            LOGGER.info("Removing STP subscription: " + session.toString());
            engine.unsubscribeTradeNotifications(subscriptions.get(session));
            subscriptions.remove(session);
        }
    }

    public void processReportAcknowledgement(SessionID session, TradeReportID tradeReportID) {
        if (bookedTrades.containsKey(session)) {
            LOGGER.info("TradeReport[" + tradeReportID.getValue() + "]: Acknowledged from " + session.getTargetCompID());
            bookedTrades.get(session).add(tradeReportID.getValue());
        }
    }

    public void sendTradeReport(SessionID session, Trade trade, Set<String> filterSet) {
        messageHandler.sendTradeReport(session, trade, filterSet, isBooked(session, trade.getTradeID()));
    }

    public boolean isBooked(SessionID session, String tradeReportID) {
        if (bookedTrades.containsKey(session)) {
            return bookedTrades.get(session).contains(tradeReportID);
        }
        return false;
    }
}
