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

package com.kaia.exchange.core.market;

import java.util.List;

public abstract class BookObserver implements Comparable {
    private String account;
    private List<String> counterParties;

    public BookObserver(String account, List<String> counterParties) {
        this.account = account;
        this.counterParties = counterParties;
    }

    public abstract void update(Book book);

    public String getAccount() {
        return this.account;
    }

    public List<String> getCounterParties() {
        return this.counterParties;
    }
}

