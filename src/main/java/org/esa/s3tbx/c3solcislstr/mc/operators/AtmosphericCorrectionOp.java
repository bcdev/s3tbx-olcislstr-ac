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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.c3solcislstr.mc.lut.AtmosphereLookupTable;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.idepix.core.IdepixConstants;

import java.io.IOException;
import java.util.List;

/**
 * Operator to perform the atmospheric correction for OLCI. For use in Monte Carlo simulations.
 *
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "AtmosphereCorrector", version = "0.1",
        authors = "Ralf Quast",
        category = "OLCI",
        copyright = "Copyright (C) 2021 by Brockmann Consult",
        description = "Performs the atmospheric correction for OLCI. For use in Monte Carlo simulations")
public class AtmosphericCorrectionOp extends PixelOperator {

    private static final double[] OLCI_TO_MERIS_CALIBRATION_COEFFICIENTS = {
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0
    };

    static final String[] TOA_RFL_BAND_NAMES = new String[]{
            "Oa02_reflectance", "Oa03_reflectance", "Oa04_reflectance", "Oa05_reflectance", "Oa06_reflectance",
            "Oa07_reflectance", "Oa08_reflectance", "Oa10_reflectance", "Oa11_reflectance", "Oa12_reflectance",
            "Oa13_reflectance", "Oa16_reflectance", "Oa17_reflectance", "Oa18_reflectance", "Oa19_reflectance"
    };

    static final String[] OLC_SDR_BAND_NAMES = new String[]{
            "Oa02_sdr", "Oa03_sdr", "Oa04_sdr", "Oa05_sdr", "Oa06_sdr",
            "Oa07_sdr", "Oa08_sdr", "Oa10_sdr", "Oa11_sdr", "Oa12_sdr",
            "Oa13_sdr", "Oa16_sdr", "Oa17_sdr", "Oa18_sdr", "Oa19_sdr"
    };

    static final String[] MER_SDR_BAND_NAMES = new String[]{
            "sdr_1", "sdr_2", "sdr_3", "sdr_4", "sdr_5",
            "sdr_6", "sdr_7", "sdr_8", "sdr_9", "sdr_10",
            "sdr_11", "sdr_12", "sdr_13", "sdr_14", "sdr_15"
    };

    private static final String[] ANCILLARY_BAND_NAMES = new String[]{
            "OZA", "OAA", "SZA", "SAA", "altitude", "aod550", "gtco3", "tcwv"
    };

    private static final int COMP_MASK_INDEX = 0;
    private static final int SNOW_MASK_INDEX = 1;
    private static final int VZA_INDEX = 2;
    private static final int VAA_INDEX = 3;
    private static final int SZA_INDEX = 4;
    private static final int SAA_INDEX = 5;
    private static final int ALT_INDEX = 6;
    private static final int AOT_INDEX = 7;
    private static final int OZO_INDEX = 8;
    private static final int CWV_INDEX = 9;
    private static final int RFL_INDEX = 10;

    private static final String LAND_AND_CLEAR_OR_SNOW_OR_ICE =
            "pixel_classif_flags.IDEPIX_LAND && !(pixel_classif_flags.IDEPIX_CLOUD || pixel_classif_flags.IDEPIX_CLOUD_BUFFER || pixel_classif_flags.IDEPIX_CLOUD_SHADOW) || pixel_classif_flags.IDEPIX_SNOW_ICE";
    private static final String EVERYWHERE_COMPUTE_MASK_EXPRESSION =
            "!quality_flags.invalid && !quality_flags.cosmetic";
    private static final String DEFAULT_COMPUTE_MASK_EXPRESSION =
            EVERYWHERE_COMPUTE_MASK_EXPRESSION + " && (" + LAND_AND_CLEAR_OR_SNOW_OR_ICE + ")";

    @Parameter(label = "Compute-mask expression",
            defaultValue = DEFAULT_COMPUTE_MASK_EXPRESSION,
            description = "The expression defining where surface directional reflectance is computed.")
    private String computeMaskExpression;

    @Parameter(label = "Compute SDR everywhere",
            defaultValue = "true",
            description = "If checked, surface directional reflectance is computed everywhere (i.e., no matter what the 'compute mask' is).")
    private boolean computeSdrEverywhere;

    @Parameter(defaultValue = "false",
            description = "If checked, the atmospheric correction is performed without considering aerosols.")
    private boolean ignoreAerosols;

    @Parameter(label = "Clone all data",
            description = "If checked, all data are copied from source to target, if not conflicting.",
            defaultValue = "true")
    private boolean cloneAll;

    @SourceProduct
    private Product sourceProduct;
    
    private AtmosphereLookupTable lookupTable;
    private double maxAot;
    private double minSza;
    private double maxSza;
    private double minVza;
    private double maxVza;
    private double minHsf;
    private double maxHsf;
    private List<String> exclusionList;

    @Override
    protected void configureTargetProduct(ProductConfigurer c) {
        c.copyMetadata();
        c.copyTimeCoding();
        c.copyTiePointGrids();
        for (int i = 0; i < TOA_RFL_BAND_NAMES.length; i++) {
            final Band rflBand = c.getSourceProduct().getBand(TOA_RFL_BAND_NAMES[i]);
            final Band sdrBand = c.getTargetProduct().addBand(OLC_SDR_BAND_NAMES[i], ProductData.TYPE_FLOAT32);
            sdrBand.setNoDataValue(Double.NaN);
            sdrBand.setNoDataValueUsed(true);
            ProductUtils.copySpectralBandProperties(rflBand, sdrBand);
        }
        if (cloneAll) {
            c.copyBands(band -> !c.getTargetProduct().containsBand(band.getName()) && !band.getName().contains("ctp") && !band.getName().contains("collocationFlags") && !band.getName().contains("frame_offset") && !band.getName().contains("FWHM") && !band.getName().contains("lambda") && !band.getName().contains("solar_flux"));
        } else {
            c.copyBands(Band::isFlagBand);
        }
        c.copyGeoCoding();
        c.copyMasks();
        c.getTargetProduct().setAutoGrouping(String.format("%s:Oa*_sdr:sdr:aod:*_flags", c.getSourceProduct().getAutoGrouping().toString()));
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer c) {
        final String compMaskExpression;
        if (computeSdrEverywhere) {
            compMaskExpression = EVERYWHERE_COMPUTE_MASK_EXPRESSION;
        } else {
            compMaskExpression = computeMaskExpression;
        }
        final String snowMaskExpression = String.format("%s.IDEPIX_SNOW_ICE", IdepixConstants.CLASSIF_BAND_NAME);
        
        defineMaskSample(c, COMP_MASK_INDEX, compMaskExpression);
        defineMaskSample(c, SNOW_MASK_INDEX, snowMaskExpression);  // actually not needed, but might become necessary
        defineSourceSamples(c, VZA_INDEX, ANCILLARY_BAND_NAMES);
        defineSourceSamples(c, RFL_INDEX, TOA_RFL_BAND_NAMES);
    }

    private static void defineMaskSample(SourceSampleConfigurer c, int index, String expression) {
        c.defineComputedSample(index, ProductData.TYPE_INT8, expression);
    }

    @SuppressWarnings("SameParameterValue")
    private void defineSourceSamples(SourceSampleConfigurer c, int index, String[] sourceBandNames) {
        for (final String name : sourceBandNames) {
            if (ignoreAerosols && index == AOT_INDEX) {
                c.defineComputedSample(index, ProductData.TYPE_FLOAT32, "0.0");
            } else {
                c.defineSample(index, name);
            }
            index++;
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer c) {
        defineTargetSamples(c, 0, OLC_SDR_BAND_NAMES);
    }

    @SuppressWarnings("SameParameterValue")
    private void defineTargetSamples(TargetSampleConfigurer c, int index, String[] targetBandNames) {
        for (String name : targetBandNames) {
            c.defineSample(index++, name);
        }
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("Reading radiative transfer lookup table ...", 1);
            lookupTable = GlobAlbedoLookupTable.read();
            pm.worked(1);
        } catch (IOException e) {
            throw new OperatorException("Radiative transfer lookup tables could not be read.", e);
        } finally {
            pm.done();
        }
        
        maxAot = lookupTable.lutRT().getDimension(0).getMax();
        minHsf = lookupTable.lutRT().getDimension(1).getMin();
        maxHsf = lookupTable.lutRT().getDimension(1).getMax();
        minSza = lookupTable.lutRT().getDimension(3).getMin();
        maxSza = lookupTable.lutRT().getDimension(3).getMax();
        minVza = lookupTable.lutRT().getDimension(4).getMin();
        maxVza = lookupTable.lutRT().getDimension(4).getMax();
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final boolean comp = sourceSamples[COMP_MASK_INDEX].getBoolean();

        if (!comp) {
            setToNoData(targetSamples);
            return;
        }

        final double vza = sourceSamples[VZA_INDEX].getDouble();
        final double vaa = sourceSamples[VAA_INDEX].getDouble();
        final double sza = sourceSamples[SZA_INDEX].getDouble();
        final double saa = sourceSamples[SAA_INDEX].getDouble();
        final double aot = Math.max(0.0, sourceSamples[AOT_INDEX].getDouble());  // could be negative due to bicubic interpolation
        final double hsf = clip(fromMeterToKilometer(sourceSamples[ALT_INDEX].getDouble()));

        if (vza < minVza || vza > maxVza || sza < minSza || sza > maxSza || aot > maxAot || hsf < minHsf || hsf > maxHsf) {
            setToNoData(targetSamples);
            return;
        }

        final double ozo = Math.max(0.0, fromKgPerSquareMeterToAtmPerCentimeter(sourceSamples[OZO_INDEX].getDouble()));  // could be negative due to bicubic interpolation
        final double cwv = Math.max(0.0, fromKgPerSquareMeterToGramPerSquareCentimeter(sourceSamples[CWV_INDEX].getDouble()));  // could be negative due to bicubic interpolation
        final double muv = Math.cos(Math.toRadians(vza));
        final double mus = Math.cos(Math.toRadians(sza));
        final double amf = 1.0 / muv + 1.0 / mus;

        final double[] toaReflectance = new double[TOA_RFL_BAND_NAMES.length];
        for (int i = 0; i < toaReflectance.length; i++) {
            toaReflectance[i] = sourceSamples[RFL_INDEX + i].getDouble() / OLCI_TO_MERIS_CALIBRATION_COEFFICIENTS[i];
        }

        final double raa = relativeAzimuthAngle(vaa, saa);
        final double[] tg = lookupTable.getTG(amf, cwv, ozo);
        final double[][] rt = lookupTable.getRT(aot, hsf, raa, sza, vza);

        final double[] pathReflectance = rt[0];
        final double[] totalTransmittance = rt[1];
        final double[] sphericalAlbedo = rt[2];

        for (int i = 0; i < OLC_SDR_BAND_NAMES.length; i++) {
            final double rho = (toaReflectance[i] / tg[i] - pathReflectance[i]) / totalTransmittance[i];
            final double sdr = rho / (1.0 + sphericalAlbedo[i] * rho);
            if (sdr > 0.0) {
                targetSamples[i].set(sdr);
            } else {
                setToNoData(targetSamples[i]);
            }
        }
    }

    private static void setToNoData(WritableSample[] targetSamples) {
        for (WritableSample targetSample : targetSamples) {
            if (targetSample.getIndex() != -1) {
                setToNoData(targetSample);
            }
        }
    }

    private static void setToNoData(WritableSample targetSample) {
        targetSample.set(Float.NaN);
    }

    private static double fromKgPerSquareMeterToAtmPerCentimeter(double ozo) {
        return ozo / 0.00803751;
    }

    private static double fromKgPerSquareMeterToGramPerSquareCentimeter(double cwv) {
        return cwv / 10.0;
    }

    private static double fromMeterToKilometer(double hsf) {
        return 0.001 * hsf;
    }

    private double clip(double hsf) {  // why is the height clipped this way?
        if (hsf < minHsf && hsf >= -0.45) {
            return minHsf;
        }
        return hsf;
    }

    private static double relativeAzimuthAngle(double vaa, double saa) {
        return Math.toDegrees(Math.acos(Math.cos(Math.toRadians(saa - vaa))));  // inefficient but always correct
    }

    @Override
    public void dispose() {
        lookupTable = null;

        super.dispose();
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AtmosphericCorrectionOp.class);
        }
    }

}
