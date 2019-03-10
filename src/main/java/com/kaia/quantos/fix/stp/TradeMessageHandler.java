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

package com.kaia.quantos.fix.stp;

import com.kaia.quantos.core.engine.Engine;
import com.kaia.quantos.core.stp.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public class TradeMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeMessageHandler.class);
    private final TradeSubscriberManager tradeSubscriptionManager;

    public TradeMessageHandler(Engine engine) {
        tradeSubscriptionManager = new TradeSubscriberManager(this, engine);
    }

    public void remove(SessionID sessionID) {
        tradeSubscriptionManager.remove(sessionID);
    }

    public void onMessage(quickfix.fix44.TradeCaptureReportRequest message, SessionID session) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        TradeRequestID requestID = new TradeRequestID();
        TradeRequestType tradeRequestType = new TradeRequestType();
        SubscriptionRequestType subscriptionRequestType = new SubscriptionRequestType();
        message.get(requestID);
        message.get(tradeRequestType);
        message.get(subscriptionRequestType);

        if (subscriptionRequestType.valueEquals(SubscriptionRequestType.SNAPSHOT) || subscriptionRequestType.valueEquals(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES)) {
            Set<String> filterSet = new HashSet<String>();

            NoPartyIDs noPartyIDs = new NoPartyIDs();
            if ( message.isSet(noPartyIDs) ) {
                message.get(noPartyIDs);

                int i = 1;
                quickfix.fix44.TradeCaptureReportRequest.NoPartyIDs group = new quickfix.fix44.TradeCaptureReportRequest.NoPartyIDs();
                while (i <= noPartyIDs.getValue() ) {
                    PartyID ID = new PartyID();
                    message.getGroup(i, group);
                    group.get(ID);
                    filterSet.add(ID.getValue());
                    i++;
                }
            }

            if ( filterSet.size() == 0 ) {
                sendTradeReportRequestAck(session, requestID, tradeRequestType, TradeRequestResult.INVALID_PARTIES, TradeRequestStatus.REJECTED);
            }
            else {
                tradeSubscriptionManager.add(session, requestID, filterSet);
                sendTradeReportRequestAck(session, requestID, tradeRequestType, TradeRequestResult.SUCCESSFUL, TradeRequestStatus.ACCEPTED);
            }
        }
        else if (subscriptionRequestType.valueEquals(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST)) {
            tradeSubscriptionManager.remove(session, requestID);
            sendTradeReportRequestAck(session, requestID, tradeRequestType, TradeRequestResult.SUCCESSFUL, TradeRequestStatus.ACCEPTED);
        }
    }

    public void onMessage(quickfix.fix44.TradeCaptureReportAck message, SessionID session) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        TradeReportID tradeReportID = new TradeReportID();
        message.get(tradeReportID);

        tradeSubscriptionManager.processReportAcknowledgement(session, tradeReportID);
    }

    private void sendTradeReportRequestAck(SessionID session, TradeRequestID requestID, TradeRequestType tradeRequestType, int result, int status) {
        quickfix.fix44.TradeCaptureReportRequestAck ack = new quickfix.fix44.TradeCaptureReportRequestAck(
                requestID,
                tradeRequestType,
                new TradeRequestResult(result),
                new TradeRequestStatus(status)
        );

        try {
            Session.sendToTarget(ack, session.getSenderCompID(), session.getTargetCompID());
        } catch (SessionNotFound e) {
            LOGGER.error("Error sending trade report request ack to client", e);
        }
    }

    public void sendTradeReport(SessionID session, Trade trade, Set<String> filterSet, boolean previouslyReported) {
        if ( filterSet.contains(trade.getSellAccount()) || filterSet.contains(trade.getBuyAccount()) ) {
            SimpleDateFormat tradeDate = new SimpleDateFormat("yyyyMMdd");
            Date tradeTime = new Date(trade.getTradeTime());
            int sides = 0;

            quickfix.fix44.TradeCaptureReport report = new quickfix.fix44.TradeCaptureReport(
                    new TradeReportID(trade.getTradeID()),
                    new PreviouslyReported(previouslyReported),
                    new LastQty(trade.getQuantity()),
                    new LastPx(trade.getPrice()),
                    new TradeDate(tradeDate.format(tradeTime)),
                    new TransactTime(tradeTime)
            );

            // sell side
            if ( filterSet.contains(trade.getSellAccount()) ) {
                quickfix.fix44.TradeCaptureReport.NoSides sellSide = new quickfix.fix44.TradeCaptureReport.NoSides();
                sellSide.set(new Side(Side.SELL));
                sellSide.set(new OrderID(trade.getSellOrderID()));
                sellSide.set(new ClOrdID(trade.getSellClientOrderID()));
                sellSide.set(new Account(trade.getSellAccount()));
                sellSide.set(new OrdType(OrdType.LIMIT));

                int parties = 0;
                if ( !trade.getBuyAccount().equals("") ) {
                    parties++;
                    quickfix.fix44.component.Parties.NoPartyIDs contraFirm = new quickfix.fix44.component.Parties.NoPartyIDs();
                    contraFirm.set(new PartyID(trade.getBuyAccount()));
                    contraFirm.set(new PartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE));
                    contraFirm.set(new PartyRole(PartyRole.CONTRA_FIRM));
                    sellSide.addGroup(contraFirm);
                }

                if ( !trade.getBuyTraderID().equals("") ) {
                    parties++;
                    quickfix.fix44.component.Parties.NoPartyIDs contraTrader = new quickfix.fix44.component.Parties.NoPartyIDs();
                    contraTrader.set(new PartyID(trade.getBuyTraderID()));
                    contraTrader.set(new PartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE));
                    contraTrader.set(new PartyRole(PartyRole.CONTRA_TRADER));
                    sellSide.addGroup(contraTrader);
                }

                if ( !trade.getSellTraderID().equals("") ) {
                    parties++;
                    quickfix.fix44.component.Parties.NoPartyIDs originatingTrader = new quickfix.fix44.component.Parties.NoPartyIDs();
                    originatingTrader.set(new PartyID(trade.getSellTraderID()));
                    originatingTrader.set(new PartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE));
                    originatingTrader.set(new PartyRole(PartyRole.ORDER_ORIGINATION_TRADER));
                    sellSide.addGroup(originatingTrader);
                }
                sellSide.set(new NoPartyIDs(parties));
                report.addGroup(sellSide);
                sides++;
            }

            // buy side
            if ( filterSet.contains(trade.getBuyAccount()) ) {
                quickfix.fix44.TradeCaptureReport.NoSides buySide = new quickfix.fix44.TradeCaptureReport.NoSides();
                buySide.set(new Side(Side.BUY));
                buySide.set(new OrderID(trade.getBuyOrderID()));
                buySide.set(new ClOrdID(trade.getBuyClientOrderID()));
                buySide.set(new Account(trade.getBuyAccount()));
                buySide.set(new OrdType(OrdType.LIMIT));

                int parties = 0;
                if ( !trade.getSellAccount().equals("") ) {
                    parties++;
                    quickfix.fix44.component.Parties.NoPartyIDs contraFirm = new quickfix.fix44.component.Parties.NoPartyIDs();
                    contraFirm.set(new PartyID(trade.getSellAccount()));
                    contraFirm.set(new PartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE));
                    contraFirm.set(new PartyRole(PartyRole.CONTRA_FIRM));
                    buySide.addGroup(contraFirm);
                }

                if ( !trade.getSellTraderID().equals("") ) {
                    parties++;
                    quickfix.fix44.component.Parties.NoPartyIDs contraTrader = new quickfix.fix44.component.Parties.NoPartyIDs();
                    contraTrader.set(new PartyID(trade.getSellTraderID()));
                    contraTrader.set(new PartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE));
                    contraTrader.set(new PartyRole(PartyRole.CONTRA_TRADER));
                    buySide.addGroup(contraTrader);
                }

                if ( !trade.getBuyTraderID().equals("") ) {
                    parties++;
                    quickfix.fix44.component.Parties.NoPartyIDs originatingTrader = new quickfix.fix44.component.Parties.NoPartyIDs();
                    originatingTrader.set(new PartyID(trade.getBuyTraderID()));
                    originatingTrader.set(new PartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE));
                    originatingTrader.set(new PartyRole(PartyRole.ORDER_ORIGINATION_TRADER));
                    buySide.addGroup(originatingTrader);
                }
                buySide.set(new NoPartyIDs(parties));
                report.addGroup(buySide);
                sides++;
            }

            report.set(new Symbol(trade.getSymbol()));
            report.set(new ExecType(ExecType.FILL));
            report.set(new NoSides(sides));

            try {
                Session.sendToTarget(report, session.getSenderCompID(), session.getTargetCompID());
                LOGGER.info("TradeReport[" + trade.getTradeID() + "]: Sent to " + session.getTargetCompID());
            } catch (SessionNotFound e) {
                LOGGER.error("Error sending trade report to client", e);
            }
        }
    }
}