package com.kaia.exchange.fix.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageStats {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageStats.class);
    String clsName;
    long start;
    long incrementStart;
    long messages;
    long incrementMessages;
    boolean complete;

    public MessageStats(Class clazz) {
        int mid=clazz.getName().lastIndexOf ('.') + 1;
        this.clsName = clazz.getName().substring(mid);
        this.start = System.currentTimeMillis();
        this.incrementStart = this.start;
        long messages = 0;
        long incrementMessages = 0;
        this.complete = false;
    }

    public synchronized void resetStart() {
        this.start = System.currentTimeMillis();
        this.incrementStart = this.start;
    }

    public synchronized void process() {
        this.messages++;
        this.incrementMessages++;
    }

    public synchronized void log() {
        try {
            long smoothed = 1000*this.messages / (System.currentTimeMillis() - this.start);
            long current = 1000*this.incrementMessages / (System.currentTimeMillis() - this.incrementStart);
            LOGGER.info("Rates:  " + this.clsName + "|" + smoothed + "|" + current);
            this.incrementStart = System.currentTimeMillis();
            this.incrementMessages = 0;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void complete() {
        this.complete = true;
    }

    public boolean isComplete() {
        return this.complete;
    }

}
