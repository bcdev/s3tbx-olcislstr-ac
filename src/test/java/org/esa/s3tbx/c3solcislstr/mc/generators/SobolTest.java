/*
 * Copyright (C) 2021 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/.
 */

package org.esa.s3tbx.c3solcislstr.mc.generators;

import org.esa.s3tbx.c3solcislstr.mc.Multivariate;
import org.esa.s3tbx.c3solcislstr.mc.NormalVariate;
import org.esa.s3tbx.c3solcislstr.mc.UniformVariate;
import org.esa.s3tbx.c3solcislstr.mc.variates.BoxMullerNormalVariate;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.commons.math3.stat.StatUtils.mean;
import static org.apache.commons.math3.stat.StatUtils.variance;

public class SobolTest {

    @Test
    public void testNextDouble_dimensionality_1_start_0() {
        final UniformVariate u = new Sobol().start(0).get(0);

        Assert.assertEquals(0.0, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.5, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.75, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.25, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.375, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.625, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.125, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.1875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.6875, u.nextDouble(), 1.0E-10);
    }

    @Test
    public void testNextDouble_dimensionality_1_start_1() {
        final UniformVariate u = new Sobol().get(0);

        Assert.assertEquals(0.5, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.75, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.25, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.375, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.625, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.125, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.1875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.6875, u.nextDouble(), 1.0E-10);
    }

    @Test
    public void testNextDouble_dimensionality_1_with_overflow() {
        final UniformVariate u = new Sobol().start(4503599627370494L).get(0);

        u.nextDouble();
        u.nextDouble();

        Assert.assertEquals(0.0, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.5, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.75, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.25, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.375, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.625, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.125, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.1875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.6875, u.nextDouble(), 1.0E-10);
    }

    @Test
    public void testNextDouble_dimensionality_2_start_0() {
        final Sobol sobol = new Sobol(2).start(0);
        final UniformVariate u = sobol.get(0);
        final UniformVariate v = sobol.get(1);;

        Assert.assertEquals(0.0, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.0, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.5, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.5, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.75, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.25, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.25, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.75, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.375, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.375, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.875, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.625, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.125, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.125, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.625, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.1875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.3125, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.6875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.8125, v.nextDouble(), 1.0E-10);
    }

    @Test
    public void testNextDouble_dimensionality_2_start_2() {
        final Sobol sobol = new Sobol(2);
        final UniformVariate u = sobol.get(0);
        final UniformVariate v = sobol.get(1);;

        Assert.assertEquals(0.75, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.25, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.25, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.75, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.375, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.375, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.875, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.625, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.125, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.125, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.625, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.1875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.3125, v.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.6875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.8125, v.nextDouble(), 1.0E-10);
    }

    @Test
    public void testNextDouble_dimensionality_3_start_0() {
        final Sobol sobol = new Sobol(3).start(0);
        final UniformVariate u = sobol.get(0);
        final UniformVariate v = sobol.get(1);;
        final UniformVariate w = sobol.get(2);;

        Assert.assertEquals(0.0, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.0, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.0, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.5, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.5, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.5, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.75, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.25, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.25, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.25, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.75, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.75, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.375, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.375, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.625, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.875, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.125, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.625, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.125, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.875, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.125, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.625, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.375, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.1875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.3125, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.9375, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.6875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.8125, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.4375, w.nextDouble(), 1.0E-10);
    }

    @Test
    public void testNextDouble_dimensionality_3_start_3() {
        final Sobol sobol = new Sobol(3);
        final UniformVariate u = sobol.get(0);
        final UniformVariate v = sobol.get(1);;
        final UniformVariate w = sobol.get(2);;

        Assert.assertEquals(0.25, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.75, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.75, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.375, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.375, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.625, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.875, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.125, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.625, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.125, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.875, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.125, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.625, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.375, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.1875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.3125, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.9375, w.nextDouble(), 1.0E-10);

        Assert.assertEquals(0.6875, u.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.8125, v.nextDouble(), 1.0E-10);
        Assert.assertEquals(0.4375, w.nextDouble(), 1.0E-10);
    }

    @Test
    public void testNextDoubles_uniformity() {
        final int n = 20;
        final double[] values = new double[n];
        final UniformVariate uniform = new Sobol().get(0);

        uniform.nextDoubles(values);

        Assert.assertEquals(0.5, mean(values), 2.0 / Math.sqrt(n));
        Assert.assertEquals(1.0 / 12.0, variance(values), 4.0 / Math.sqrt(n));
    }

    @Test
    public void testNextDoubles_normality() {
        final int n = 20;
        final double[] values = new double[n];
        final Multivariate multivariate = new Sobol(2);
        final UniformVariate u = multivariate.get(0);
        final UniformVariate v = multivariate.get(1);
        final NormalVariate normal = new BoxMullerNormalVariate(u, v);

        normal.nextDoubles(values);

        Assert.assertEquals(0.0, mean(values), 2.0 / Math.sqrt(n));
        Assert.assertEquals(1.0, variance(values), 4.0 / Math.sqrt(n));
    }

}
