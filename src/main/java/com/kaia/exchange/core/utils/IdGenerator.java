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
