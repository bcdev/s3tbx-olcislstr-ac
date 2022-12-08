package org.esa.s3tbx.c3solcislstr.ac.auxdata.lut;
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


import java.util.Arrays;

public class Remapped implements MultivariateLookupTable {

    private final MultivariateLookupTable t;
    private final int[] remapping;

    public Remapped(MultivariateLookupTable t, int[] remapping) {
        this.t = t;
        this.remapping = remapping;
    }

    @Override
    public int getDimensionCount() {
        return t.getDimensionCount();
    }

    @Override
    public IntervalPartition[] getDimensions() {
        return t.getDimensions();
    }

    @Override
    public IntervalPartition getDimension(int i) {
        return t.getDimension(i);
    }

    @Override
    public double[] getValues(final double... coordinates) throws IllegalArgumentException {
        final double[] values = t.getValues(coordinates);
        return Arrays.stream(remapping).mapToDouble(j -> values[j]).toArray();
    }
}