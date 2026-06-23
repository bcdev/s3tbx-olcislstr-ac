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

import java.util.Arrays;

/**
 * Correlates a cube of random numbers.
 *
 * @author Ralf Quast
 */
abstract class Correlator implements Cube {

    /**
     * No error correlation.
     */
    static final class None extends Correlator {

        /**
         * Simply delegates to a given cube.
         *
         * @param cube The cube.
         */
        public None(Cube cube) {
            super(cube);
        }

        @Override
        public double[] spectrum(int x, int y) {
            return delegate().spectrum(x, y);
        }

        @Override
        public double get(int i) {
            return delegate().get(i);
        }
    }

    /**
     * Constant error correlation.
     */
    static final class Constant extends Correlator {

        private final double b;
        private final double r;

        /**
         * Correlates the random numbers in a given cube with a given bias.
         *
         * @param cube The cube.
         * @param b The bias.
         * @param r The correlation coefficient.
         */
        public Constant(Cube cube, double b, double r) {
            super(cube);
            this.b = b;
            this.r = r;
        }

        @Override
        public double[] spectrum(int x, int y) {
            final double[] spectrum = delegate().spectrum(x, y);
            Arrays.setAll(spectrum, i -> correlated(spectrum[i]));
            return spectrum;
        }

        @Override
        public double get(int i) {
            return correlated(delegate().get(i));
        }

        private double correlated(double v) {
            return Math.sqrt(1.0 - r) * v + Math.sqrt(r) * b;
        }
    }

    private final Cube cube;

    public Correlator(Cube cube) {
        this.cube = cube;
    }

    @Override
    public int depth() {
        return cube.depth();
    }

    @Override
    public int height() {
        return cube.height();
    }

    @Override
    public int width() {
        return cube.width();
    }

    @Override
    public int size() {
        return cube.size();
    }

    @Override
    public double get(int x, int y, int z) {
        return get(z + depth() * (x + width() * y));
    }

    /**
     * Returns the delegate cube.
     *
     * @return the delegate cube.
     */
    protected final Cube delegate() {
        return cube;
    }
}
