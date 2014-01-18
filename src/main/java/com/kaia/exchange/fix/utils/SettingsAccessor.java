package com.kaia.exchange.fix.utils;

import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SettingsAccessor {
    public static final String ADD_SYMBOL_TO_MESSAGE = "MarketDataAddSymbolToMessage";
    public static final String COUNTER_PARTIES = "CounterParties";
    public static final String ACCOUNT = "Account";
    private static SettingsAccessor instance;
    private SessionSettings settings = null;

    private SettingsAccessor(SessionSettings settings) {
        this.settings = settings;
    }

    public static SettingsAccessor createInstance(SessionSettings settings) {
        if (instance == null) {
            instance = new SettingsAccessor(settings);
        }
        return instance;
    }

    public static List<String> getCounterParties(SessionID session) {
        List<String> counterParties = new ArrayList<String>();
        try {
            if ( instance.settings.isSetting(session, COUNTER_PARTIES) ) {
                StringTokenizer tokens = new StringTokenizer(instance.settings.getString(session, COUNTER_PARTIES),",");
                while(tokens.hasMoreTokens()) {
                    counterParties.add(tokens.nextToken());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return counterParties;
    }

    public static String getAccount(SessionID session) {
        String account = "";
        try {
            if ( instance.settings.isSetting(session, ACCOUNT) ) {
                account = instance.settings.getString(session, ACCOUNT);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return account;
    }

    public static boolean addSymbolToMarketData(SessionID session) {
        boolean add = false;
        try {
            add = instance.settings.getBool(session, ADD_SYMBOL_TO_MESSAGE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return add;
    }
}

