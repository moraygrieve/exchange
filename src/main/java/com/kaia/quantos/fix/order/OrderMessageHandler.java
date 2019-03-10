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

package com.kaia.quantos.fix.order;

import com.kaia.quantos.core.engine.Engine;
import com.kaia.quantos.core.engine.Order;
import com.kaia.quantos.core.engine.OrderGenerator;
import com.kaia.quantos.core.utils.Constants;
import com.kaia.quantos.core.utils.IdGenerator;
import com.kaia.quantos.fix.utils.SettingsAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderMessageHandler implements OrderGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderMessageHandler.class);
    private final Engine engine;
    private final IdGenerator generator;
    private Map<String, OrderTracker> orderIdMap;
    private Map<String, String> clOrderIdMap;

    public OrderMessageHandler(Engine engine) {
        this.engine = engine;
        this.generator = new IdGenerator();
        this.orderIdMap = new HashMap<String, OrderTracker>();
        this.clOrderIdMap = new HashMap<String, String>();
    }

    public void remove(SessionID sessionID) {
        List<String> orderIDs = new ArrayList<String>();
        for (String key : orderIdMap.keySet()) {
            if ( orderIdMap.get(key).getSession().equals(sessionID) ) {
                orderIDs.add(key);
            }
        }

        if ( orderIDs.size() > 0 ) {
            LOGGER.info("Removing orders on session disconnect " + sessionID.toString());
            for (String orderId : orderIDs) {
                LOGGER.info("Removing order " + orderId);
                engine.deleteOrder(this, orderIdMap.get(orderId).getSymbol(), orderIdMap.get(orderId).getSide(), orderIdMap.get(orderId).getOrderId(), true);
            }
        }
    }

    @Override
    public void orderAccepted(Order order, String text) {
        sendExecutionReport(order, ExecType.NEW, OrdStatus.NEW, text);
    }

    @Override
    public void orderRejected(Order order, String text) {
        sendExecutionReport(order, ExecType.REJECTED, OrdStatus.REJECTED, text);
        removeOrderTracker(order.getOrderId());
    }

    @Override
    public void amendAccepted(Order order, String text) {
        sendExecutionReport(order, ExecType.REPLACE, OrdStatus.REPLACED, text);
        order.setClientOrderId(orderIdMap.get(order.getOrderId()).changeAccepted());
    }

    @Override
    public void amendRejected(Order order, String text) {
        sendOrderCancelReject(order, CxlRejResponseTo.ORDER_CANCEL_REPLACE_REQUEST, order.isFilled() ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED, text);
        orderIdMap.get(order.getOrderId()).changeRejected();
    }

    @Override
    public void cancelAccepted(Order order, String text) {
        sendExecutionReport(order, ExecType.CANCELED, OrdStatus.CANCELED, text);
        order.setClientOrderId(orderIdMap.get(order.getOrderId()).changeAccepted());
        removeOrderTracker(order.getOrderId());
    }

    @Override
    public void cancelRejected(Order order, String text) {
        sendOrderCancelReject(order, CxlRejResponseTo.ORDER_CANCEL_REQUEST, order.isFilled() ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED, text);
        orderIdMap.get(order.getOrderId()).changeRejected();
    }

    @Override
    public void orderMatched(Order order, String text) {
        sendExecutionReport(order, ExecType.TRADE, order.isFilled() ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED, text);
        if (order.isFilled()) { removeOrderTracker(order.getOrderId()); }
    }

    @Override
    public void orderLocked(String tradeId, Order order, float price, long quantity) {
    }

    public void onMessage(quickfix.fix44.NewOrderSingle message, SessionID session) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        ClOrdID clOrdID = new ClOrdID();
        Symbol symbol = new Symbol();
        Side side = new Side();
        OrdType ordType = new OrdType();
        Price price = new Price();
        OrderQty orderQty = new OrderQty();
        TimeInForce timeInForce = new TimeInForce(TimeInForce.DAY);
        message.get(clOrdID);
        message.get(symbol);
        message.get(side);
        message.get(ordType);
        message.get(orderQty);
        if (ordType.valueEquals(OrdType.LIMIT)) {
            message.get(price);
        }
        if (message.isSetField(TimeInForce.FIELD)) {
            message.get(timeInForce);
        }

        String traderId = "";
        NoPartyIDs noPartyIDs = new NoPartyIDs();
        if ( message.isSet(noPartyIDs) ) {
            message.get(noPartyIDs);

            int i = 1;
            quickfix.fix44.NewOrderSingle.NoPartyIDs group = new quickfix.fix44.NewOrderSingle.NoPartyIDs();
            while (i <= noPartyIDs.getValue() ) {
                PartyID ID = new PartyID();
                PartyIDSource source = new PartyIDSource();
                PartyRole role = new PartyRole();
                message.getGroup(i, group);
                group.get(ID);
                group.get(source);
                group.get(role);
                if ( source.valueEquals(PartyIDSource.PROPRIETARY_CUSTOM_CODE)  &&  role.valueEquals(PartyRole.ORDER_ORIGINATION_TRADER) ) {
                    traderId = ID.getValue();
                    break;
                }
                i++;
            }
        }

        String orderId = generator.genOrderID();
        Order order = new Order(orderId, clOrdID.getValue(), SettingsAccessor.getAccount(session),
                traderId,  symbol.getValue(), side.valueEquals(Side.BUY) ? Constants.BUY : Constants.SELL,
                Constants.LIMIT, price.getValue(), (long) orderQty.getValue(), SettingsAccessor.getCounterParties(session));

        addOrderTracker(session, symbol.getValue(), side.valueEquals(Side.BUY) ? Constants.BUY : Constants.SELL, orderId, clOrdID.getValue());

        sendExecutionReport(order, ExecType.PENDING_NEW, OrdStatus.PENDING_NEW, "Order pending placement");

        try {
            if (!(timeInForce.valueEquals(TimeInForce.DAY) || timeInForce.valueEquals(TimeInForce.IMMEDIATE_OR_CANCEL))) {
                throw new RuntimeException("Unsupported time in force, use DAY or IOC");
            }
            if (!(ordType.valueEquals(OrdType.LIMIT))) {
                throw new RuntimeException("Unsupported OrdType");
            }

            engine.insertOrder(this, order, true);
        } catch (Exception e) {
            orderRejected(order, e.getMessage());
        }
    }

    public void onMessage(quickfix.fix44.OrderCancelReplaceRequest message, SessionID session) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        Symbol symbol = new Symbol();
        Side side = new Side();
        ClOrdID clOrdID = new ClOrdID();
        OrigClOrdID origClOrdID = new OrigClOrdID();
        Price price = new Price();
        OrderQty orderQty = new OrderQty();
        message.get(symbol);
        message.get(side);
        message.get(clOrdID);
        message.get(origClOrdID);
        message.get(orderQty);
        message.get(price);

        boolean unknownOrder = true;
        if (clOrderIdMap.containsKey(origClOrdID.getValue())) {
            String orderId = clOrderIdMap.get(origClOrdID.getValue());
            if ( orderIdMap.containsKey(orderId)) {
                clOrderIdMap.put(clOrdID.getValue(), orderId);
                OrderTracker tracker = orderIdMap.get(orderId);
                tracker.addChange(clOrdID.getValue(), origClOrdID.getValue());
                unknownOrder = !engine.amendOrder(this, tracker.getSymbol(), tracker.getSide(), tracker.getOrderId(), price.getValue(), (long) orderQty.getValue(), true);
            }
        }

        if (unknownOrder)
            sendOrderCancelRejectOnUnknown(session, clOrdID.getValue(), origClOrdID.getValue(), CxlRejResponseTo.ORDER_CANCEL_REPLACE_REQUEST);

    }

    public void onMessage(quickfix.fix44.OrderCancelRequest message, SessionID session) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        Symbol symbol = new Symbol();
        Side side = new Side();
        ClOrdID clOrdID = new ClOrdID();
        OrigClOrdID origClOrdID = new OrigClOrdID();
        message.get(symbol);
        message.get(side);
        message.get(clOrdID);
        message.get(origClOrdID);

        boolean unknownOrder = false;
        if (clOrderIdMap.containsKey(origClOrdID.getValue())) {
            String orderId = clOrderIdMap.get(origClOrdID.getValue());
            if (orderIdMap.containsKey(orderId)) {
                clOrderIdMap.put(clOrdID.getValue(), orderId);
                OrderTracker tracker = orderIdMap.get(orderId);
                tracker.addChange(clOrdID.getValue(), origClOrdID.getValue());
                unknownOrder = !engine.deleteOrder(this, tracker.getSymbol(), tracker.getSide(), tracker.getOrderId(), true);
            }
        }
        if (unknownOrder)
            sendOrderCancelRejectOnUnknown(session, clOrdID.getValue(), origClOrdID.getValue(), CxlRejResponseTo.ORDER_CANCEL_REQUEST);
    }

    private void sendOrderCancelRejectOnUnknown(SessionID session, String clOrdID, String origClOrdID, char type) {
        quickfix.fix44.OrderCancelReject reject = new quickfix.fix44.OrderCancelReject(
                new OrderID("NONE"),
                new ClOrdID(clOrdID),
                new OrigClOrdID(origClOrdID),
                new OrdStatus(OrdStatus.REJECTED),
                new CxlRejResponseTo(type)
        );

        reject.set(new Text("Unknown order"));

        try {
            Session.sendToTarget(reject, session.getSenderCompID(), session.getTargetCompID());
        } catch (SessionNotFound e) {
            LOGGER.error("Error sending order cancel reject to client", e);
        }
    }

    private void sendOrderCancelReject(Order order, char type, char status, String text) {
        if ( orderIdMap.containsKey(order.getOrderId()) ) {
            OrderTracker tracker = orderIdMap.get(order.getOrderId());

            quickfix.fix44.OrderCancelReject reject = new quickfix.fix44.OrderCancelReject(
                    new OrderID(order.getOrderId()),
                    new ClOrdID(tracker.getChangeClOrdId()),
                    new OrigClOrdID(tracker.getChangeOrigClOrdId()),
                    new OrdStatus(status),
                    new CxlRejResponseTo(type)
            );

            reject.set(new Text(text));

            try {
                Session.sendToTarget(reject, tracker.getSenderCompID(), tracker.getTargetCompID());
            } catch (SessionNotFound e) {
                LOGGER.error("Error sending order cancel reject to client", e);
            }
        }
    }

    private void sendExecutionReport(Order order, char execType, char ordStatus, String text) {
        if ( orderIdMap.containsKey(order.getOrderId()) ) {
            OrderTracker tracker = orderIdMap.get(order.getOrderId());

            quickfix.fix44.ExecutionReport execReport = new quickfix.fix44.ExecutionReport(
                    new OrderID(order.getOrderId()),
                    new ExecID(),
                    new ExecType(execType), new OrdStatus(ordStatus),
                    new Side(order.getSide().equals(Constants.BUY) ? Side.BUY : Side.SELL), new LeavesQty(order.getRemainingQuantity()),
                    new CumQty(order.getExecutedQuantity()), new AvgPx(order.getAvgExecutedPrice()));

            execReport.set(new Symbol(order.getSymbol()));
            execReport.set(new Price(order.getPrice()));
            execReport.set(new OrdType(OrdType.LIMIT));

            if (!orderIdMap.get(order.getOrderId()).isPendingChange()) {
                execReport.set(new ClOrdID(order.getClientOrderId()));
            }
            else {
                execReport.set(new ClOrdID(tracker.getChangeClOrdId()));
                execReport.set(new OrigClOrdID(tracker.getChangeOrigClOrdId()));
            }

            execReport.set(new OrderQty(order.getQuantity()));
            execReport.set(new Text(text));

            switch (execType) {
                case ExecType.TRADE:
                    execReport.set(new LastQty(order.getLastExecutedQuantity()));
                    execReport.set(new LastPx(order.getLastExecutedPrice()));
                    execReport.set(new ExecID(order.getLastExecutionId()));
                    execReport.set(new NoPartyIDs(1));

                    quickfix.fix44.component.Parties.NoPartyIDs noPartyIDs = new quickfix.fix44.component.Parties.NoPartyIDs();
                    noPartyIDs.set(new PartyID(order.getLastCounterParty()));
                    noPartyIDs.set(new PartyIDSource(PartyIDSource.BIC));
                    noPartyIDs.set(new PartyRole(PartyRole.CONTRA_FIRM));
                    execReport.addGroup(noPartyIDs);
                    break;
                default:
                    execReport.set(new ExecID(generator.genUpdateID()));
                    break;
            }

            try {
                Session.sendToTarget(execReport, tracker.getSenderCompID(), tracker.getTargetCompID());
            } catch (SessionNotFound e) {
                LOGGER.error("Error sending execution report to client", e);
            }
        }
    }

    private void addOrderTracker(SessionID session, String symbol, String side, String orderId, String clOrdId) {
        if (!orderIdMap.containsKey(orderId)) {
            orderIdMap.put(orderId, new OrderTracker(session, symbol, side, orderId, clOrdId));
            clOrderIdMap.put(clOrdId, orderId);
        }
    }

    private void removeOrderTracker(String orderId) {
        if (orderIdMap.containsKey(orderId)) {
            orderIdMap.get(orderId).removeClOrdIdEntries(clOrderIdMap);
            orderIdMap.remove(orderId);
        }
    }
}