package org.esa.s3tbx.c3solcislstr.ac;

import org.esa.s3tbx.c3solcislstr.ac.aot.lut.HyLutOlci;
import org.esa.s3tbx.c3solcislstr.ac.aot.lut.HyLutSlstr;
import org.esa.s3tbx.c3solcislstr.ac.aot.lut.Lut;
import org.esa.s3tbx.c3solcislstr.ac.auxdata.SdrAuxdata;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.core.util.ProductUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

@OperatorMetadata(alias = "Sdr.OlciSlstr", version = "0.8",
        authors = "O.Danne, M.Peters",
        internal = true,
        copyright = "Copyright (C) 2022 by Brockmann Consult",
        description = "C3SLot5 Operator for OLCI SLSTR SDR retrieval")

public class SdrOlciSlstrOp extends PixelOperator {

    @SourceProduct
    private Product sourceProduct;

    @SourceProduct
    private Product aotProduct;

    @SourceProduct(alias = "reflectance", optional = true)
    private Product reflectanceProduct = null;

    @Parameter(description = "Sensor")
    protected Sensor sensor;

    @Parameter(defaultValue = "false",
            description = "If set, SDR uncertainty bands will be written into SDR product")
    boolean writeSdrUncertaintyBands;

    @Parameter(defaultValue = "false",
            description = " If set, SDR are computed everywhere (brute force, ignores clouds etc.)")
    boolean computeSdrEverywhere;

    //todo add description
    @Parameter
    protected String landExpression;

    @Parameter(label = "Path to AC LUT", description = "The look-up-table used for the atmospheric correction of OLCI bands.")
    private File pathToLutOlci;
    @Parameter(label = "Path to AC LUT", description = "The look-up-table used for the atmospheric correction of SLSTR bands.")
    private File pathToLutSlstr;

    static final int SRC_LAND_MASK = 0;
    static final int SRC_SNOW_MASK = 1;

    static final int SRC_VZA_OLCI = 2;
    static final int SRC_VAA_OLCI = 3;
    static final int SRC_SZA_OLCI = 4;
    static final int SRC_SAA_OLCI = 5;

    static final int SRC_VZA_SLSTR = 6;

    static final int SRC_VAA_SLSTR = 7;

    static final int SRC_SZA_SLSTR = 8;

    static final int SRC_SAA_SLSTR = 9;

    static final int SRC_DEM_OLCI = 10;

    static final int SRC_DEM_SLSTR = 11;
    static final int SRC_AOT = 12;
    static final int SRC_AOT_ERR = 13;
    static final int SRC_OZONE = 14;

    static final int SRC_SURFACE_PRESSURE = 15;

    static final int SRC_Water_VAPOUR = 16;
    static final int SRC_TOA_RFL = 17;
    int SRC_TOA_VAR;

    SdrAuxdata aux;

    private static final String HY_LUT_LOCATION_OLCI_S3A = "/luts_backup/lut/SENTINEL3_1_OLCI_lut_glob_c3s_v2.nc";

    private static final String HY_LUT_LOCATION_OLCI_S3B = "/luts_backup/lut/SENTINEL3_2_OLCI_lut_glob_c3s_v2.nc";

    private static final String HY_LUT_LOCATION_SLSTR_S3A = "/luts_backup/lut/SENTINEL3_1_SLSTR_lut_glob_c3s_v2.nc";

    private static final String HY_LUT_LOCATION_SLSTR_S3B = "/luts_backup/lut/SENTINEL3_2_SLSTR_lut_glob_c3s_v2.nc";

    private Lut hyLutOlci;
    private double[] hyLutOlciMinMax;

    private Lut hyLutSlstr;
    private double[] hyLutSlstrMinMax;


    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        String lutPathOlci = getLutOlciPath();
        String lutPathSlstr = getLutSlstrPath();
        hyLutOlciMinMax = new double[14];
        hyLutSlstrMinMax = new double[14];
        try {
            hyLutOlci = HyLutOlci.read(lutPathOlci, hyLutOlciMinMax);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            hyLutSlstr = HyLutSlstr.read(lutPathSlstr, hyLutSlstrMinMax);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        aux = SdrAuxdata.getInstance(sensor);
    }

    private String getLutOlciPath() {
        if (pathToLutOlci == null) {
            URL lutUrl;
            switch (sensor) {
                case OLCI_SLSTR_S3A:
                    lutUrl = getClass().getResource(HY_LUT_LOCATION_OLCI_S3A);
                    break;
                case OLCI_SLSTR_S3B:
                    lutUrl = getClass().getResource(HY_LUT_LOCATION_OLCI_S3B);
                    break;
                default:
                    throw new OperatorException("Sensor '" + sensor.getName() + "' not supported.");
            }
            if (lutUrl == null) {
                throw new OperatorException("Could not access default OLCI AC LUT file");
            }
            return lutUrl.toExternalForm();
        } else {
            return pathToLutOlci.getAbsolutePath();
        }
    }

