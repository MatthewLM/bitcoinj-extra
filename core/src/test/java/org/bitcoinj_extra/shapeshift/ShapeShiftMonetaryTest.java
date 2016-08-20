/*
 * Copyright (C) 2016 Matthew Mitchell
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bitcoinj_extra.shapeshift;

import org.bitcoinj_extra.core.Coin;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Matthew Mitchell
 */
public class ShapeShiftMonetaryTest {

    @Test
    public void createWithCoinRate() {

        // Example will overflow with 64bit integers

        ShapeShiftMonetary result = new ShapeShiftMonetary(
                Coin.valueOf(123456789012345L),
                Coin.valueOf(98765432),
                8
        );

        assertEquals(124999998999999L, result.getValue());
        assertEquals("1249999.98999999", result.toPlainString());

    }

    @Test
    public void createWithShapeshiftRate() {

        // Example will overflow with 64bit integers

        ShapeShiftMonetary result = new ShapeShiftMonetary(
                Coin.valueOf(123456789),
                new ShapeShiftMonetary(987654321054321L, 6)
        );

        assertEquals(1219326311193415L, result.getValue());
        assertEquals("1219326311.193415", result.toPlainString());

    }

}