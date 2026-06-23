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

package org.esa.s3tbx.c3solcislstr.mc.lut;

import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

public class ArrayUtilitiesTest {

    @Test
    public void testReorder() {
        final int[] sourceCardinals = {5, 4, 3, 2};
        final int[] targetCardinals = {2, 3, 4, 5};
        final int[] resultCardinals = {0, 0, 0, 0};
        final Array sourceArray = new Array.Double(IntStream.range(0, 120).asDoubleStream().toArray());
        final Array targetArray = new Array.Double(new double[120]);
        final Array resultArray = new Array.Double(new double[120]);

        final int[] reordering = {3, 2, 1, 0};  // reversion
        final int[] sourceStrides = ArrayUtilities.computeStrides(sourceCardinals, new int[4]);
        final int[] targetStrides = ArrayUtilities.computeStrides(targetCardinals, new int[4]);

        ArrayUtilities.reorder(reordering, sourceStrides, sourceArray, targetStrides, targetArray);

        ArrayUtilities.reorder(reordering, targetStrides, targetArray, sourceStrides, resultArray);

        for (int i = 0; i < sourceArray.getLength(); i++) {
            Assert.assertEquals(sourceArray.getValue(i), resultArray.getValue(i), 0.0);
        }
    }
}
