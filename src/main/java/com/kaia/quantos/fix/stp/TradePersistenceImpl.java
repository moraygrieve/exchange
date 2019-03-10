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

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.kaia.quantos.core.stp.Trade;
import com.kaia.quantos.core.stp.TradeCache;
import com.kaia.quantos.core.stp.TradePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Calendar;

// @TODO better abstraction over persistence of the trade object
public class TradePersistenceImpl implements TradePersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradePersistenceImpl.class);
    private final SQLiteConnection db;
    private SQLiteStatement insertStatement = null;

    public TradePersistenceImpl(String database) {
        java.util.logging.Logger.getLogger("com.almworks.sqlite4java").setLevel(java.util.logging.Level.OFF);

        db = new SQLiteConnection(new File(database));
        try {
            db.open(false);
            LOGGER.info("Initialised database " + database + " successfully");
        } catch (SQLiteException e) {
            LOGGER.error("Error initialising database", e);
        }

        try {
            insertStatement = db.prepare("insert into Trade values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        } catch (SQLiteException e) {
            LOGGER.error("Error preparing insert statement for trade persistence", e);
        }
    }

    @Override
    public boolean persist(Trade trade) {
        if (insertStatement == null) { return false; }

        try {
            insertStatement.bind(1, trade.getTradeID());
            insertStatement.bind(2, trade.getTradeTime());
            insertStatement.bind(3, trade.getSymbol());
            insertStatement.bind(4, trade.getPrice());
            insertStatement.bind(5, trade.getQuantity());
            insertStatement.bind(6, trade.getBuyOrderID());
            insertStatement.bind(7, trade.getBuyClientOrderID());
            insertStatement.bind(8, trade.getBuyAccount());
            insertStatement.bind(9, trade.getBuyTraderID());
            insertStatement.bind(10, trade.getBuyType());
            insertStatement.bind(11, trade.getSellOrderID());
            insertStatement.bind(12, trade.getSellClientOrderID());
            insertStatement.bind(13, trade.getSellAccount());
            insertStatement.bind(14, trade.getSellTraderID());
            insertStatement.bind(15, trade.getSellType());
            insertStatement.bind(16, trade.getAggressorID());
            insertStatement.step();
            insertStatement.reset();
        } catch (SQLiteException e) {
            LOGGER.error("Error persisting trade ", e);
            return false;
        }
        return true;
    }

    public boolean load(TradeCache cache) {
        SQLiteStatement st = null;
        try {
            st = db.prepare("select * from Trade where tradeTime >= " + startOfDay() + ";");
            while (st.step()) {
                Trade trade = new Trade(
                        st.columnString(0),
                        st.columnLong(1),
                        st.columnString(2),
                        st.columnDouble(3),
                        st.columnLong(4),
                        st.columnString(5),
                        st.columnString(6),
                        st.columnString(7),
                        st.columnString(8),
                        st.columnString(9),
                        st.columnString(10),
                        st.columnString(11),
                        st.columnString(12),
                        st.columnString(13),
                        st.columnString(14),
                        st.columnString(15));
                cache.add(trade, "LOADING", false);
            }
        } catch (SQLiteException e) {
            LOGGER.error("Error loading trades ", e);
            return false;
        } finally {
            if (st != null) {
                st.dispose();
            }
        }
        return true;
    }

    private long startOfDay() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        return today.getTimeInMillis();
    }
}
