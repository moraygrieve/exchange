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

package com.kaia.quantos.fix.utils;

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