    private String getLutSlstrPath() {
        if (pathToLutSlstr == null) {
            URL lutUrl;
            switch (sensor) {
                case OLCI_SLSTR_S3A:
                    lutUrl = getClass().getResource(HY_LUT_LOCATION_SLSTR_S3A);
                    break;
                case OLCI_SLSTR_S3B:
                    lutUrl = getClass().getResource(HY_LUT_LOCATION_SLSTR_S3B);
                    break;
                default:
                    throw new OperatorException("Sensor '" + sensor.getName() + "' not supported.");
            }
            if (lutUrl == null) {
                throw new OperatorException("Could not access default SLSTR AC LUT file");
            }
            return lutUrl.toExternalForm();
        } else {
            return pathToLutSlstr.getAbsolutePath();
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        addSdrBands(targetProduct);
        if (writeSdrUncertaintyBands) {
            addSdrErrorBands(targetProduct);
        }

        // copy flag coding and flag images
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        targetProduct.setAutoGrouping("sdr_error:sdr");
    }

    private void addSdrBands(Product targetProduct) {
        for (int i = 0; i < sensor.getToaBandNamesToCorrected().length; i++) {
            Band srcBand = sourceProduct.getBand(sensor.getToaBandNamesToCorrected()[i]);
            Band band = targetProduct.addBand(sensor.getSdrBandNames()[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
            ProductUtils.copySpectralBandProperties(srcBand, band);
        }
    }

    private void addSdrErrorBands(Product targetProduct) {
        for (int i = 0; i < sensor.getToaBandNamesToCorrected().length; i++) {
            Band srcBand = sourceProduct.getBand(sensor.getToaBandNamesToCorrected()[i]);
            Band band = targetProduct.addBand(sensor.getSdrErrorBandNames()[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
            ProductUtils.copySpectralBandProperties(srcBand, band);
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer configurator) {
        final String commonLandExpr;
        if (landExpression != null && !landExpression.isEmpty()) {
            commonLandExpr = landExpression;
        } else {
            if (computeSdrEverywhere) {
                //commonLandExpr = OlciAcConstants.LAND_EXPR_OLCI_IGNORE_CLOUDS;
                commonLandExpr = OlciSlstrAcConstants.OLCI_SLSTR_ALL_VALID;
            } else {
                commonLandExpr = sensor.getLandExpr();
            }
        }

//        final static String[] OLCI_SLSTR_ANCILLARY_BAND_NAMES = new String[]{
//                OLCI_SLSTR_VZA_TP_NAME, OLCI_SLSTR_VAA_TP_NAME, OLCI_SLSTR_SZA_TP_NAME, OLCI_SLSTR_SAA_TP_NAME,
//                OLCI_SLSTR_SZA_TP_NADIR_NAME, OLCI_SLSTR_SAA_TP_NADIR_NAME, OLCI_SLSTR_VZA_TP_NADIR_NAME, OLCI_SLSTR_VAA_TP_NADIR_NAME,
//                OLCI_SLSTR_ALTITUDE_NADIR_NAME, OLCI_SLSTR_ALTITUDE_NAME,
//                AOT_AUX_BAND_NAME, AOT_AUX_ERR_BAND_NAME,
//                OLCI_SLSTR_OZO_TP_NAME, OLCI_SLSTR_SURF_PRESS_TP_NAME, OLCI_SLSTR_WATERVAPOR_TP_NAME};

        int ancillaryIndex = SRC_VZA_OLCI;
        for (int i = 0; i < sensor.getAncillaryBandNames().length; i++) {
            if (sensor.getAncillaryBandNames()[i].contains("aot")) {
                configurator.defineSample(ancillaryIndex++, sensor.getAncillaryBandNames()[i], aotProduct);
            } else {
                configurator.defineSample(ancillaryIndex++, sensor.getAncillaryBandNames()[i], sourceProduct);
            }
        }

        for (int i = 0; i < sensor.getToaBandNames().length; i++) {
            configurator.defineSample(SRC_TOA_RFL + i, sensor.getToaBandNames()[i], sourceProduct);
        }

        SRC_TOA_VAR = SRC_TOA_RFL + sensor.getToaBandNames().length;

//        ImageVarianceOp imageVarianceOp = new ImageVarianceOp();
//        imageVarianceOp.setParameterDefaultValues();
//        // sourceProduct contains a copy of the required bands, use original to avoid one copy
//        if (reflectanceProduct != null) {
//            imageVarianceOp.setSourceProduct(reflectanceProduct);
//        } else {
//            imageVarianceOp.setSourceProduct(sourceProduct);
//        }
//        Product varianceProduct = imageVarianceOp.getTargetProduct();
//        for (int i = 0; i < sensor.getToaBandNames().length; i++) {
//            configurator.defineSample(SRC_TOA_VAR + i, sensor.getToaBandNames()[i], varianceProduct);
//        }

        final String snowMaskExpression = "pixel_classif_flags.IDEPIX_SNOW_ICE";

        BandMathsOp.BandDescriptor bdSnow = new BandMathsOp.BandDescriptor();
        bdSnow.name = "snow_mask";
        bdSnow.expression = snowMaskExpression;
        bdSnow.type = ProductData.TYPESTRING_INT8;

        BandMathsOp snowOp = new BandMathsOp();
        snowOp.setParameterDefaultValues();
        snowOp.setSourceProduct(sourceProduct);
        snowOp.setTargetBandDescriptors(bdSnow);
        Product snowMaskProduct = snowOp.getTargetProduct();

        configurator.defineSample(SRC_SNOW_MASK, snowMaskProduct.getBandAt(0).getName(), snowMaskProduct);

        BandMathsOp.BandDescriptor bdLand = new BandMathsOp.BandDescriptor();
        bdLand.name = "land_mask";
        bdLand.expression = commonLandExpr;
        bdLand.type = ProductData.TYPESTRING_INT8;

        BandMathsOp landOp = new BandMathsOp();
        landOp.setParameterDefaultValues();
        landOp.setSourceProduct(sourceProduct);
        landOp.setTargetBandDescriptors(bdLand);
        Product landMaskProduct = landOp.getTargetProduct();

        configurator.defineSample(SRC_LAND_MASK, landMaskProduct.getBandAt(0).getName(), landMaskProduct);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer configurator) {
        int index = 0;
        for (int i = 0; i < sensor.getSdrBandNames().length; i++) {
            configurator.defineSample(index++, sensor.getSdrBandNames()[i]);
        }
        if (writeSdrUncertaintyBands) {
            for (int i = 0; i < sensor.getSdrErrorBandNames().length; i++) {
                configurator.defineSample(index++, sensor.getSdrErrorBandNames()[i]);
            }
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final int landFlag = sourceSamples[SRC_LAND_MASK].getInt();
        final int snowFlag = sourceSamples[SRC_SNOW_MASK].getInt();

        if (landFlag == 0 && snowFlag == 0) {
            // not land and not snow
            OlciSlstrAcUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        double vza_olci = sourceSamples[SRC_VZA_OLCI].getDouble();
        double vaa_olci = sourceSamples[SRC_VAA_OLCI].getDouble();
        double sza_olci = sourceSamples[SRC_SZA_OLCI].getDouble();
        double saa_olci = sourceSamples[SRC_SAA_OLCI].getDouble();
        double vza_slstr = sourceSamples[SRC_VZA_SLSTR].getDouble();
        double vaa_slstr = sourceSamples[SRC_VAA_SLSTR].getDouble();
        double sza_slstr = sourceSamples[SRC_SZA_SLSTR].getDouble();
        double saa_slstr = sourceSamples[SRC_SAA_SLSTR].getDouble();
        double hsf = sourceSamples[SRC_DEM_OLCI].getDouble();
        double aot = sourceSamples[SRC_AOT].getDouble();
        double delta_aot = sourceSamples[SRC_AOT_ERR].getDouble();


        double phi_olci = abs(saa_olci - vaa_olci);
        if (phi_olci > 180.0) {
            phi_olci = 360.0 - phi_olci;
        }
        phi_olci = min(phi_olci, 179);
        phi_olci = max(phi_olci, 1);


        double phi_slstr = abs(saa_slstr - vaa_slstr);
        if (phi_slstr > 180.0) {
            phi_slstr = 360.0 - phi_slstr;
        }
        phi_slstr = min(phi_slstr, 179);
        phi_slstr = max(phi_slstr, 1);

        //TODO check LUT vza range [60-0] instead of [0, 60]
        double vzaMinOlci = hyLutOlciMinMax[1];
        double vzaMaxOlci = hyLutOlciMinMax[0];

        double szaMinOlci = hyLutOlciMinMax[2];
        double szaMaxOlci = hyLutOlciMinMax[3];

        double hsfMinOlci = hyLutOlciMinMax[4];
        double hsfMaxOlci = hyLutOlciMinMax[5];

        double aotMinOlci = hyLutOlciMinMax[6];
        double aotMaxOlci = hyLutOlciMinMax[7];

        double ozoMinOlci = hyLutOlciMinMax[8];
        double ozoMaxOlci = hyLutOlciMinMax[9];

        double cwvMinOlci = hyLutOlciMinMax[10];
        double cwvMaxOlci = hyLutOlciMinMax[11];

        double amfMinOlci = hyLutOlciMinMax[12];
        double amfMaxOlci = hyLutOlciMinMax[13];


        double vzaMinSlstr = hyLutSlstrMinMax[1];
        double vzaMaxSlstr = hyLutSlstrMinMax[0];

        double szaMinSlstr = hyLutSlstrMinMax[2];
        double szaMaxSlstr = hyLutSlstrMinMax[3];

        double hsfMinSlstr = hyLutSlstrMinMax[4];
        double hsfMaxSlstr = hyLutSlstrMinMax[5];

        double aotMinSlstr = hyLutSlstrMinMax[6];
        double aotMaxSlstr = hyLutSlstrMinMax[7];


        double ozoMinSlstr = hyLutSlstrMinMax[8];
        double ozoMaxSlstr = hyLutSlstrMinMax[9];

        double cwvMinSlstr = hyLutSlstrMinMax[10];
        double cwvMaxSlstr = hyLutSlstrMinMax[11];

        double amfMinSlstr = hyLutSlstrMinMax[12];
        double amfMaxSlstr = hyLutSlstrMinMax[13];


        hsf *= 0.001; // convert m to km
        if (hsf <= 0.0 && hsf >= -0.45) {
            hsf = hsfMinOlci;
        }

        if (vza_olci < vzaMinOlci || vza_olci > vzaMaxOlci ||
                sza_olci < szaMinOlci || sza_olci > szaMaxOlci ||
                vza_slstr < vzaMinSlstr || vza_slstr > vzaMaxSlstr ||
                sza_slstr < szaMinSlstr || sza_slstr > szaMaxSlstr ||
                aot < aotMinOlci || aot > aotMaxOlci ||
                hsf < hsfMinOlci || hsf > hsfMaxOlci ||
                aot < aotMinSlstr || aot > aotMaxSlstr ||
                hsf < hsfMinSlstr || hsf > hsfMaxSlstr) {
            OlciSlstrAcUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        double ozo;
        double cwv;
        double gas;

//      CWV & OZO - provided as a constant value and the other as pixel-based, depending on the sensor
//      MERIS/OLCI: OZO per-pixel, CWV as constant value
        ozo = 0.001 * sourceSamples[SRC_OZONE].getDouble() * 46698.0;
        //Repaced OlciSlstrAcConstants.CWV_CONSTANT_VALUE;   constant mean value of 1.5
        cwv = sourceSamples[SRC_Water_VAPOUR].getDouble();
        gas = ozo;

        double vza_olci_r = toRadians(vza_olci);
        double sza_olci_r = toRadians(sza_olci);
        double muv_olci = cos(vza_olci_r);
        double mus_olci = cos(sza_olci_r);
        double amf_olci = 1.0 / muv_olci + 1.0 / mus_olci;

        double vza_slstr_r = toRadians(vza_slstr);
        double sza_slstr_r = toRadians(sza_slstr);
        double muv_slstr = cos(vza_slstr_r);
        double mus_slstr = cos(sza_slstr_r);
        double amf_slstr = 1.0 / muv_slstr + 1.0 / mus_slstr;

        if (amf_olci < amfMinOlci || amf_olci > amfMaxOlci ||
                amf_slstr < amfMinSlstr || amf_slstr > amfMaxSlstr ||
                Double.isNaN(amf_olci) ||Double.isNaN(amf_slstr) ||
                Double.isNaN(ozo) ||Double.isNaN(cwv)) {
            OlciSlstrAcUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        double[] toa_rfl = new double[sensor.getNumBands()];
        for (int i = 0; i < toa_rfl.length; i++) {
            double toaRefl = sourceSamples[SRC_TOA_RFL + i].getDouble();
            toaRefl /= sensor.getCalCoeff()[i];
            toa_rfl[i] = toaRefl;
        }

        if (ozo <= ozoMinOlci || ozo <= ozoMinSlstr) {
            ozo = Math.max(ozoMinOlci,ozoMinSlstr);
        }
        if (ozo >= ozoMaxOlci || ozo >= ozoMaxSlstr) {
            ozo = Math.min(ozoMaxOlci,ozoMaxSlstr);
        }

        if (cwv <= cwvMinOlci || cwv <= cwvMinSlstr) {
            cwv = Math.max(cwvMinOlci,cwvMinSlstr);
        }
        if (cwv >= ozoMaxOlci || cwv >= ozoMaxSlstr) {
            cwv = Math.min(cwvMaxOlci,cwvMaxSlstr);
        }


        //TODO
        double[] tg_olci = hyLutOlci.getTG(cwv, ozo, amf_olci);
        double[] tg_slstr = hyLutSlstr.getTG(cwv, ozo, amf_slstr);
        double[][] f_int_all_olci = hyLutOlci.getRT(aot, sza_olci, vza_olci, phi_olci, hsf);
        double[][] f_int_all_slstr = hyLutSlstr.getRT(aot, sza_olci, vza_olci, phi_slstr, hsf);

        int[] keyCorrectedYesNo = sensor.gettoaBandNamesToCorrectedBinaer();

        if (f_int_all_olci == null || f_int_all_slstr == null) {
            OlciSlstrAcUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }


//        final double sec = 1.0 / Math.cos(Math.toRadians(sza));
//        for (int i = 0; i < N_WAV; i++) {
//            matrix[0][i] *= sec * Math.PI;
//            matrix[1][i] *= sec;
//            matrix[3][i] = 1.0 - matrix[3][i];  // down
//            matrix[4][i] = 1.0 - matrix[4][i];  // up

        double[] sab = new double[sensor.getNumBands()];
        double[] rfl_pix = new double[sensor.getNumBands()];
        double[] deltaReflf2deltaAot = new double[sensor.getNumBands()];
        double rpw;
        double[] ttot = new double[sensor.getNumBands()];
        double[] f_int = new double[5];
        double[] tg = new double[sensor.getNumBands()];
        double x_term;
        int counter = 0;
        for (int i = 0; i < sensor.getNumBands(); i++) {
            //TODO check
            if (i < 21) { // OLCI
                for (int j = 0; j < 5; j++) {
                    f_int[j] = f_int_all_olci[j][i];
                }
                tg[i] = tg_olci[i];
                rpw = f_int[0]; // * Math.PI / mus_olci; // Path Radiance
                ttot[i] = f_int[1]; // / mus_olci;    // Total TOA flux (Isc*Tup*Tdw)
            } else { //SLSTR
                for (int k = 0; k < 5; k++) {
                    //TODO after OLCI and SLSTR LUT delivery
//                    f_int[j] = f_int_all_slstr[k][i];
                    f_int[k] = f_int_all_slstr[k][i - 21];
                }
                //TODO after OLCI and SLSTR LUT delivery
//                tg[i] = tg_slstr[i];
                tg[i] = tg_slstr[i - 21];
                rpw = f_int[0]; // * Math.PI / mus_slstr; // Path Radiance
                ttot[i] = f_int[1]; // / mus_slstr;    // Total TOA flux (Isc*Tup*Tdw)
            }

            deltaReflf2deltaAot[i] = f_int[3];
            sab[i] = f_int[2];        // Spherical Albedo

            toa_rfl[i] = toa_rfl[i] / tg[i];

            x_term = (toa_rfl[i] - rpw) / ttot[i];
            rfl_pix[i] = x_term / (1. + sab[i] * x_term); //calculation of SDR

            if (keyCorrectedYesNo[i] == 1) {
                targetSamples[counter].set(rfl_pix[i]);
                counter++;
            }
        }

        // compute and write uncertainties only on demand
        if (writeSdrUncertaintyBands) {
            double[] err_rad = new double[sensor.getNumBands()];
            double[] err_RTM = new double[sensor.getNumBands()];
            double[] err_all = new double[sensor.getNumBands()];
            double[] err_aod = new double[sensor.getNumBands()];

            for (int i = 0; i < sensor.getNumBands(); i++) {
                //TODO check getRadiometricError == relative error
                err_rad[i] = sensor.getRadiometricError() * toa_rfl[i] / ttot[i];
                err_RTM[i] = sensor.getRtmError();
                err_aod[i] = deltaReflf2deltaAot[i] * delta_aot;
                err_all[i] = Math.pow((err_rad[i] * err_rad[i] + err_RTM[i] * err_RTM[i] + err_aod[i] * err_aod[i]), 0.5);
            }


            for (int i = 0; i < sensor.getNumBands(); i++) {
                if (keyCorrectedYesNo[i] == 1) {
                    targetSamples[counter].set(err_all[i]);
                    counter++;
                }
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SdrOlciSlstrOp.class);
        }
    }


}
