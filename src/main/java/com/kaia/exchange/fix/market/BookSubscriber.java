package com.kaia.exchange.fix.market;

import com.kaia.exchange.core.market.Book;
import com.kaia.exchange.core.market.BookObserver;
import com.kaia.exchange.fix.utils.SettingsAccessor;
import quickfix.SessionID;
import quickfix.field.MDReqID;
import quickfix.field.NoMDEntries;
import quickfix.field.SubscriptionRequestType;
import quickfix.fix44.MarketDataIncrementalRefresh;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

public class BookSubscriber extends BookObserver {
    private final BookMessageHandler handler;
    private final SubscriptionRequestType requestType;
    private final SessionID session;
    private final MDReqID mdReqID;
    private final String symbol;
    private boolean addSymbolToMessage;
    private BookImpl current;

    public BookSubscriber(BookMessageHandler handler, SubscriptionRequestType requestType, SessionID session,
                          MDReqID mdReqID, String symbol) {

        super(SettingsAccessor.getAccount(session), SettingsAccessor.getCounterParties(session));
        this.handler = handler;
        this.requestType = requestType;
        this.session = session;
        this.mdReqID = mdReqID;
        this.symbol = symbol;
        this.current = new BookImpl(symbol, SettingsAccessor.getAccount(session));
        this.addSymbolToMessage = SettingsAccessor.addSymbolToMarketData(session);
    }

    @Override
    public void update(Book book) {
        if (book instanceof BookImpl) {
            if (getRequestType().valueEquals(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES)) {
                MarketDataIncrementalRefresh refresh = current.incremental(book);
                refresh.set(mdReqID);
                if (addSymbolToMessage) refresh.setField(new quickfix.field.Symbol(symbol));
                if (refresh.isSetField(NoMDEntries.FIELD)) handler.sendMessage(refresh, session);
            } else if (getRequestType().valueEquals(SubscriptionRequestType.SNAPSHOT)) {
                MarketDataSnapshotFullRefresh snapshot = current.snapshot(book);
                snapshot.set(mdReqID);
                if (addSymbolToMessage) snapshot.setField(new quickfix.field.Symbol(symbol));
                if (snapshot.isSetField(NoMDEntries.FIELD)) handler.sendMessage(snapshot, session);
            }
            current = (BookImpl) book;
        }
    }

    public SubscriptionRequestType getRequestType() {
        return requestType;
    }

    public String getSenderCompID() {
        return session.getSenderCompID();
    }

    public String getTargetCompID() {
        return session.getTargetCompID();
    }

    public MDReqID getMdReqID() {
        return mdReqID;
    }

    public String getSymbol() {
        return symbol;
    }

    public int compareTo(Object o) {
        return 0;
    }
}
