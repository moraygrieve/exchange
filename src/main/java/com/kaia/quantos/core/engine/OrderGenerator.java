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

package com.kaia.quantos.core.engine;

public interface OrderGenerator {
    public void orderAccepted(Order order, String text);

    public void orderRejected(Order order, String text);

    public void amendAccepted(Order order, String text);

    public void amendRejected(Order order, String text);

    public void cancelAccepted(Order order, String text);

    public void cancelRejected(Order order, String text);

    public void orderMatched(Order order, String text);

    public void orderLocked(String tradeId, Order order, float price, long quantity);
}
