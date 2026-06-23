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

package org.esa.s3tbx.c3solcislstr.mc;

/**
 * Generic quantitative uncertainty model.
 */
public interface UncertaintyModel {

    /**
     * Computes the uncertainty associated with a measurement.
     *
     * @param measurement The measurement.
     * @param c           The coefficients of the uncertainty model (and any additional parameters).
     * @return the uncertainty associated with the measurement.
     */
    double getUncertainty(double measurement, double... c);

    /**
     * Returns the number of uncertainty model coefficients.
     *
     * @return the number of uncertainty model coefficients.
     */
    int getCoefficientCount();

}
