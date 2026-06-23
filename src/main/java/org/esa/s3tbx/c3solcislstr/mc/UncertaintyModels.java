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

public enum UncertaintyModels implements UncertaintyModel {

    /**
     * An uncertainty model for CAMS total aerosol optical depth (AOD).
     * <p>
     * Yields an uncertainty of 0.08.
     *
     * See <https://aerocom.met.no/cgi-bin/surfobs_annualrs.pl?PROJECT=CAMS>
     */
    CAMS_AOD_2020(new ConstantUncertaintyModel(0.08)),

    /**
     * An uncertainty model for measured radiance (or an equivalent measurand). The
     * uncertainty model is applicable to linear calibration models where the
     * uncertainty associated with sensor telemetry data is dominated by Poisson
     * noise (or shot noise).
     */
    POISSON(new UncertaintyModel() {
        /**
         * Computes the uncertainty associated with a measurement.
         *
         * @param measurement The measurement.
         * @param c           The coefficients of the uncertainty model, which are
         *                    c[0] = uncertainty due to any effect not quantified otherwise
         *                    c[1] = ratio of noise variance to signal
         *                    c[2] = uncertainty associated with the calibration coefficient
         * @return the uncertainty associated with the measurement.
         */
        @Override
        public double getUncertainty(double measurement, double... c) {
            return Math.sqrt(square(c[0]) + measurement * (c[1] + measurement * square(c[2])));
        }

        @Override
        public int getCoefficientCount() {
            return 3;
        }

        private double square(double x) {
            return x * x;
        }
    }),

    /**
     * An uncertainty model for derived quantities, the relative uncertainty of which
     * is 1 percent (which is the relative radiometric accuracy specified for OLCI).
     */
    RELATIVE_01(new RelativeUncertaintyModel(0.01)),

    /**
     * An uncertainty model for derived quantities, the relative uncertainty of which
     * is 10 percent.
     */
    RELATIVE_10(new RelativeUncertaintyModel(0.10)),

    /**
     * An uncertainty model for derived quantities, the relative uncertainty of which
     * is 15 percent.
     */
    RELATIVE_15(new RelativeUncertaintyModel(0.15)),

    /**
     * An uncertainty model for derived quantities, the relative uncertainty of which
     * is 20 percent.
     */
    RELATIVE_20(new RelativeUncertaintyModel(0.20));

    private final UncertaintyModel uncertaintyModel;

    UncertaintyModels(UncertaintyModel uncertaintyModel) {
        this.uncertaintyModel = uncertaintyModel;
    }

    @Override
    public double getUncertainty(double measurement, double... c) {
        return uncertaintyModel.getUncertainty(measurement, c);
    }

    @Override
    public int getCoefficientCount() {
        return uncertaintyModel.getCoefficientCount();
    }
}
