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

package com.kaia.quantos.fix.market;

import com.kaia.quantos.core.engine.Engine;
import com.kaia.quantos.core.utils.Observer;
import com.kaia.quantos.fix.utils.MessageStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.MDReqID;
import quickfix.field.NoRelatedSym;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;

public class BookMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookMessageHandler.class);
    private final BookSubscriptionManager mdSubscriptionManager;
    private final MessageStats stats;
    private Thread statsThread;

    public BookMessageHandler(Engine engine) {
        stats = new MessageStats(BookMessageHandler.class);
        mdSubscriptionManager = new BookSubscriptionManager(engine, this);

        statsThread = new Thread() {
            public void run() {
                while (true)  {
                    try {
                        Thread.sleep(5000);
                        stats.log();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }


    public void addSubscriptionObserver(Observer<BookSubscriber> t) {
        mdSubscriptionManager.addObserver(t);
    }

    public void remove(SessionID session) {
        if ( mdSubscriptionManager.hasSubscriptions(session) ) {
            LOGGER.info("Clearing market data subscriptions on session disconnect " + session.toString());
            mdSubscriptionManager.remove(session);
        }
    }

    public void onMessage(quickfix.fix44.MarketDataRequest message, SessionID session) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        SubscriptionRequestType requestType = new SubscriptionRequestType();
        NoRelatedSym noRelatedSym = new NoRelatedSym();
        MDReqID mdReqID = new MDReqID();
        message.get(requestType);
        message.get(noRelatedSym);
        message.get(mdReqID);

        if ( statsThread.getState().equals(Thread.State.NEW) ) {
            stats.resetStart();
            statsThread.start();
        }

        quickfix.fix44.MarketDataRequest.NoRelatedSym noRelatedSyms = new quickfix.fix44.MarketDataRequest.NoRelatedSym();
        for (int i = 1; i <= noRelatedSym.getValue(); ++i) {
            message.getGroup(i, noRelatedSyms);
            Symbol symbol = new Symbol();
            noRelatedSyms.get(symbol);

            if (requestType.valueEquals(SubscriptionRequestType.SNAPSHOT) || requestType.valueEquals(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES)) {
                mdSubscriptionManager.add(session, symbol.getValue(), requestType, mdReqID);
            }
            else if (requestType.valueEquals(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST)) {
                mdSubscriptionManager.remove(session, mdReqID);
            }
        }
    }

    public void sendMessage(quickfix.fix44.Message message, SessionID session) {
        try {
            stats.process();
            Session.sendToTarget(message, session.getSenderCompID(), session.getTargetCompID());
            message.clear();
        } catch (SessionNotFound e) {
            LOGGER.error("Error sending market update to client", e);
        }
    }
}