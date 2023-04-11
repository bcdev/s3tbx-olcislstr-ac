package org.esa.s3tbx.c3solcislstr.ac;

import org.esa.s3tbx.c3solcislstr.ac.aot.lut.HyLutOlci;
import org.esa.s3tbx.c3solcislstr.ac.aot.lut.HyLutSlstr;
import org.esa.s3tbx.c3solcislstr.ac.aot.lut.Lut;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;

import java.io.File;
import java.io.IOException;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

@OperatorMetadata(alias = "Sdr.OlciSlstr", version = "0.8",
        authors = "G. Kirches, O.Danne, M.Peters",
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

    //    static final int SRC_DEM_SLSTR = 11;
    static final int SRC_AOT = 12;
    static final int SRC_AOT_ERR = 13;
    static final int SRC_OZONE = 14;

//    static final int SRC_SURFACE_PRESSURE = 15;

    static final int SRC_Water_VAPOUR = 16;
    static final int SRC_TOA_RFL = 17;
    int SRC_TOA_VAR;

    private Lut hyLutOlci;
    private double[] hyLutOlciMinMax;

    private Lut hyLutSlstr;
    private double[] hyLutSlstrMinMax;
    private double vzaMinOlci;
    private double vzaMaxOlci;
    private double szaMinOlci;
    private double szaMaxOlci;
    private double hsfMinOlci;
    private double hsfMaxOlci;
    private double aotMinOlci;
    private double aotMaxOlci;
    private double ozoMinOlci;
    private double ozoMaxOlci;
    private double cwvMinOlci;
    private double cwvMaxOlci;
    private double amfMinOlci;
    private double amfMaxOlci;
    private double vzaMinSlstr;
    private double vzaMaxSlstr;
    private double szaMinSlstr;
    private double szaMaxSlstr;
    private double hsfMinSlstr;
    private double hsfMaxSlstr;
    private double aotMinSlstr;
    private double aotMaxSlstr;
    private double ozoMinSlstr;
    private double ozoMaxSlstr;
    private double cwvMinSlstr;
    private double cwvMaxSlstr;
    private double amfMinSlstr;
    private double amfMaxSlstr;

    private double[] geophysicalNoDataValues;


    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        String lutPathOlci = pathToLutOlci.getAbsolutePath();
        String lutPathSlstr = pathToLutSlstr.getAbsolutePath();
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
        //TODO check LUT vza range [60-0] instead of [0, 60]
        initMinMaxInputValues();
        geophysicalNoDataValues = new double[sensor.getNumBands()];
        if (SRC_TOA_RFL + sensor.getNumBands() > getSourceProduct().getNumBands()) {
            throw new IllegalArgumentException(sensor.getNumBands() + " sensor bands starting at " + SRC_TOA_RFL + ", but " + getSourceProduct().getNumBands() + " source product bands");
        }
        for (int bandIndex = 0; bandIndex < sensor.getNumBands(); ++bandIndex) {
            geophysicalNoDataValues[bandIndex] = getSourceProduct().getBandAt(SRC_TOA_RFL + bandIndex).getGeophysicalNoDataValue();
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
        for (int i = 0; i < sensor.getToaBandNamesToBeCorrected().length; i++) {
            Band srcBand = sourceProduct.getBand(sensor.getToaBandNamesToBeCorrected()[i]);
            Band band = targetProduct.addBand(sensor.getSdrBandNames()[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
            ProductUtils.copySpectralBandProperties(srcBand, band);
        }
    }

    private void addSdrErrorBands(Product targetProduct) {
        for (int i = 0; i < sensor.getToaBandNamesToBeCorrected().length; i++) {
            Band srcBand = sourceProduct.getBand(sensor.getToaBandNamesToBeCorrected()[i]);
            Band band = targetProduct.addBand(sensor.getSdrErrorBandNames()[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
            ProductUtils.copySpectralBandProperties(srcBand, band);
        }
    }

    /**
     * Configures a stack of samples with input data bands:
     * Position 0: "land", depends on landExpr parameter, computeSdrEverywhere (valid and not cosmetic), sensor
     * Position 1: IDEPIX_SNOW_ICE
     * Position 2-16: ancillary bands VZA .. WV
     * Position 17-43: Oa01_reflectance .. Sl06_reflectance_an
     * @param configurator
     */
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

    /**
     * Configures a stack of samples
     * Position 0-20: sdr_Oa01 .. sdr_Sl06
     * Position 21-41: sdr_error_Oa01 .. sdr_error_Sl06, optional, if writeSdrUncertaintyBands
     * @param configurator
     */
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
            // not land (=valid and not cosmetic if computeSdrEverywhere) and not snow
            OlciSlstrAcUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        // determine ancillary and geometric data of pixel
        final double vza_olci = sourceSamples[SRC_VZA_OLCI].getDouble();
        final double vaa_olci = sourceSamples[SRC_VAA_OLCI].getDouble();
        final double sza_olci = sourceSamples[SRC_SZA_OLCI].getDouble();
        final double saa_olci = sourceSamples[SRC_SAA_OLCI].getDouble();
        final double vza_slstr = sourceSamples[SRC_VZA_SLSTR].getDouble();
        final double vaa_slstr = sourceSamples[SRC_VAA_SLSTR].getDouble();
        final double sza_slstr = sourceSamples[SRC_SZA_SLSTR].getDouble();
        final double saa_slstr = sourceSamples[SRC_SAA_SLSTR].getDouble();
        final double hsf_meters = sourceSamples[SRC_DEM_OLCI].getDouble();
        final double aot = sourceSamples[SRC_AOT].getDouble();
        final double delta_aot = sourceSamples[SRC_AOT_ERR].getDouble();

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

        double hsf = hsf_meters * 0.001; // convert m to km
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

        double ozo = 0.001 * sourceSamples[SRC_OZONE].getDouble() * 46698.0;
        double cwv = sourceSamples[SRC_Water_VAPOUR].getDouble();

        final double muv_olci = cos(toRadians(vza_olci));
        final double mus_olci = cos(toRadians(sza_olci));
        final double amf_olci = 1.0 / muv_olci + 1.0 / mus_olci;

        final double muv_slstr = cos(toRadians(vza_slstr));
        final double mus_slstr = cos(toRadians(sza_slstr));
        final double amf_slstr = 1.0 / muv_slstr + 1.0 / mus_slstr;

        if (amf_olci < amfMinOlci || amf_olci > amfMaxOlci ||
                amf_slstr < amfMinSlstr || amf_slstr > amfMaxSlstr ||
                Double.isNaN(amf_olci) || Double.isNaN(amf_slstr) ||
                Double.isNaN(ozo) || Double.isNaN(cwv)) {
            OlciSlstrAcUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        if (ozo <= ozoMinOlci || ozo <= ozoMinSlstr) {
            ozo = Math.max(ozoMinOlci, ozoMinSlstr);
        }
        if (ozo >= ozoMaxOlci || ozo >= ozoMaxSlstr) {
            ozo = Math.min(ozoMaxOlci, ozoMaxSlstr);
        }

        if (cwv <= cwvMinOlci || cwv <= cwvMinSlstr) {
            cwv = Math.max(cwvMinOlci, cwvMinSlstr);
        }
        if (cwv >= ozoMaxOlci || cwv >= ozoMaxSlstr) {
            cwv = Math.min(cwvMaxOlci, cwvMaxSlstr);
        }

        // apply LUT to ancillary and geometric data of pixel
        //TODO adapt to common OLCI+SLSTR LUT when available
        final double[] tg_olci = hyLutOlci.getTG(cwv, ozo, amf_olci);
        final double[] tg_slstr = hyLutSlstr.getTG(cwv, ozo, amf_slstr);
        final double[][] f_int_all_olci = hyLutOlci.getRT(aot, sza_olci, vza_olci, phi_olci, hsf);
        final double[][] f_int_all_slstr = hyLutSlstr.getRT(aot, sza_slstr, vza_slstr, phi_slstr, hsf);

        if (f_int_all_olci == null || f_int_all_slstr == null) {
            OlciSlstrAcUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        // reflectance bands correction loop
        int counter = 0;
        final int ERROR_TARGET_BAND_OFFSET = sensor.getSdrBandNames().length;
        for (int i = 0; i < sensor.getNumBands(); ++i) {
            // skip bands not to be corrected
            if (! sensor.isToaBandToBeCorrected(i)) {
                continue;
            }
            // check for no-data value
            double toaRefl = sourceSamples[SRC_TOA_RFL + i].getDouble();
            if (toaRefl == geophysicalNoDataValues[i]) {
                targetSamples[counter].set(Double.NaN);
                if (writeSdrUncertaintyBands) {
                    targetSamples[counter + ERROR_TARGET_BAND_OFFSET].set(Double.NaN);
                }
            } else {
                // apply calibration
                toaRefl = toaRefl / sensor.getCalCoeff()[i];
                // determine LUT parameters for band i
                final double rpw;
                final double ttot;
                final double sab;  // Spherical Albedo
                final double deltaReflf2deltaAot;
                final double tg;
                //TODO check
                if (i < 21) { // OLCI
                    rpw = f_int_all_olci[0][i];
                    ttot = f_int_all_olci[1][i];
                    sab = f_int_all_olci[2][i];  // Spherical Albedo
                    deltaReflf2deltaAot = f_int_all_olci[3][i];
                    tg = tg_olci[i];
                    //rpw = f_int[0]; // * Math.PI / mus_olci; // Path Radiance
                    //ttot[i] = f_int[1]; // / mus_olci;    // Total TOA flux (Isc*Tup*Tdw)
                } else { //SLSTR
                    //TODO after OLCI and SLSTR LUT delivery: remove if above, ...
                    // ... = f_int_all_slstr[k][i];
                    rpw = f_int_all_slstr[0][i - 21];
                    ttot = f_int_all_slstr[1][i - 21];
                    sab = f_int_all_slstr[2][i - 21];  // Spherical Albedo
                    deltaReflf2deltaAot = f_int_all_slstr[3][i - 21];
                    //TODO after OLCI and SLSTR LUT delivery
                    // tg = tg_slstr[i];
                    tg = tg_slstr[i - 21];
                    //rpw = f_int[0]; // * Math.PI / mus_slstr; // Path Radiance
                    //ttot[i] = f_int[1]; // / mus_slstr;    // Total TOA flux (Isc*Tup*Tdw)
                }
                // calculate corrected reflectance
                toaRefl = toaRefl / tg;
                final double x_term = (toaRefl - rpw) / ttot;
                final double rfl_pix = x_term / (1. + sab * x_term);  //calculation of SDR

                targetSamples[counter].set(rfl_pix);

                // calculate uncertainty
                if (writeSdrUncertaintyBands) {
                    final double err_rad = sensor.getRadiometricError() * toaRefl / ttot;
                    final double err_RTM = sensor.getRtmError();
                    final double err_aod = deltaReflf2deltaAot * delta_aot;
                    final double err_all = Math.sqrt(err_rad * err_rad + err_RTM * err_RTM + err_aod * err_aod);

                    targetSamples[counter + ERROR_TARGET_BAND_OFFSET].set(err_all);
                }
            }
            // increase target cursor
            counter++;
        }
    }

    private void initMinMaxInputValues() {
        vzaMinOlci = hyLutOlciMinMax[1];
        vzaMaxOlci = hyLutOlciMinMax[0];

        szaMinOlci = hyLutOlciMinMax[2];
        szaMaxOlci = hyLutOlciMinMax[3];

        hsfMinOlci = hyLutOlciMinMax[4];
        hsfMaxOlci = hyLutOlciMinMax[5];

        aotMinOlci = hyLutOlciMinMax[6];
        aotMaxOlci = hyLutOlciMinMax[7];

        ozoMinOlci = hyLutOlciMinMax[8];
        ozoMaxOlci = hyLutOlciMinMax[9];

        cwvMinOlci = hyLutOlciMinMax[10];
        cwvMaxOlci = hyLutOlciMinMax[11];

        amfMinOlci = hyLutOlciMinMax[12];
        amfMaxOlci = hyLutOlciMinMax[13];

        vzaMinSlstr = hyLutSlstrMinMax[1];
        vzaMaxSlstr = hyLutSlstrMinMax[0];

        szaMinSlstr = hyLutSlstrMinMax[2];
        szaMaxSlstr = hyLutSlstrMinMax[3];

        hsfMinSlstr = hyLutSlstrMinMax[4];
        hsfMaxSlstr = hyLutSlstrMinMax[5];

        aotMinSlstr = hyLutSlstrMinMax[6];
        aotMaxSlstr = hyLutSlstrMinMax[7];

        ozoMinSlstr = hyLutSlstrMinMax[8];
        ozoMaxSlstr = hyLutSlstrMinMax[9];

        cwvMinSlstr = hyLutSlstrMinMax[10];
        cwvMaxSlstr = hyLutSlstrMinMax[11];

        amfMinSlstr = hyLutSlstrMinMax[12];
        amfMaxSlstr = hyLutSlstrMinMax[13];
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SdrOlciSlstrOp.class);
        }
    }


}
