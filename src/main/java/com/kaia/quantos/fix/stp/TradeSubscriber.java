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

import com.kaia.quantos.core.stp.Trade;
import com.kaia.quantos.core.utils.Observer;
import quickfix.SessionID;

import java.util.Set;

public class TradeSubscriber implements Observer<Trade> {
    private final TradeSubscriberManager subscriptionManager;
    private final SessionID sessionID;
    private final Set<String> filterSet;

    public TradeSubscriber(TradeSubscriberManager subscriptionManager, SessionID sessionID, Set<String> filterSet) {
        this.subscriptionManager = subscriptionManager;
        this.sessionID = sessionID;
        this.filterSet = filterSet;
    }

    @Override
    public void update(Trade trade) {
        subscriptionManager.sendTradeReport(sessionID, trade, filterSet);
    }
}
