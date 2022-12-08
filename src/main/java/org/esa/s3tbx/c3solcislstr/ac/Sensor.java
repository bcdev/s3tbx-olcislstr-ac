/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.c3solcislstr.ac;

import static org.esa.s3tbx.c3solcislstr.ac.OlciSlstrAcConstants.*;

/**
 * Encapsulates the differences between the different sensors
 */
public enum Sensor {

    // todo: define all numbers as constants

    OLCI_SLSTR_S3A("OLCI_SLSTR_S3A", 27, 15,0.02, 0.04, 0.05, 0.005,
            OLCI_SLSTR_CALIBRATION_COEFFS,
            OLCI_SLSTR_WAVELENGHTS_S3A,
            1.0,
            OLCI_SLSTR_TOA_BAND_NAMES,
            OLCI_SLSTR_TOA_BAND_NAMES_TO_CORRECTD,
            OLCI_SLSTR_TOA_BAND_NAMES_TO_CORRECTED_BINAER,
            OLCI_SLSTR_TOA_BAND_NAMES_MERIS_HERITTAGE,
            OLCI_SLSTR_TOA_BAND_NAMES_OLCI,
            OLCI_SLSTR_TOA_BAND_NAMES_SLSTR_NADIR,
            OLCI_SLSTR_ANCILLARY_BAND_NAMES,
            OLCI_SLSTR_SDR_BAND_NAMES,
            OLCI_SLSTR_SDR_ERROR_BAND_NAMES,
            OLCI_SLSTR_GEOM_BAND_NAMES_OLCI,
            OLCI_SLSTR_GEOM_BAND_NAMES_SLSTR_NADIR,
            OLCI_SLSTR_OZO_TP_NAME,
            OLCI_SLSTR_SURF_PRESS_TP_NAME,
            OLCI_SLSTR_WATERVAPOR_TP_NAME,
            OLCI_SLSTR_NDVI_BAND_NAME,
            OLCI_SLSTR_NDVI_EXPR,
            OLCI_SLSTR_NIR_NAME,
            OLCI_SLSTR_VALID_EXPR,
            OLCI_SLSTR_AOT_OUT_EXPR,
            OLCI_SLSTR_SPEC_WEIGHTS,
            LAND_EXPR_OLCI_SLSTR,
            INPUT_INCONSISTENCY_ERROR_MESSAGE),

    OLCI_SLSTR_S3B("OLCI_SLSTR_S3B", 27, 15,0.02, 0.04, 0.05, 0.005,
            OLCI_SLSTR_CALIBRATION_COEFFS,
            OLCI_SLSTR_WAVELENGHTS_S3B,
            1.0,
            OLCI_SLSTR_TOA_BAND_NAMES,
            OLCI_SLSTR_TOA_BAND_NAMES_TO_CORRECTD,
            OLCI_SLSTR_TOA_BAND_NAMES_TO_CORRECTED_BINAER,
            OLCI_SLSTR_TOA_BAND_NAMES_MERIS_HERITTAGE,
            OLCI_SLSTR_TOA_BAND_NAMES_OLCI,
            OLCI_SLSTR_TOA_BAND_NAMES_SLSTR_NADIR,
            OLCI_SLSTR_ANCILLARY_BAND_NAMES,
            OLCI_SLSTR_SDR_BAND_NAMES,
            OLCI_SLSTR_SDR_ERROR_BAND_NAMES,
            OLCI_SLSTR_GEOM_BAND_NAMES_OLCI,
            OLCI_SLSTR_GEOM_BAND_NAMES_SLSTR_NADIR,
            OLCI_SLSTR_OZO_TP_NAME,
            OLCI_SLSTR_SURF_PRESS_TP_NAME,
            OLCI_SLSTR_WATERVAPOR_TP_NAME,
            OLCI_SLSTR_NDVI_BAND_NAME,
            OLCI_SLSTR_NDVI_EXPR,
            OLCI_SLSTR_NIR_NAME,
            OLCI_SLSTR_VALID_EXPR,
            OLCI_SLSTR_AOT_OUT_EXPR,
            OLCI_SLSTR_SPEC_WEIGHTS,
            LAND_EXPR_OLCI_SLSTR,
            INPUT_INCONSISTENCY_ERROR_MESSAGE),

