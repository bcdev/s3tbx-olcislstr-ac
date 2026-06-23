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
 * An uncertainty model for derived quantities. Yields a constant
 * uncertainty.
 */
public class ConstantUncertaintyModel implements UncertaintyModel {
    private final double uncertainty;

    /**
     * Creates a new uncertainty model.
     *
     * @param uncertainty The uncertainty.
     */
    public ConstantUncertaintyModel(double uncertainty) {
        this.uncertainty = uncertainty;
    }

    @Override
    public double getUncertainty(double x, double... c) {
        return uncertainty;
    }

    @Override
    public int getCoefficientCount() {
        return 0;
    }
}
