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
 * Plain matrix interface for use in matrix-free computations.
 *
 * @author Ralf Quast
 */
interface Matrix {
    /**
     * Returns the number of rows.
     * @return the number of rows.
     */
    int rowSize();

    /**
     * Returns the number of columns.
     * @return the number of columns.
     */
    int colSize();

    /**
     * Computes the product of this matrix with a vector supplied as argument.
     *
     * @param v The vector.
     *
     * @return the result vector.
     */
    public Vector multiply(Vector v);
}
