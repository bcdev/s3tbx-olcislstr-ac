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
 * An uncertainty model for derived quantities, the relative uncertainty of which
 * is constant.
 */
public class RelativeUncertaintyModel implements UncertaintyModel {

    private final double relativeUncertainty;

    /**
     * Creates a new relative uncertainty model.
     *
     * @param relativeUncertainty the relative uncertainty.
     */
    public RelativeUncertaintyModel(double relativeUncertainty) {
        this.relativeUncertainty = relativeUncertainty;
    }

    @Override
    public double getUncertainty(double measurement, double... c) {
        return Math.abs(relativeUncertainty * measurement);
    }

    @Override
    public int getCoefficientCount() {
        return 0;
    }
}
