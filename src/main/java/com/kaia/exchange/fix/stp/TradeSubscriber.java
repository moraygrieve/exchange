package com.kaia.exchange.fix.stp;

import com.kaia.exchange.core.stp.Trade;
import com.kaia.exchange.core.utils.Observer;
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
