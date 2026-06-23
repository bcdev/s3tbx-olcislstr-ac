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

public class LatinHypercubeTest {

    public static final int M = 18;

    @Test
    public void testNextDouble_dimensionality_1_start_0() {
        final Multivariate multivariate = new LatinHypercube(1, M, 0);
        final UniformVariate u = multivariate.get(0);

        assertEquidistribution(u);
    }

    @Test
    public void testNextDouble_dimensionality_1_start_1() {
        final Multivariate multivariate = new LatinHypercube(1, M, 1);
        final UniformVariate u = multivariate.get(0);

        assertEquidistribution(u);
    }

    @Test
    public void testNextDouble_dimensionality_2_start_7() {
        final Multivariate multivariate = new LatinHypercube(2, M, 7);
        final UniformVariate u = multivariate.get(0);
        final UniformVariate v = new LatinHypercube(2, M, 7).get(1);

        assertEquidistribution(u);
        assertEquidistribution(v);
    }

    @Test
    public void testNextDoubles_uniformity() {
        final int n = 20;
        final double[] values = new double[n];
        final UniformVariate uniform = new LatinHypercube(1, n).get(0);

        uniform.nextDoubles(values);

        Assert.assertEquals(0.5, mean(values), 2.0 / Math.sqrt(n));
        Assert.assertEquals(1.0 / 12.0, variance(values), 4.0 / Math.sqrt(n));
    }

    @Test
    public void testNextDoubles_normality() {
        final int n = 20;
        final double[] values = new double[n];
        final Multivariate multivariate = new LatinHypercube(2, n);
        final UniformVariate u = multivariate.get(0);
        final UniformVariate v = multivariate.get(1);
        final NormalVariate normal = new BoxMullerNormalVariate(u, v);

        normal.nextDoubles(values);

        Assert.assertEquals(0.0, mean(values), 2.0 / Math.sqrt(n));
        Assert.assertEquals(1.0, variance(values), 4.0 / Math.sqrt(n));
    }

    private void assertEquidistribution(UniformVariate u) {
        final double[] values = u.nextDoubles(new double[M]);
        final boolean[] test = new boolean[M];

        for (int i = 0; i < M; i++) {
            final int index = (int) (values[i] * M);
            test[index] = !test[index];
        }
        for (int i = 0; i < M; i++) {
            Assert.assertTrue(test[i]);
        }
    }

}
