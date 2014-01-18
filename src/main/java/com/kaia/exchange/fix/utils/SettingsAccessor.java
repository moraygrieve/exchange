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

