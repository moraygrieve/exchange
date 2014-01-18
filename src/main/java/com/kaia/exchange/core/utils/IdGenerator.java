package com.kaia.exchange.core.utils;

public class IdGenerator {
    private static int orderIdCounter = 0;
    private static int tradeIdCounter = 0;
    private static int updateIdCounter = 0;
    private static long startTime = System.currentTimeMillis();

    public String genOrderID() {
        return "ORD:"+startTime+":"+Integer.toString(orderIdCounter++);
    }

    public String genTradeID() {
        return "TRD:"+startTime+":"+Integer.toString(tradeIdCounter++);
    }

    public String genUpdateID() {
        return "UPD:"+startTime+":"+Integer.toString(updateIdCounter++);
    }
}
