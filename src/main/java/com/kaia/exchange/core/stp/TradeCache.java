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
