/*
 * Copyright (C) 2021 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, see http://www.gnu.org/licenses/.
 */

package org.esa.s3tbx.c3solcislstr.mc.generators;

import org.esa.s3tbx.c3solcislstr.mc.RandomNumberGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MelgTest {

    private RandomNumberGenerator rng;

    @Before
    public void setUp() {
        rng = new Melg(new long[]{0x12345L, 0x23456L, 0x34567L, 0x45678L});
    }

    @Test
    public void testNextLong() {
        Assert.assertEquals(Long.parseUnsignedLong("16675511042081433281"), rng.nextLong());
        Assert.assertEquals(Long.parseUnsignedLong("8489326016911908102"), rng.nextLong());
        Assert.assertEquals(Long.parseUnsignedLong("16071362722047509693"), rng.nextLong());
        Assert.assertEquals(Long.parseUnsignedLong("11631833934008589069"), rng.nextLong());
        Assert.assertEquals(Long.parseUnsignedLong("3308423691540511443"), rng.nextLong());
        Assert.assertEquals(Long.parseUnsignedLong("12463994900921303743"), rng.nextLong());

        for (int i = 6; i < 999; i++) {
            rng.nextLong();
        }

        Assert.assertEquals(Long.parseUnsignedLong("13711744326396256691"), rng.nextLong());
    }
}