    OLCI_SLSTR_NOMINAL("OLCI_SLSTR_NOMINAL", 27, 15,0.02, 0.04, 0.05, 0.005,
            OLCI_SLSTR_CALIBRATION_COEFFS,
            OLCI_SLSTR_WAVELENGHTS_NOMINAL,
            1.0,
            OLCI_SLSTR_TOA_BAND_NAMES,
            OLCI_SLSTR_TOA_BAND_NAMES_TO_CORRECTD,
            OLCI_SLSTR_TOA_BAND_NAMES_TO_CORRECTED_BINAER,
            OLCI_SLSTR_TOA_BAND_NAMES_MERIS_HERITTAGE,
            OLCI_SLSTR_TOA_BAND_NAMES_OLCI,
            OLCI_SLSTR_TOA_BAND_NAMES_SLSTR_NADIR,
            OLCI_SLSTR_ANCILLARY_BAND_NAMES,
            OLCI_SLSTR_SDR_BAND_NAMES,
            OLCI_SLSTR_SDR_ERROR_BAND_NAMES,
            OLCI_SLSTR_GEOM_BAND_NAMES_OLCI,
            OLCI_SLSTR_GEOM_BAND_NAMES_SLSTR_NADIR,
            OLCI_SLSTR_OZO_TP_NAME,
            OLCI_SLSTR_SURF_PRESS_TP_NAME,
            OLCI_SLSTR_WATERVAPOR_TP_NAME,
            OLCI_SLSTR_NDVI_BAND_NAME,
            OLCI_SLSTR_NDVI_EXPR,
            OLCI_SLSTR_NIR_NAME,
            OLCI_SLSTR_VALID_EXPR,
            OLCI_SLSTR_AOT_OUT_EXPR,
            OLCI_SLSTR_SPEC_WEIGHTS,
            LAND_EXPR_OLCI_SLSTR,
            INPUT_INCONSISTENCY_ERROR_MESSAGE);

    private final String name;
    private final int numBands;
    private final int numBandsAotCorr;
    private final double radiometricError;
    private final double cwvError;
    private final double ozoError;
    private final double rtmError;
    private final float[] calCoeff;
    private final float[] wavelength;
    private final double errCoregScale;
    private final String[] toaBandNames;
    private final String[] toaBandNamesToCorrected;
    private final int[] toaBandNamesToCorrectedBinaer;
    private final String[] toaBandNamesMerisHeritage;
    private final String[] toaBandNamesOlci;
    private final String[] toaBandNamesSlstrNadir;
    private final String[] ancillaryBandNames;
    private final String[] sdrBandNames;
    private final String[] sdrErrorBandNames;
    private final String[] geomBandNamesOlci;
    private final String[] geomBandNamesSlstrNadir;
    private final String ozoneBandNames;
    private final String surfPressBandName;
    private final String wvBandName;
    private final String ndviBandName;
    private final String ndviExpr;
    private final String nirName;
    private final String validExpr;
    private final String aotOutExpr;
    private final double[] specWeights;
    private final String landExpr;

    private final String inputErrorMessage;


