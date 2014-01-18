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

package com.kaia.exchange.fix;

import com.kaia.exchange.core.engine.Engine;
import com.kaia.exchange.core.utils.Observer;
import com.kaia.exchange.fix.market.BookMessageHandler;
import com.kaia.exchange.fix.market.BookSubscriber;
import com.kaia.exchange.fix.order.OrderMessageHandler;
import com.kaia.exchange.fix.stp.TradeMessageHandler;
import com.kaia.exchange.fix.utils.SettingsAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.MsgType;

public class MessageHandler extends quickfix.fix44.MessageCracker implements quickfix.Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);
    private final BookMessageHandler bookHandler;
    private final OrderMessageHandler orderHandler;
    private final TradeMessageHandler tradeHandler;

    public MessageHandler(Engine engine) {
        bookHandler = new BookMessageHandler(engine);
        orderHandler = new OrderMessageHandler(engine);
        tradeHandler = new TradeMessageHandler(engine);
    }

    public void addSubscriptionObserver(Observer<BookSubscriber> t) {
        bookHandler.addSubscriptionObserver(t);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        String type = message.getHeader().getString(MsgType.FIELD);
        if (type.equals(MsgType.LOGOUT)) {
            orderHandler.remove(sessionId);
            bookHandler.remove(sessionId);
            tradeHandler.remove(sessionId);
        }
        else if (type.equals(MsgType.LOGON)) {
            LOGGER.info("Received logon : " + sessionId.getSenderCompID() + "|" + sessionId.getTargetCompID() + "|" +
                    SettingsAccessor.getAccount(sessionId) + "|" + SettingsAccessor.getCounterParties(sessionId) + "|");
        }
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        try {
            crack(message, sessionId);
        }
        catch(Exception e) {
            LOGGER.error("Exception cracking message",e);
        }
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
    }

    @Override
    public void onCreate(SessionID sessionId) {
    }

    @Override
    public void onLogon(SessionID sessionId) {
    }

    @Override
    public void onLogout(SessionID sessionId) {
    }

    @Override
    public void onMessage(quickfix.fix44.TradeCaptureReportRequest message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        tradeHandler.onMessage(message, sessionID);
    }

    @Override
    public void onMessage(quickfix.fix44.TradeCaptureReportAck message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        tradeHandler.onMessage(message, sessionID);
    }

    @Override
    public void onMessage(quickfix.fix44.MarketDataRequest message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        bookHandler.onMessage(message, sessionID);
    }

    @Override
    public void onMessage(quickfix.fix44.NewOrderSingle message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        orderHandler.onMessage(message, sessionID);
    }

    @Override
    public void onMessage(quickfix.fix44.OrderCancelReplaceRequest message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        orderHandler.onMessage(message, sessionID);
    }

    @Override
    public void onMessage(quickfix.fix44.OrderCancelRequest message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        orderHandler.onMessage(message, sessionID);
    }
}
