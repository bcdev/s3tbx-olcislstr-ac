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

package org.esa.s3tbx.c3solcislstr.mc.operators;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LinearErrorCodecTest {

    private ErrorCodec codec;

    @Before
    public void setUp() {
        codec = ErrorCodec.LINEAR;
    }

    @Test
    public void testEncode() {
        final double[] original = {0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        final byte[] encoded = {0, 13, 25, 51, 76, 102, 127, 127};

        for (int i = 0; i < original.length; i++) {
            Assert.assertEquals(encoded[i], codec.encode(original[i]));
        }
        for (int i = 1; i < original.length; i++) {
            Assert.assertEquals(-encoded[i], codec.encode(-original[i]));
        }
    }

    @Test
    public void testDecode() {
        final byte[] encoded = {0, 13, 25, 51, 76, 102, 127};
        final double[] decoded = {0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0};
        final double[] accuracyGoal = {0.0, 0.02, 0.02, 0.02, 0.02, 0.02, 0.0};

        for (int i = 0; i < encoded.length; i++) {
            Assert.assertEquals(decoded[i], codec.decode(encoded[i]), accuracyGoal[i]);
        }

        for (int i = 1; i < encoded.length; i++) {
            Assert.assertEquals(-decoded[i], codec.decode((byte) -encoded[i]), accuracyGoal[i]);
        }
    }
}
