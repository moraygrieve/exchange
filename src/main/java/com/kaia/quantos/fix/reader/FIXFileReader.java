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

package com.kaia.quantos.fix.reader;

import com.kaia.quantos.core.engine.Engine;
import com.kaia.quantos.core.engine.Order;
import com.kaia.quantos.core.engine.OrderGenerator;
import com.kaia.quantos.core.utils.Constants;
import com.kaia.quantos.core.utils.IdGenerator;
import com.kaia.quantos.core.utils.Observer;
import com.kaia.quantos.fix.market.BookSubscriber;
import com.kaia.quantos.fix.utils.MessageStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.FieldConvertError;
import quickfix.SessionSettings;
import quickfix.field.*;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FIXFileReader implements OrderGenerator, Runnable, Observer<BookSubscriber>  {
    private static final String REPLAY_FILE = "ReplayFile";
    private static final String REPLAY_DATA_DICTIONARY = "ReplayDataDictionary";
    private static final String REPLAY_ID = "ReplayID";
    private static final String REPLAY_ACCOUNT = "ReplayAccount";
    private static final String REPLAY_TRADER = "ReplayTrader";
    private static final String REPLAY_MODE = "ReplayMode";
    private static final String REPLAY_NANO_SLEEP = "ReplayNanoSleep";
    private static final String REPLAY_RAMP_FACTOR = "ReplayRampFactor";
    private static final String REPLAY_LOOP_COUNT = "ReplayLoopCount";
    private static final String REPLAY_WAIT_SYMBOL = "ReplayWaitSymbol";
    private static final String REPLAY_FORWARD_TO = "ReplayForwardTo";
    private static final Logger LOGGER = LoggerFactory.getLogger(FIXFileReader.class);

    class OrderContainer {
        String orderId;
        String symbol;
        String side;

        OrderContainer(String orderId, String symbol, String side) {
            this.orderId = orderId;
            this.symbol = symbol;
            this.side = side;
        }
    }

    private enum ReplayMode {
        REAL, FIXED, FAST, UNKNOWN;

        private static final Map<String, ReplayMode> nameToValueMap = new HashMap<String, ReplayMode>();
        static {
            for (ReplayMode value : EnumSet.allOf(ReplayMode.class))
                nameToValueMap.put(value.name(), value);
        }

        public static ReplayMode forName(String name) {
            if ( nameToValueMap.containsKey(name) )
                return nameToValueMap.get(name);
            return ReplayMode.UNKNOWN;
        }
    }

    private final Engine engine;
    private final IdGenerator generator;
    private Map<String, OrderContainer> orderDetails;
    private Map<String, String> orderMap;
    private String file;
    private String dataDictionary;
    private String targetCompID = "SIMULATOR";
    private String account = "ACT";
    private String traderID = "atrader";
    private boolean running = false;
    private Object lock = new Object();
    private ReplayMode replayMode = ReplayMode.REAL;
    private long replayNanoSleep = 500000;
    private double replayRampFactor = 0.9;
    private long replayLoopCount = 1;
    private String replayWaitSymbol = null;
    private String replayForwardTo = null;
    private List<String> counterParties = new ArrayList<String>();

    public FIXFileReader(Engine engine, SessionSettings settings)  throws ConfigError, FieldConvertError {
        this.engine = engine;
        this.generator = new IdGenerator();
        this.orderDetails = new HashMap<String, OrderContainer>();
        this.orderMap = new HashMap<String, String>();
        processSettings(settings);
    }

    @Override
    public void orderAccepted(Order order, String text) {
    }

    @Override
    public void orderRejected(Order order, String text) {
        removeOrder(order.getOrderId());
    }

    @Override
    public void amendAccepted(Order order, String text) {
    }

    @Override
    public void amendRejected(Order order, String text) {
    }

    @Override
    public void cancelAccepted(Order order, String text) {
        removeOrder(order.getOrderId());
    }

    @Override
    public void cancelRejected(Order order, String text) {
    }

    @Override
    public void orderMatched(Order order, String text) {
        if (order.isFilled())
            removeOrder(order.getOrderId());
    }

    @Override
    public void orderLocked(String tradeId, Order order, float price, long quantity) {}


    @Override
    public void update(BookSubscriber bookSubscriber) {
        if ( this.replayWaitSymbol == null || this.running ) return;
        if ( bookSubscriber.getSymbol().equals(this.replayWaitSymbol) ) {
            synchronized (lock) {
                LOGGER.info("Received subscription for " + this.replayWaitSymbol);
                lock.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        if ( this.replayWaitSymbol != null ) {
            LOGGER.info("Waiting on subscription for " + this.replayWaitSymbol);
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {e.printStackTrace();}
            }
            start();
        }
        else {
            start();
        }
    }

    public void start() {
        this.running = true;
        long count = this.replayLoopCount;
        while (count>0 | this.replayLoopCount<0)  {
            runLoop();
            if (this.replayLoopCount > 0 ) --count;
        }
    }

    public void runLoop() {
        LOGGER.info("Running reader on file " + file);
        File testFile = new File(this.file);
        String line;
        final MessageStats stats = new MessageStats(FIXFileReader.class);
        long messagesSkipped = 0;
        BufferedReader input;
        SendingTime sendingTime = new SendingTime();
        quickfix.Message message;
        quickfix.DataDictionary dictionary;
        long real_start = System.currentTimeMillis(), record_start = 0L;
        long next_ramp = real_start + 60000L;
        long nanoSleep =  this.replayNanoSleep;

        try {
            dictionary = new DataDictionary(new FileInputStream(new File(this.dataDictionary)));
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        long forwardTo=0L;
        if (this.replayForwardTo!=null) {
            DateFormat dateformat = new SimpleDateFormat("yyyyMMDD-HH:mm:ss");
            try {
                Date date = (Date) dateformat.parse(this.replayForwardTo);
                forwardTo = date.getTime();
                LOGGER.info("Forwarding to " + this.replayForwardTo + "(" + forwardTo + ")");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        Thread t = new Thread() {
            public void run() {
                while (!stats.isComplete())  {
                    try {
                        Thread.sleep(5000);
                        stats.log();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();

        try {
            input =  new BufferedReader(new FileReader(testFile));
            while (( line = input.readLine()) != null){
                try {
                    boolean updateMarketObservers = true;
                    message = new quickfix.Message(line.substring(line.indexOf("8="), line.length()), dictionary, true);
                    if (message.isApp()) {
                        // get the sending time of the message
                        long record_now = message.getHeader().getField(sendingTime).getValue().getTime();

                        // work out how to sleep / update market observers
                        if (this.replayForwardTo != null && record_now < forwardTo) {
                            Thread.yield();
                            updateMarketObservers=false;

                            stats.resetStart();
                            real_start = System.currentTimeMillis();
                            next_ramp = real_start + 60000L;
                            messagesSkipped++;
                            if ( (messagesSkipped % 10000L) == 0 ) {
                                LOGGER.info("Currently at " + message.getHeader().getField(sendingTime).getValue() + "("+record_now+")");
                            }
                        }
                        else if ( replayMode == ReplayMode.REAL || replayMode == ReplayMode.UNKNOWN ) {
                            if (record_start == 0L ) { record_start = record_now; }
                            long real_elapsed = System.currentTimeMillis() - real_start;
                            long record_elapsed = record_now - record_start;

                            if (real_elapsed < record_elapsed) {
                                try {
                                    Thread.sleep(record_elapsed-real_elapsed);
                                }
                                catch(InterruptedException e) {}
                            }
                            stats.process();
                        }
                        else if ( replayMode == ReplayMode.FIXED ) {
                            sleepNanos( nanoSleep );
                            if ( this.replayRampFactor > 0.0 && System.currentTimeMillis() > next_ramp ) {
                                nanoSleep = (long)(nanoSleep*this.replayRampFactor) ;
                                LOGGER.info("Ramping rate, nano sleep is now " + nanoSleep);
                                next_ramp += 30000L;
                            }
                            Thread.yield();
                            stats.process();
                        }
                        else if ( replayMode == ReplayMode.FAST ) {
                            Thread.yield();
                            stats.process();
                        }

                        // process the event
                        MsgType msgType = new MsgType();
                        message.getHeader().getField(msgType);
                        if (msgType.getValue().equals(MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH)) {
                            removeOrders(message.getString(Symbol.FIELD));
                            processSnapshot(message, updateMarketObservers);
                        }
                        else if ( msgType.getValue().equals(MsgType.MARKET_DATA_INCREMENTAL_REFRESH) ) {
                            processIncrementalRefresh(message, updateMarketObservers);
                        }
                    }
                    else if (message.isAdmin()) {
                        MsgType msgType = new MsgType();
                        message.getHeader().getField(msgType);
                        if (msgType.getValue().equals(MsgType.LOGOUT)) {
                            removeAllOrders();
                        }
                    }
                    message.clear();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }
            }
            LOGGER.info("Completed reading of input file ...");
            removeAllOrders();
            input.close();
            stats.complete();
            t.join();
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }

    public static void sleepNanos (long nanoDuration) throws InterruptedException {
        final long end = System.nanoTime() + nanoDuration;
        long timeLeft;
        do {
            timeLeft = end - System.nanoTime();
        } while (timeLeft > 0);
    }

    public void processSnapshot(quickfix.Message message, boolean update) {
        boolean gotSymbol=false;
        String mdEntryId, symbol="";
        char mdEntryType;
        double mdEntryPx, mdEntrySize;
        try {
            if (message.isSetField(Symbol.FIELD)) {
                symbol = message.getString(Symbol.FIELD);
                gotSymbol = true;
            }
            for (quickfix.Group group: message.getGroups(NoMDEntries.FIELD)) {
                mdEntryId = group.getString(MDEntryID.FIELD);
                mdEntryType = group.getChar(MDEntryType.FIELD);
                mdEntryPx = group.getDouble(MDEntryPx.FIELD);
                mdEntrySize = group.getDouble(MDEntrySize.FIELD);
                if (!gotSymbol && group.isSetField(Symbol.FIELD))
                    symbol = message.getString(Symbol.FIELD);
                addOrder(symbol, mdEntryId, mdEntryType, mdEntryPx, mdEntrySize);
            }
            if ( update ) engine.updateMarketObservers(symbol);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void processIncrementalRefresh(quickfix.Message message, boolean update) {
        boolean gotSymbol=false;
        String mdEntryId, mdEntryRefID, symbol="";
        char mdEntryType, mdUpdateAction;
        double mdEntryPx, mdEntrySize;
        try {
            if (message.isSetField(Symbol.FIELD)) {
                symbol = message.getString(Symbol.FIELD);
                gotSymbol = true;
            }
            for (quickfix.Group group: message.getGroups(NoMDEntries.FIELD)) {
                mdUpdateAction = group.getChar(MDUpdateAction.FIELD);
                switch (mdUpdateAction) {
                    case MDUpdateAction.NEW:
                        mdEntryId = group.getString(MDEntryID.FIELD);
                        mdEntryType = group.getChar(MDEntryType.FIELD);
                        mdEntryPx = group.getDouble(MDEntryPx.FIELD);
                        mdEntrySize = group.getDouble(MDEntrySize.FIELD);
                        if (!gotSymbol && group.isSetField(Symbol.FIELD))
                            symbol = group.getString(Symbol.FIELD);
                        addOrder(symbol, mdEntryId, mdEntryType, mdEntryPx, mdEntrySize);
                        break;
                    case MDUpdateAction.CHANGE:
                        mdEntryId = group.getString(MDEntryID.FIELD);
                        mdEntryPx = group.getDouble(MDEntryPx.FIELD);
                        mdEntrySize = group.getDouble(MDEntrySize.FIELD);
                        if (!gotSymbol && group.isSetField(Symbol.FIELD))
                            symbol = group.getString(Symbol.FIELD);
                        if ( group.isSetField(MDEntryRefID.FIELD) ) {
                            mdEntryRefID = group.getString(MDEntryRefID.FIELD);
                            amendOrder(symbol, mdEntryId, mdEntryRefID, mdEntryPx, mdEntrySize);
                        }
                        else {
                            amendOrder(symbol, mdEntryId, null, mdEntryPx, mdEntrySize);
                        }
                        break;
                    case MDUpdateAction.DELETE:
                        mdEntryId = group.getString(MDEntryID.FIELD);
                        if (!gotSymbol && group.isSetField(Symbol.FIELD))
                            symbol = group.getString(Symbol.FIELD);
                        deleteOrder(symbol, mdEntryId);
                        break;
                }
            }
            if ( update ) engine.updateMarketObservers(symbol);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addOrder(String symbol, String id, char mdEntryType, double mdEntryPx, double mdEntrySize) {
        String clOrderId = symbol+":"+id;
        if (orderDetails.containsKey(clOrderId)) {
            amendOrder(symbol, id, null, mdEntryPx, mdEntrySize);
        }
        else {
            String orderId = generator.genOrderID();
            String side =  (mdEntryType == MDEntryType.BID) ? Constants.BUY : Constants.SELL;
            Order order = new Order(orderId, clOrderId, this.account, this.traderID,  symbol,
                    side, Constants.LIMIT, mdEntryPx, (long) mdEntrySize, this.counterParties);

            orderMap.put(orderId, clOrderId);
            orderDetails.put(clOrderId, new OrderContainer(orderId, symbol, side));
            try {
                engine.insertOrder(this, order, false);
            } catch (Exception e) {
                orderRejected(order, e.getMessage());
            }
        }
    }

    public void amendOrder(String symbol, String id, String refId, double mdEntryPx, double mdEntrySize) {
        String clOrderId = symbol+":"+id;
        if ( refId != null ) {
            clOrderId = symbol+":"+refId;
        }
        if (orderDetails.containsKey(clOrderId)) {
            OrderContainer container = orderDetails.get(clOrderId);
            if ( engine.amendOrder(this, symbol, container.side, container.orderId, mdEntryPx, (long) mdEntrySize, false) ) {
                if ( refId != null ) {
                    removeOrder(container.orderId);
                    orderMap.put(container.orderId, symbol+":"+id);
                    orderDetails.put(symbol+":"+id, new OrderContainer(container.orderId, container.symbol, container.side));
                }
            }
            else {
                LOGGER.warn("Unable to amend order for " + clOrderId);
            }
        }
        else {
            LOGGER.warn("Unable to find order for " + clOrderId + " to amend");
        }
    }

    public void deleteOrder(String symbol, String id) {
        String clOrderId = symbol+":"+id;
        if (orderDetails.containsKey(clOrderId)) {
            OrderContainer container = orderDetails.get(clOrderId);
            if ( !engine.deleteOrder(this, symbol, container.side, container.orderId, false) ){
                LOGGER.warn("Unable to delete order for " + clOrderId);
            }
        }
        else {
            LOGGER.warn("Unable to find order for " + clOrderId + " to delete");
        }
    }

    public void removeAllOrders() {
        LOGGER.info("Removing all orders");
        List<String> clOrderIDs = new ArrayList<String>();
        for (String key : orderMap.values())
            clOrderIDs.add(key);

        if ( clOrderIDs.size() > 0 ) {
            for (String clOrderId : clOrderIDs) {
                OrderContainer container = orderDetails.get(clOrderId);
                if ( !engine.deleteOrder(this, container.symbol, container.side, container.orderId, false) ){
                    LOGGER.warn("Unable to delete order for " + clOrderId);
                }
            }
        }
    }

    public void removeOrders(String symbol) {
        LOGGER.info("Removing all orders for "+symbol);
        List<String> clOrderIDs = new ArrayList<String>();
        for (String key : orderMap.values())
            clOrderIDs.add(key);

        if ( clOrderIDs.size() > 0 ) {
            for (String clOrderId : clOrderIDs) {
                OrderContainer container = orderDetails.get(clOrderId);
                if (symbol.equals(container.symbol) ) {
                    if ( !engine.deleteOrder(this, container.symbol, container.side, container.orderId, false) ){
                        LOGGER.warn("Unable to delete order for " + clOrderId);
                    }
                }
            }
        }
    }

    public void removeOrder(String orderId) {
        if (orderMap.containsKey(orderId)) {
            String clOrderId =  orderMap.get(orderId);
            orderMap.remove(orderId);
            if ( orderDetails.containsKey(clOrderId) )
                orderDetails.remove(clOrderId);
        }
    }

    private void processSettings(SessionSettings settings) throws ConfigError, FieldConvertError {
        this.file = settings.getString(REPLAY_FILE);
        this.dataDictionary = settings.getString(REPLAY_DATA_DICTIONARY);

        if (settings.isSetting(REPLAY_ID)) {
            try { this.targetCompID = settings.getString(REPLAY_ID); }
            catch(Exception e) {
                LOGGER.warn("Error processing " + REPLAY_ID + ", using default " + this.targetCompID);
            }
        }

        if (settings.isSetting(REPLAY_ACCOUNT)) {
            try { this.account = settings.getString(REPLAY_ACCOUNT); }
            catch(Exception e) {
                LOGGER.warn("Error processing " + REPLAY_ACCOUNT + ", using default " + this.account);
            }
        }

        if (settings.isSetting(REPLAY_TRADER)) {
            try { this.traderID = settings.getString(REPLAY_TRADER); }
            catch(Exception e) {
                LOGGER.warn("Error processing " + REPLAY_TRADER + ", using default " + this.traderID);
            }
        }

        if (settings.isSetting(REPLAY_MODE)) {
            try {
                this.replayMode = ReplayMode.forName(settings.getString(REPLAY_MODE));
            }
            catch(Exception e) {
                LOGGER.warn("Error processing " + REPLAY_MODE + ", using default " + this.replayMode);
            }
        }

        if (settings.isSetting(REPLAY_NANO_SLEEP)) {
            try {
                this.replayNanoSleep = settings.getLong(REPLAY_NANO_SLEEP);
            }
            catch(Exception e) {}
        }

        if (settings.isSetting(REPLAY_RAMP_FACTOR)) {
            try {
                this.replayRampFactor = settings.getLong(REPLAY_RAMP_FACTOR);
            }
            catch(Exception e) {}
        }

        if (settings.isSetting(REPLAY_LOOP_COUNT)) {
            try {
                this.replayLoopCount = settings.getLong(REPLAY_LOOP_COUNT);
            }
            catch(Exception e) {}
        }

        if (settings.isSetting(REPLAY_WAIT_SYMBOL)) {
            try {
                this.replayWaitSymbol = settings.getString(REPLAY_WAIT_SYMBOL);
            }
            catch(Exception e) {}
        }

        if (settings.isSetting(REPLAY_FORWARD_TO)) {
            try {
                this.replayForwardTo = settings.getString(REPLAY_FORWARD_TO);
            }
            catch(Exception e) {}
        }

        try {
            StringTokenizer tokens = new StringTokenizer(settings.getString("ReplayCounterParties"),",");
            while(tokens.hasMoreTokens()) {
                this.counterParties.add(tokens.nextToken());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
