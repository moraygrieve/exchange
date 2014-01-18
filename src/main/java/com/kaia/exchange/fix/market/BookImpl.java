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

package com.kaia.exchange.fix.market;

import com.kaia.exchange.core.market.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.field.*;
import quickfix.fix44.MarketDataIncrementalRefresh;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

public class BookImpl extends Book {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookImpl.class);
    private Map<Double,Long> asks;
    private Map<Double,Long> bids;

    public BookImpl(String symbol, String account) {
        this(symbol, account, new TreeMap<Double, Long>(), new TreeMap<Double, Long>());
    }

    public BookImpl(String symbol, String account, Map<Double, Long> asks, Map<Double, Long> bids) {
        super(symbol, account);
        this.asks = asks;
        this.bids = bids;
    }

    public MarketDataSnapshotFullRefresh snapshot(Book newBook)
            throws IllegalArgumentException {
        if (!(newBook instanceof BookImpl)) {
            throw new IllegalArgumentException("Supplied book is not an instance of BookImpl");
        }
        BookImpl book = (BookImpl) newBook;
        MarketDataSnapshotFullRefresh message = new MarketDataSnapshotFullRefresh();
        MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();
        addForSnapshot(book.asks, message, group, new MDEntryType(MDEntryType.OFFER));
        addForSnapshot(book.bids, message, group, new MDEntryType(MDEntryType.BID));
        return message;
    }

    private void addForSnapshot(Map<Double, Long> side, MarketDataSnapshotFullRefresh message,
                                MarketDataSnapshotFullRefresh.NoMDEntries group, MDEntryType entryType) {
        for (Map.Entry<Double, Long> entry : side.entrySet()) {
            String entryID = getEntryID(entry.getKey(), entryType);
            group.set(entryType);
            group.setField(new MDEntryID(entryID) );
            group.setField(new Symbol(symbol));
            group.set(new MDEntryPx(entry.getKey()));
            group.set(new MDEntrySize(entry.getValue()));
            message.addGroup(group);
        }
    }

    public MarketDataIncrementalRefresh incremental(Book newBook)
            throws IllegalArgumentException {
        if (!(newBook instanceof BookImpl)) {
            throw new IllegalArgumentException("Supplied book is not an instance of BookImpl");
        }
        BookImpl book = (BookImpl) newBook;
        MarketDataIncrementalRefresh message = new MarketDataIncrementalRefresh();
        MarketDataIncrementalRefresh.NoMDEntries group = new MarketDataIncrementalRefresh.NoMDEntries();
        processAsks(book.asks, message, group);
        processBids(book.bids, message, group);
        return message;
    }

    private void processAsks(Map<Double, Long> _new, MarketDataIncrementalRefresh message, MarketDataIncrementalRefresh.NoMDEntries group) {
        MDEntryType type = new MDEntryType(MDEntryType.OFFER);
        added(_new, asks, message, group, type);
        changed(_new, asks, message, group, type);
        deleted(_new, asks, message, group, type);
    }

    private void processBids(Map<Double, Long> _new, MarketDataIncrementalRefresh message, MarketDataIncrementalRefresh.NoMDEntries group) {
        MDEntryType type = new MDEntryType(MDEntryType.BID);
        added(_new, bids, message, group, type);
        changed(_new, bids, message, group, type);
        deleted(_new, bids, message, group, type);
    }

    private void added(Map<Double, Long> _new, Map<Double, Long> _old,
                       MarketDataIncrementalRefresh message, MarketDataIncrementalRefresh.NoMDEntries group,
                       MDEntryType entryType) {
        for (Map.Entry<Double, Long> entry : _new.entrySet()) {
            if (!_old.containsKey(entry.getKey())) {
                String entryID = getEntryID(entry.getKey(), entryType);
                group.set(new MDUpdateAction(MDUpdateAction.NEW));
                group.set(entryType);
                group.set(new MDEntryID(entryID));
                group.set(new Symbol(symbol));
                group.set(new MDEntryPx(entry.getKey()));
                group.set(new MDEntrySize(entry.getValue()));
                message.addGroup(group);
                LOGGER.info("NEW" + "|" + symbol + "|" + entryID + "|" + entry.getKey() + "|" + entry.getValue());
            }
        }
    }

    private void deleted(Map<Double, Long> _new, Map<Double, Long> _old,
                         MarketDataIncrementalRefresh message, MarketDataIncrementalRefresh.NoMDEntries group,
                         MDEntryType entryType) {
        for (Map.Entry<Double, Long> entry : _old.entrySet()) {
            if (!_new.containsKey(entry.getKey())) {
                String entryID = getEntryID(entry.getKey(), entryType);
                group.set(new MDUpdateAction(MDUpdateAction.DELETE));
                group.set(entryType);
                group.set(new MDEntryID(getEntryID(entry.getKey(), entryType)));
                group.set(new Symbol(symbol));
                message.addGroup(group);
                LOGGER.info("DELETE" + "|" + symbol + "|" + entryID + "|" + entry.getKey() + "|" + entry.getValue());
            }
        }
    }

    private void changed(Map<Double, Long> _new, Map<Double, Long> _old,
                         MarketDataIncrementalRefresh message, MarketDataIncrementalRefresh.NoMDEntries group,
                         MDEntryType entryType) {
        for (Map.Entry<Double, Long> entry : _new.entrySet()) {
            if (_old.containsKey(entry.getKey())) {
                if (!_new.get(entry.getKey()).equals(_old.get(entry.getKey()))) {
                    String entryID = getEntryID(entry.getKey(), entryType);
                    group.set(new MDUpdateAction(MDUpdateAction.CHANGE));
                    group.set(entryType);
                    group.set(new MDEntryID(getEntryID(entry.getKey(), entryType)));
                    group.set(new Symbol(symbol));
                    group.set(new MDEntryPx(entry.getKey()));
                    group.set(new MDEntrySize(entry.getValue()));
                    message.addGroup(group);
                    LOGGER.info("CHANGE" + "|" + symbol + "|" + entryID + "|" + entry.getKey() + "|" + entry.getValue());
                }
            }
        }
    }

    private String getEntryID(double inValue, MDEntryType entryType) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (entryType.getValue() == MDEntryType.OFFER) {
            pw.printf("o-%.5f", inValue);
        } else if (entryType.getValue() == MDEntryType.BID) {
            pw.printf("b-%.5f", inValue);
        }
        return sw.toString();
    }

    @Override
    public void display() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Book["+symbol+"|" +account+"]:");
            displayLevel(asks, "  ASK");
            displayLevel(bids, "  BID");
        }
    }

    private void displayLevel(Map<Double, Long> levels, String side) {
        for (Map.Entry<Double, Long> level : levels.entrySet()) {
            LOGGER.info(side + "|" + level.getValue() + "@" + level.getKey());
        }
    }
}
