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

package org.esa.s3tbx.c3solcislstr.ac.auxdata.lut;

import java.util.stream.IntStream;

public class ArrayUtilities {

    public static Array degrade(int dimensionIndex, int coordinateIndex, int[] cardinals, Array source, Array result) {
        if (source.getLength() % result.getLength() != 0) {
            throw new IllegalArgumentException("Length of the source must be a multiple of the length of the result.");
        }
        final int[] strides = ArrayUtilities.computeStrides(cardinals, new int[cardinals.length]);
        final int sliceLength = strides[dimensionIndex];
        final int sliceStride = strides[dimensionIndex - 1];

        if (source.getLength() % sliceStride != 0) {
            throw new IllegalArgumentException("Cardinal numbers are not compatible with the length of the source.");
        }
        if (result.getLength() % sliceLength != 0) {
            throw new IllegalArgumentException("Cardinal numbers are not compatible with the length of the result.");
        }
        if (source.getLength() / sliceStride != result.getLength() / sliceLength) {
            throw new IllegalArgumentException("Illegal arguments.");
        }

        for (int s = coordinateIndex * sliceLength, r = 0; s < source.getLength(); s += sliceStride, r += sliceLength) {
            for (int i = 0; i < sliceLength; i++) {
                result.setValue(r + i, source.getValue(s + i));
            }
        }

        return result;
    }

    public static Array reorder(int[] reordering, int[] cardinals, Array source, Array result) {
        final int n = reordering.length;
        final int[] sourceStrides = computeStrides(cardinals, new int[n]);
        final int[] resultStrides = computeStrides(reorder(reordering, cardinals, new int[n]), new int[n]);

        reorder(reordering, sourceStrides, source, resultStrides, result);

        return result;
    }

    static void reorder(int[] reordering, int[] sourceStrides, Array source, int[] resultStrides, Array result) {
        final int n = reordering.length;
        final int[] mi = new int[n];
        final int[] mj = new int[n];

        final int elementCount = source.getLength();
        for (int i = 0; i < elementCount; i++) {
            iToMi(sourceStrides, i, mi);
            reorder(reordering, mi, mj);
            result.setValue(miToI(resultStrides, mj), source.getValue(i));
        }
    }

    static int[] reorder(int[] reordering, int[] source, int[] target) {
        IntStream.range(0, reordering.length).forEach(i -> target[i] = source[reordering[i]]);
        return target;
    }

    static int[] computeStrides(int[] cardinals, int[] strides) {
        for (int i = cardinals.length, stride = 1; i-- > 0; stride *= cardinals[i]) {
            strides[i] = stride;
        }
        return strides;
    }

    private static int miToI(int[] strides, int[] mi) {
        return IntStream.range(0, strides.length).map(i -> mi[i] * strides[i]).sum();
    }

    private static void iToMi(int[] strides, int i, int[] mi) {
        for (int j = 0; j < strides.length; ++j) {
            mi[j] = i / strides[j];
            i = i - mi[j] * strides[j];
        }
    }

}
