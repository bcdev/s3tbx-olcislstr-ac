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

import java.text.MessageFormat;

public class VectorLookupTable implements MultivariateLookupTable {

    /**
     * The lookup values.
     */
    private final Array values;
    /**
     * The dimensions associated with the lookup table.
     */
    private final IntervalPartition[] dimensions;
    /**
     * The strides defining the layout of the lookup value array.
     */
    private final int[] strides;
    /**
     * The relative array offsets of the lookup values for the vertices of a coordinate grid cell.
     */
    private final int[] o;
    /**
     * the length of the lookup vector.
     */
    private final int vectorLength;

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the lookup vector.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public VectorLookupTable(int length, final double[] values, final IntervalPartition... dimensions) {
        this(length, new Array.Double(values), dimensions);
    }

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the lookup vector.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public VectorLookupTable(int length, final float[] values, final IntervalPartition... dimensions) {
        this(length, new Array.Float(values), dimensions);
    }

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the lookup vector.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public VectorLookupTable(int length, final double[] values, final double[]... dimensions) {
        this(length, values, IntervalPartition.createArray(dimensions));
    }

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the lookup vector.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public VectorLookupTable(int length, final float[] values, final float[]... dimensions) {
        this(length, values, IntervalPartition.createArray(dimensions));
    }

    public VectorLookupTable(int length, final Array values, final IntervalPartition... dimensions) {
        if (length < 1) {
            throw new IllegalArgumentException("length < 1");
        }
        vectorLength = length;

        ensureLegalArray(dimensions);
        ensureLegalArray(values, vectorLength * getVertexCount(dimensions));

        this.values = values;
        this.dimensions = dimensions;

        final int n = dimensions.length;

        strides = new int[n];
        // Compute strides
        for (int i = n, stride = vectorLength; i-- > 0; stride *= dimensions[i].getCardinal()) {
            strides[i] = stride;
        }

        o = new int[1 << n];
        computeVertexOffsets(strides, o);
    }

    /**
     * Returns the number of dimensions associated with the lookup table.
     *
     * @return the number of dimensions.
     */
    @Override
    public final int getDimensionCount() {
        return dimensions.length;
    }

    /**
     * Returns the dimensions associated with the lookup table.
     *
     * @return the dimensions.
     */
    @Override
    public final IntervalPartition[] getDimensions() {
        return dimensions;
    }

    /**
     * Returns the the ith dimension associated with the lookup table.
     *
     * @param i the index number of the dimension of interest
     * @return the ith dimension.
     */
    @Override
    public final IntervalPartition getDimension(final int i) {
        return dimensions[i];
    }

    /**
     * Returns an interpolated value array for the given coordinates.
     *
     * @param coordinates the coordinates of the lookup point.
     * @return the interpolated value array.
     * @throws IllegalArgumentException if the length of the {@code coordinates} array is
     *                                  not equal to the number of dimensions associated
     *                                  with the lookup table.
     * @throws NullPointerException     if the {@code coordinates} array is {@code null}.
     */
    @Override
    public final double[] getValues(final double... coordinates) throws IllegalArgumentException {
        ensureLegalArray(coordinates, dimensions.length);
        final FracIndex[] fracIndices = FracIndex.createArray(dimensions.length);
        for (int i = 0; i < dimensions.length; ++i) {
            computeFracIndex(dimensions[i], coordinates[i], fracIndices[i]);
        }

        return getValues(fracIndices);
    }

    private double[] getValues(final FracIndex... fracIndexes) {
        int origin = 0;
        for (int i = 0; i < dimensions.length; ++i) {
            origin += fracIndexes[i].i * strides[i];
        }
        final double[][] slices = new double[1 << dimensions.length][vectorLength];
        for (int i = 0; i < slices.length; ++i) {
            values.getValues(origin + o[i], slices[i]);
        }
        for (int i = dimensions.length; i-- > 0;) {
            final int m = 1 << i;
            final double f = fracIndexes[i].f;

            for (int j = 0; j < m; ++j) {
                for (int k = 0; k < vectorLength; ++k) {
                    slices[j][k] += f * (slices[m + j][k] - slices[j][k]);
                }
            }
        }

        return slices[0];
    }


