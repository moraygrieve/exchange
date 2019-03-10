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

import com.kaia.quantos.core.utils.Pair;
import quickfix.SessionID;

import java.util.LinkedList;
import java.util.Map;

public class OrderTracker {
    private final SessionID session;
    private String symbol;
    private String side;
    private String orderId;
    private String clOrdId;
    private LinkedList<Pair<String,String>> changeRequests;
    private boolean pendingChange;

    public OrderTracker(SessionID session, String symbol, String side, String orderId, String clOrdId) {
        this.session = session;
        this.symbol = symbol;
        this.side = side;
        this.orderId = orderId;
        this.clOrdId = clOrdId;
        changeRequests = new LinkedList<Pair<String, String>>();
        pendingChange = false;
    }

    public SessionID getSession() {
        return session;
    }

    public String getSenderCompID() {
        return session.getSenderCompID();
    }

    public String getTargetCompID() {
        return session.getTargetCompID();
    }

    public String getSymbol() {
        return symbol;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSide() {
        return side;
    }

    public String getClOrdId() {
        return clOrdId;
    }

    public void addChange(String clOrdId, String origClOrdId) {
        changeRequests.addLast(new Pair<String,String>(clOrdId, origClOrdId));
        pendingChange = true;
    }

    public String getChangeClOrdId() {
        if ( changeRequests.size() == 0 ) { return clOrdId; }
        return changeRequests.getLast().getLeft();
    }

    public String getChangeOrigClOrdId() {
        if ( changeRequests.size() == 0 ) { return clOrdId; }
        return changeRequests.getLast().getRight();
    }

    public boolean isPendingChange() {
        return pendingChange;
    }

    public String changeAccepted() {
        if ( !pendingChange ) {
            return clOrdId;
        }
        else {
            pendingChange = false;
            clOrdId = changeRequests.getLast().getLeft();
            return clOrdId;
        }
    }

    public void changeRejected() {
        pendingChange = false;
    }

    public void removeClOrdIdEntries(Map<String, String> clOrderIdMap) {
        for (Pair pair: changeRequests) {
            if ( clOrderIdMap.containsKey(pair.getLeft()) ) {
                clOrderIdMap.remove(pair.getLeft());
            }
        }
    }
}