    Sensor(String name, int numBands, int numBandsAotCorr, double radiometricError,
           double cwvError, double ozoError, double rtmError, float[] calCoeff, float[] wavelength, double errCoregScale,
           String[] toaBandNames, String[] toaBandNamesToCorrected, int[] toaBandNamesToCorrectedBinaer,
           String [] toaBandNamesMerisHeritage,String [] toaBandNamesOlci, String[] toaBandNamesSlstrNadir,
           String[] ancillaryBandNames, String[] sdrBandBandNames, String[] sdrErrorBandNames,
           String[] geomBandNamesOlci, String[] geomBandNamesSlstrNadir,
           String ozoneBandNames, String surfPressBandName, String wvBandName, String ndviBandName, String ndviExpr,
           String nirName, String validExpr, String aotOutExpr, double[] specWeights,
           String landExpr, String inputErrorMessage) {

        this.name = name;
        this.numBands = numBands;
        this.numBandsAotCorr = numBandsAotCorr;
        this.radiometricError = radiometricError;
        this.cwvError = cwvError;
        this.ozoError = ozoError;
        this.rtmError = rtmError;
        this.calCoeff = calCoeff;
        this.wavelength = wavelength;
        this.errCoregScale = errCoregScale;
        this.toaBandNames = toaBandNames;
        this.toaBandNamesToCorrected = toaBandNamesToCorrected;
        this.toaBandNamesToCorrectedBinaer= toaBandNamesToCorrectedBinaer;
        this.toaBandNamesMerisHeritage = toaBandNamesMerisHeritage;
        this.toaBandNamesOlci = toaBandNamesOlci;
        this.toaBandNamesSlstrNadir = toaBandNamesSlstrNadir;
        this.ancillaryBandNames = ancillaryBandNames;
        this.sdrBandNames = sdrBandBandNames;
        this.sdrErrorBandNames = sdrErrorBandNames;
        this.geomBandNamesOlci = geomBandNamesOlci;
        this.geomBandNamesSlstrNadir = geomBandNamesSlstrNadir;
        this.ozoneBandNames = ozoneBandNames;
        this.ndviBandName = ndviBandName;
        this.ndviExpr = ndviExpr;
        this.nirName = nirName;
        this.validExpr = validExpr;
        this.aotOutExpr = aotOutExpr;
        this.surfPressBandName = surfPressBandName;
        this.wvBandName = wvBandName;
        this.specWeights = specWeights;
        this.landExpr = landExpr;
        this.inputErrorMessage = inputErrorMessage;
    }

    public String getName() {
        return name;
    }

    public int getNumBands() {
        return numBands;
    }

    public int getNumBandsAotCorr() {return numBandsAotCorr; }

    /**
     * a priori radiometric error (%)
     */
    public double getRadiometricError() {
        return radiometricError;
    }

    public double getCwvError() {
        return cwvError;
    }

    public double getOzoError() {
        return ozoError;
    }

    public double getRtmError() {
        return rtmError;
    }

    public float[] getWavelength() {
        return wavelength;
    }

    public float[] getCalCoeff() {
        return calCoeff;
    }

    public double getErrCoregScale() {
        return errCoregScale;
    }

    public String[] getToaBandNamesOlci() {
        return toaBandNamesOlci;
    }

    public String[] getToaBandNamesSlstrNadir() {
        return toaBandNamesSlstrNadir;
    }

    public String[] getToaBandNames() {
        return toaBandNames;
    }

    public String[] getToaBandNamesToCorrected() {
        return toaBandNamesToCorrected;
    }
    public int[] gettoaBandNamesToCorrectedBinaer() {
        return toaBandNamesToCorrectedBinaer;
    }

    public String[] getToaBandNamesMerisHerritage() {return toaBandNamesMerisHeritage; }
    public String[] getAncillaryBandNames() {
        return ancillaryBandNames;
    }

    public String[] getSdrBandNames() {
        return sdrBandNames;
    }

    public String[] getSdrErrorBandNames() {
        return sdrErrorBandNames;
    }

    public String getLandExpr() {
        return landExpr;
    }

    public String getINPUT_INCONSISTENCY_ERROR_MESSAGE() {
        return INPUT_INCONSISTENCY_ERROR_MESSAGE;
    }

    public String[] getGeomBandNamesOlci() {
        return geomBandNamesOlci;
    }

    public String[] getGeomBandNamesSlstrNadir() {
        return geomBandNamesSlstrNadir;
    }


    public String getOzoneBandNames() {
        return ozoneBandNames;
    }

    public String getSurfPressBandName() {
        return surfPressBandName;
    }

    public String getWvBandName() {
        return wvBandName;
    }

    public String getNdviBandName() {
        return ndviBandName;
    }

    public String getNdviExpr() {
        return ndviExpr;
    }

    public String getNirName() {
        return nirName;
    }

    public String getValidExpr() {
        return validExpr;
    }

    public String getAotOutExpr() {
        return aotOutExpr;
    }

    public double[] getSpecWeights() {
        return specWeights;
    }
}
