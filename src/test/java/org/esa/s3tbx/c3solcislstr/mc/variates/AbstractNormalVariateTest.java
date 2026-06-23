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

package org.esa.s3tbx.c3solcislstr.mc.variates;

import org.esa.s3tbx.c3solcislstr.mc.RandomVariate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.math3.stat.StatUtils.mean;
import static org.apache.commons.math3.stat.StatUtils.variance;

public abstract class AbstractNormalVariateTest {

    static final int N = 1000000;

    private RandomVariate randomVariate;

    protected abstract RandomVariate createNormalVariate();

    @Before
    public void setUp() {
        randomVariate = createNormalVariate();
    }

    @Test
    public void testNextDoubles_0() {
        double[] values = randomVariate.nextDoubles(new double[0]);

        Assert.assertEquals(0, values.length);
    }

    @Test
    public void testNextDoubles_1() {
        double[] values = randomVariate.nextDoubles(new double[1]);

        Assert.assertEquals(1, values.length);
        Assert.assertNotEquals(0.0, values[0]);
    }

    @Test
    public void testNextDoubles_2() {
        double[] values = randomVariate.nextDoubles(new double[2]);

        Assert.assertEquals(2, values.length);
        Assert.assertNotEquals(0.0, values[0]);
        Assert.assertNotEquals(0.0, values[1]);
    }

    @Test
    public void testNextDoubles_3() {
        double[] values = randomVariate.nextDoubles(new double[3]);

        Assert.assertEquals(3, values.length);
        Assert.assertNotEquals(0.0, values[0]);
        Assert.assertNotEquals(0.0, values[1]);
        Assert.assertNotEquals(0.0, values[2]);
    }

    @Test
    public void testNextDoubles_n() {
        double[] values = randomVariate.nextDoubles(new double[N]);

        Assert.assertEquals(N, values.length);
        Assert.assertEquals(0.0, mean(values), 2.0 / Math.sqrt(N));
        Assert.assertEquals(1.0, variance(values), 4.0 / Math.sqrt(N));
    }
}
