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

package com.kaia.exchange.core.stp;

import com.kaia.exchange.core.utils.Observable;
import com.kaia.exchange.core.utils.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TradeCache extends Observable<Trade> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(TradeCache.class);
    private static TradeCache ourInstance = null;
    private List<Trade> cache;
    private TradePersistence storage;

    public static void initialise(TradePersistence storage) {
        if (ourInstance == null) {
            ourInstance = new TradeCache();
            ourInstance.cache = new ArrayList<Trade>();
            ourInstance.storage = storage;
        }
    }

    private TradeCache() {
    }

    public static TradeCache getInstance() {
        return ourInstance;
    }

    public void add(Trade trade, String message, boolean persist) {
        trade.display(message);
        cache.add(trade);
        if (persist ) {
            storage.persist(trade);
        }
        setChanged();
        notifyObservers(trade);
    }

    public void getTradesForSubscriber(Observer<Trade> subscriber) {
        for (Trade trade : cache) {
            notifyObserver(subscriber, trade);
        }
    }
}
