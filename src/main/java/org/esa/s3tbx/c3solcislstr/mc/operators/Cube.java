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

/**
 * A cube of numbers (which facilitates a vector view).
 *
 * @author Ralf Quast
 */
public interface Cube extends Vector {

    /**
     * Returns the depth of the cube (i.e., its extend along the z-axis, e.g., spectral).
     *
     * @return the depth of the cube.
     */
    int depth();

    /**
     * Returns the height of the cube  (i.e., its extend along the y-axis, e.g., along track or latitudinal).
     *
     * @return the height of the cube.
     */
    int height();

    /**
     * Returns the widths of the cube  (i.e., its extend along the x-axis, e.g., across track or longitudinal).
     *
     * @return the width of the cube.
     */
    int width();

    /**
     * Returns the value at a certain point.
     *
     * @param x The x coordinate of the point.
     * @param y The y coordinate of the point.
     * @param z The z coordinate of the point.
     *
     * @return the value at the given point.
     */
    double get(int x, int y, int z);

    /**
     * Returns the spectrum at a certain point.
     *
     * @param x The x coordinate of the point.
     * @param y The y coordinate of the point.
     *
     * @return the spectrum at the given point.
     */
    double[] spectrum(int x, int y);
}
