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

public class PcgTest {

    private RandomNumberGenerator rng;

    @Before
    public void setUp() {
        rng = new Pcg(42L, 54L);
    }

    @Test
    public void testNextLong() {
        Assert.assertEquals(0xa15c02b7L, rng.nextLong());
        Assert.assertEquals(0x7b47f409L, rng.nextLong());
        Assert.assertEquals(0xba1d3330L, rng.nextLong());
        Assert.assertEquals(0x83d2f293L, rng.nextLong());
        Assert.assertEquals(0xbfa4784bL, rng.nextLong());
        Assert.assertEquals(0xcbed606eL, rng.nextLong());
    }
}
