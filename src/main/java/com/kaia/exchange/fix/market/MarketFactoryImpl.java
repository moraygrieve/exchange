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

package com.kaia.exchange.fix.market;

import com.kaia.exchange.core.market.BookFactory;
import com.kaia.exchange.core.market.Market;
import com.kaia.exchange.core.market.MarketFactory;

public class MarketFactoryImpl extends MarketFactory {

    public MarketFactoryImpl(BookFactory bookFactory) {
        super(bookFactory);
    }

    @Override
    public Market createMarket(String symbol) {
        return new MarketImpl(symbol, bookFactory);
    }
}
