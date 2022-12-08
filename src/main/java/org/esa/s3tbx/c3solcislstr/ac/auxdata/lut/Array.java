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

import static org.esa.s3tbx.c3solcislstr.ac.auxdata.lut.ArrayUtilities.degrade;

/**
 * Interface for wrapping primitive arrays.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface Array {

    /**
     * Creates a new instance of this class from a data object.
     *
     * @param data The data object (either of type double[] or float[])
     * @return the array created.
     */
    static Array create(Object data) {
        if (data instanceof double[]) {
            return new Double((double[]) data);
        }
        if (data instanceof float[]) {
            return new Float((float[]) data);
        }
        throw new IllegalArgumentException("Data object must be an array of type 'double[]' or 'float[]''.");
    }

    /**
     * Returns the length of the primitive array wrapped.
     *
     * @return the lenght of the primitive array wrapped.
     */
    int getLength();

    /**
     * Returns the ith value of the primitive array wrapped.
     *
     * @param i the array index.
     * @return the ith value of the primitive array wrapped.
     */
    double getValue(int i);

    /**
     * Gets a slice of values from the primitive array wrapped, starting at a given index.
     *
     * @param start The start index.
     * @param slice  The slice of values (on return).
     */
    void getValues(int start, double[] slice);

    /**
     * Sets the ith value of the primitive array wrapped to a new value.
     *
     * @param i the array index.
     * @param d the new value.
     */
    void setValue(int i, double d);

    Array append(Array array);

    Array degraded(int dimensionIndex, int coordinateIndex, int[] cardinals);

    Array reordered(int[] reordering, int[] cardinals);

        /**
         * Class for wrapping {@code double} primitive arrays.
         */
    final class Double implements Array {
        private final double[] values;

        public Double(double[] values) {
            if (values == null) {
                throw new NullPointerException("values == null");
            }
            this.values = values;
        }

        @Override
        public final int getLength() {
            return values.length;
        }

        @Override
        public final double getValue(int i) {
            return values[i];
        }

        @Override
        public final void getValues(int start, double[] slice) {
            System.arraycopy(this.values, start, slice, 0, slice.length);
        }

        @Override
        public void setValue(int i, double d) {
            values[i] = d;
        }

        public Array degraded(int dimensionIndex, int coordinateIndex, int[] cardinals) {
            return degrade(dimensionIndex, coordinateIndex, cardinals, this, newArray(getLength() / cardinals[dimensionIndex]));
        }

        @Override
        public Array reordered(int[] reordering, int[] cardinals) {
            return ArrayUtilities.reorder(reordering, cardinals, this, newArray(getLength()));
        }

        @Override
        public Array append(Array that) {
            final int thisLength = this.getLength();
            final int thatLength = that.getLength();
            final Array mergedArray = newArray(thisLength + thatLength);
            IntStream.range(0, thisLength).forEach(i -> mergedArray.setValue(i, getValue(i)));
            IntStream.range(0, thatLength).forEach(i -> mergedArray.setValue(i + thisLength, that.getValue(i)));

            return mergedArray;
        }

        private Array newArray(int n) {
            return new Double(new double[n]);
        }
    }

    /**
     * Class for wrapping {@code float} primitive arrays.
     */
    final class Float implements Array {
        private final float[] values;

        public Float(float[] values) {
            if (values == null) {
                throw new IllegalArgumentException("values == null");
            }
            this.values = values;
        }

        @Override
        public final int getLength() {
            return values.length;
        }

        @Override
        public final double getValue(int i) {
            return values[i];
        }

        @Override
        public final void getValues(int start, double[] slice) {
            for (int i = 0; i < slice.length; ++i) {
                slice[i] = values[start + i];
            }
        }

        @Override
        public void setValue(int i, double d) {
            values[i] = (float) d;
        }

        @Override
        public Array degraded(int dimensionIndex, int coordinateIndex, int[] cardinals) {
            return degrade(dimensionIndex, coordinateIndex, cardinals, this, newArray(getLength() / cardinals[dimensionIndex]));
        }

        @Override
        public Array reordered(int[] reordering, int[] cardinals) {
            return ArrayUtilities.reorder(reordering, cardinals, this, newArray(getLength()));
        }

        @Override
        public Array append(Array that) {
            final int thisLength = this.getLength();
            final int thatLength = that.getLength();
            final Array mergedArray = newArray(thisLength + thatLength);
            IntStream.range(0, thisLength).forEach(i -> mergedArray.setValue(i, getValue(i)));
            IntStream.range(0, thatLength).forEach(i -> mergedArray.setValue(i + thisLength, that.getValue(i)));

            return mergedArray;
        }

        private Array newArray(int n) {
            return new Float(new float[n]);
        }

    }
}