    /**
     * Computes the {@link FracIndex} of a coordinate value with respect to a given
     * interval partition. The integral component of the returned {@link FracIndex}
     * corresponds to the index of the maximum partition member which is less than
     * or equal to the coordinate value. The [0, 1) fractional component describes
     * the position of the coordinate value within its bracketing subinterval.
     * <p>
     * Exception: If the given coordinate value is equal to the partition maximum,
     * the fractional component of the returned {@link FracIndex} is equal to 1.0,
     * and the integral component is set to the index of the next to last partition
     * member.
     *
     * @param partition  the interval partition.
     * @param coordinate the coordinate value. If the coordinate value is less (greater)
     *                   than the minimum (maximum) of the given interval partition,
     *                   the returned {@link FracIndex} is the same as if the coordinate.
     *                   value was equal to the partition minimum (maximum).
     * @param fracIndex  the {@link FracIndex}.
     */
    public static void computeFracIndex(final IntervalPartition partition, final double coordinate,
                                        final FracIndex fracIndex) {
        int lo = 0;
        int hi = partition.getCardinal() - 1;

        if (partition.getMonotonicity() > 0) {  // partition is strictly increasing
            while (hi > lo + 1) {
                final int m = (lo + hi) >> 1;

                if (coordinate < partition.get(m)) {
                    hi = m;
                } else {
                    lo = m;
                }
            }
        } else {  // partition is strictly decreasing
            while (hi > lo + 1) {
                final int m = (lo + hi) >> 1;

                if (coordinate > partition.get(m)) {
                    hi = m;
                } else {
                    lo = m;
                }
            }
        }

        fracIndex.i = lo;
        fracIndex.f = (coordinate - partition.get(lo)) / (partition.get(hi) - partition.get(lo));
        fracIndex.truncate();
    }

    /**
     * Computes the relative array offsets of the lookup values for the vertices
     * of a coordinate grid cell.
     *
     * @param strides the strides defining the layout of the lookup value array.
     * @param offsets the offsets.
     */
    static void computeVertexOffsets(final int[] strides, final int[] offsets) {
        for (int i = 0; i < strides.length; ++i) {
            final int k = 1 << i;

            for (int j = 0; j < k; ++j) {
                offsets[k + j] = offsets[j] + strides[i];
            }
        }
    }

    /**
     * Returns the number of vertices in the coordinate grid defined by the given dimensions.
     *
     * @param dimensions the dimensions defining the coordinate grid.
     *
     * @return the number of vertices.
     */
    static int getVertexCount(final IntervalPartition[] dimensions) {
        int count = 1;

        for (final IntervalPartition dimension : dimensions) {
            count *= dimension.getCardinal();
        }

        return count;
    }

    static <T> void ensureLegalArray(final T[] array) throws IllegalArgumentException {
        if (array == null) {
            throw new IllegalArgumentException("array == null");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("array.length == 0");
        }
        for (final T element : array) {
            if (element == null) {
                throw new IllegalArgumentException("element == null");
            }
        }
    }

    static void ensureLegalArray(final double[] array, final int length) throws
            IllegalArgumentException,
            NullPointerException {
        if (array == null) {
            throw new IllegalArgumentException("array == null");
        }
        if (array.length != length) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "array.length = {0} does not correspond to the expected length {1}", array.length, length));
        }
        for (final double d : array) {
            if (Double.isNaN(d)) {
                throw new IllegalArgumentException("element is NaN");
            }
        }
    }

    static void ensureLegalArray(Array array, final int length) throws
            IllegalArgumentException,
            NullPointerException {
        if (array == null) {
            throw new IllegalArgumentException("array == null");
        }
        if (array.getLength() != length) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "array.length = {0} does not correspond to the expected length {1}", array.getLength(), length));
        }
        for (int i = 0; i < array.getLength(); i++) {
            if (Double.isNaN(array.getValue(i))) {
                throw new IllegalArgumentException("element is NaN");
            }
        }
    }
}
