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

package org.esa.s3tbx.c3solcislstr.mc.operators;

import java.util.stream.IntStream;

public final class DefaultCube extends DefaultVector implements Cube {

    private final int l;
    private final int m;
    private final int n;

    public DefaultCube(int l, int m, int n, double[] values) {
        super(values);
        this.l = l;
        this.m = m;
        this.n = n;
    }

    @Override
    public int depth() {
        return n;
    }

    @Override
    public int height() {
        return l;
    }

    @Override
    public int width() {
        return m;
    }

    @Override
    public double get(int x, int y, int z) {
        return get(z + n * (x + m * y));
    }

    @Override
    public double[] spectrum(int x, int y) {
        return IntStream.range(0, n).mapToDouble(i -> get(i + n * (x + m * y))).toArray();
    }
}
