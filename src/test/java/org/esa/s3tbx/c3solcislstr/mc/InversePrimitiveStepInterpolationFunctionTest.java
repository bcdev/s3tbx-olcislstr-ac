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

package org.esa.s3tbx.c3solcislstr.mc;

import org.junit.Assert;
import org.junit.Test;

public class InversePrimitiveStepInterpolationFunctionTest {

    @Test
    public void testInterpolation_2_vertices() {
        final double[] x = {1.0, 2.0};
        final double[] y = {4.0, 6.0};
        final InterpolationFunction f = new InterpolationFunction.InversePrimitiveStep(2, x, y);

        Assert.assertTrue(Double.isNaN(f.getY(5.1)));

        Assert.assertEquals(1.0, f.getY(0.0), 0.0);
        Assert.assertEquals(1.4, f.getY(1.6), 1.0E-10);
        Assert.assertEquals(1.5, f.getY(2.0), 1.0E-10);
        Assert.assertEquals(2.0, f.getY(5.0), 1.0E-10);
    }

    @Test
    public void testInterpolation_3_vertices() {
        final double[] x = {1.0, 2.0, 3.0};
        final double[] y = {4.0, 6.0, 7.0};
        final InterpolationFunction f = new InterpolationFunction.InversePrimitiveStep(3, x, y);

        Assert.assertTrue(Double.isNaN(f.getY(11.6)));

        Assert.assertEquals(1.0, f.getY(0.0), 0.0);
        Assert.assertEquals(1.4, f.getY(1.6), 1.0E-10);
        Assert.assertEquals(1.5, f.getY(2.0), 1.0E-10);
        Assert.assertEquals(2.0, f.getY(5.0), 1.0E-10);
        Assert.assertEquals(2.4, f.getY(7.4), 1.0E-10);
        Assert.assertEquals(2.5, f.getY(8.0), 1.0E-10);
        Assert.assertEquals(3.0, f.getY(11.5), 1.0E-10);
    }

}
