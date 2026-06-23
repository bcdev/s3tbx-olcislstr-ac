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

import org.esa.s3tbx.c3solcislstr.mc.Multivariate;
import org.esa.s3tbx.c3solcislstr.mc.UniformVariate;
import org.esa.s3tbx.c3solcislstr.mc.generators.LatinHypercube;
import org.esa.s3tbx.c3solcislstr.mc.generators.Melg;
import org.esa.s3tbx.c3solcislstr.mc.generators.Pcg;
import org.esa.s3tbx.c3solcislstr.mc.generators.Sobol;
import org.esa.s3tbx.c3solcislstr.mc.variates.BoxMullerNormalVariate;
import org.esa.s3tbx.c3solcislstr.mc.variates.EmpiricVariate;
import org.esa.snap.collocation.CollocateOp;
import org.esa.snap.collocation.ResamplingType;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.netcdf.util.MetadataUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.esa.s3tbx.c3solcislstr.mc.operators.AtmosphericCorrectionOp.*;

/**
 * Operator for retrieving CCI Medium Resolution Land Cover Surface Directional Reflectance (SDR).
 * The operator wraps an internal processing graph. Applicable to OLCI only.
 *
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "SdrProcessor", version = "0.1",
        authors = "Ralf Quast",
        category = "OLCI",
        copyright = "Copyright (C) 2021 by Brockmann Consult",
        description = "Operator for retrieving CCI Medium Resolution Land Cover Surface Directional Reflectance")
public class SdrOp extends Operator {

    static final String[] RAD_BAND_NAMES = new String[]{
            "Oa01_radiance", "Oa02_radiance", "Oa03_radiance", "Oa04_radiance", "Oa05_radiance", "Oa06_radiance",
            "Oa07_radiance", "Oa08_radiance", "Oa09_radiance", "Oa10_radiance", "Oa11_radiance", "Oa12_radiance",
            "Oa13_radiance", "Oa14_radiance", "Oa15_radiance", "Oa16_radiance", "Oa17_radiance", "Oa18_radiance",
            "Oa19_radiance", "Oa20_radiance", "Oa21_radiance"
    };

    static final String[] UNC_BAND_NAMES = new String[]{
            "Oa01_radiance_unc", "Oa02_radiance_unc", "Oa03_radiance_unc", "Oa04_radiance_unc", "Oa05_radiance_unc", "Oa06_radiance_unc",
            "Oa07_radiance_unc", "Oa08_radiance_unc", "Oa09_radiance_unc", "Oa10_radiance_unc", "Oa11_radiance_unc", "Oa12_radiance_unc",
            "Oa13_radiance_unc", "Oa14_radiance_unc", "Oa15_radiance_unc", "Oa16_radiance_unc", "Oa17_radiance_unc", "Oa18_radiance_unc",
            "Oa19_radiance_unc", "Oa20_radiance_unc", "Oa21_radiance_unc"
    };

    private static final String DATE_AND_TIME_OF_PARENT = "";
    private static final String DATE_AND_TIME_OF_SOURCE = "";
    private static final boolean DO_NOT_RENAME = false;
    private static final String MELG = "MELG";

    @Parameter(label = "Seed number",
            description = "A numeric value to seed the random number generator",
            defaultValue = "42")
    private long seed;

    @Parameter(label = "Selector",
            description = "A numeric value to select the random stream. If zero, no randomization is performed at all.",
            defaultValue = "0")
    private long selector;

    @Parameter(label = "Sampling type",
            description = "The sampling type.",
            defaultValue = "Sobol", valueSet = {"Latin hypercube", "Random", "Sobol"})
    private String samplingType;

    @Parameter(label = "Simulation count",
            description = "The number of simulations (only used for Latin hypercube sampling).",
            defaultValue = "10")
    private int simulationCount;

    @Parameter(label = "Radiance error correlation",
            description = "The type of error correlation",
            defaultValue = "Constant", valueSet = {"None", "Constant"})
    private String radErrorCorrelationType;

    @Parameter(label = "Radiance error correlation coefficient",
            description = "The error correlation coefficient (used to generate a sequence of correlated random numbers).",
            defaultValue = "0.5", interval = "[0.0, 1.0]")
    private double radErrorCorrelationCoefficient;

    @Parameter(label = "Use constant radiance bias",
            description = "If checked, all random numbers are correlated with a constant bias rather than a random bias (using the specified error correlation coefficient).",
            defaultValue = "true")
    private boolean radUseConstantBias;

    @Parameter(label = "Radiance uncertainty model",
            description = "The type of uncertainty model",
            defaultValue = "Relative", valueSet = {"Poisson", "Relative"})
    private String radUncertaintyModelType;

    @Parameter(label = "Cloud-to-clear NN threshold over land",
            description = "The NN value threshold to separate clouds from cloud-free land surface. If zero, a threshold value is generated randomly.",
            defaultValue = "0.0")
    private double cloudyClearThresholdLnd;

    @Parameter(label = "Cloud-to-clear NN threshold over water",
            description = "The NN value threshold to separate clouds from cloud-free water surface. If zero, a threshold value is generated randomly.",
            defaultValue = "0.0")
    private double cloudyClearThresholdWtr;

    @Parameter(label = "Randomly mutant cloud-clear",
            description = "If checked, the cloud mask is mutated randomly.",
            defaultValue = "true")
    private boolean randomlyMutantCloudyClear;

    @Parameter(defaultValue = "2", interval = "[0, 100]",
            description = "The width (pixels) of the 'safety buffer' around a pixel, which is classified as cloudy.",
            label = "Width of the cloud buffer")
    private int cloudBufferWidth;

    @Parameter(label = "CAMS repository",
            description = "Location of the CAMS aerosol product repository (or of a specific product file)",
            notNull = true, notEmpty = true)
    private File camsRepository;

    @Parameter(label = "CAMS regression coefficient",
            description = "The regression coefficient (see score summary statistics https://aerocom.met.no/cgi-bin/surfobs_annualrs.pl)",
            defaultValue = "1.0")
    private double camsRegressionCoefficient;

    @Parameter(label = "CAMS regression constant",
            description = "The regression constant (see score summary statistics https://aerocom.met.no/cgi-bin/surfobs_annualrs.pl)",
            defaultValue = "0.0")
    private double camsRegressionConstant;

    @Parameter(label = "CAMS error correlation",
            description = "The type of error correlation",
            defaultValue = "Constant", valueSet = {"None", "Constant"})
    private String camsErrorCorrelationType;

    @Parameter(label = "CAMS error correlation coefficient",
            description = "The error correlation coefficient (used to generate a sequence of correlated random numbers).",
            defaultValue = "0.5", interval = "[0.0, 1.0]")
    private double camsErrorCorrelationCoefficient;

    @Parameter(label = "Use constant CAMS bias",
            description = "If checked, all random numbers are correlated with a constant bias rather than a random bias (using the specified error correlation coefficient).",
            defaultValue = "true")
    private boolean camsUseConstantBias;

    @Parameter(label = "CAMS uncertainty model",
            description = "The type of uncertainty model",
            defaultValue = "CAMS AOD (2020)",
            valueSet = {"CAMS AOD (2020)", "Relative (10%)", "Relative (15%)", "Relative (20%)"})
    private String camsUncertaintyModelType;

    @Parameter(label = "Compute SDR everywhere",
            defaultValue = "true",
            description = "If checked, surface directional reflectance is computed everywhere.")
    private boolean computeSdrEverywhere;

    @Parameter(defaultValue = "false",
            description = "If checked, the atmospheric correction is performed without considering aerosols.")
    private boolean ignoreAerosols;

    @Parameter(label = "Clone all data",
            description = "If checked, all data are copied from source to target, if not conflicting.",
            defaultValue = "true")
    private boolean cloneAll;

    @Parameter(label = "Activate test mode",
            description = "Not used, for compatibility with previous versions only.",
            defaultValue = "false")
    private boolean activateTestMode;

    @SourceProduct(label = "Source product", description = "The source product")
    private Product sourceProduct;

    @TargetProduct(label = "Target product", description = "The target product")
    private Product targetProduct;

    private Pcg pcg;
    private double radBias;
    private double camsBias;
    @SuppressWarnings("FieldCanBeLocal")
    private Multivariate mv;

    @Override
    public void initialize() throws OperatorException {
        pcg = new Pcg(seed, selector);
        mv = multivariate(samplingType);

        if (radUseConstantBias) {
            radBias = new BoxMullerNormalVariate(mv.get(0), mv.get(1)).nextDouble();
        }
        if (camsUseConstantBias) {
            camsBias = new BoxMullerNormalVariate(mv.get(2), mv.get(3)).nextDouble();
        }
        if (cloudyClearThresholdLnd == 0.0) {
            cloudyClearThresholdLnd = randomCloudyClearThreshold(mv.get(4), "threshold_pdf_lnd.dat");
        }
        if (cloudyClearThresholdWtr == 0.0) {
            cloudyClearThresholdWtr = randomCloudyClearThreshold(mv.get(5), "threshold_pdf_wtr.dat");
        }

        targetProduct = performAtmosphericCorrection(prepareAtmosphericCorrection(sourceProduct));
        if (isMutant()) {
            targetProduct = mutateSurfaceReflectance(targetProduct);
        }

        for (int i = 0; i < TOA_RFL_BAND_NAMES.length; i++) {
            final Band olcBand = targetProduct.getBand(OLC_SDR_BAND_NAMES[i]);
            final Band merBand = targetProduct.addBand(MER_SDR_BAND_NAMES[i], OLC_SDR_BAND_NAMES[i]);
            ProductUtils.copySpectralBandProperties(olcBand, merBand);
            merBand.setDescription("MERIS equivalent for OLCI '" + OLC_SDR_BAND_NAMES[i] + "'");
        }
        addGlobalAttribute(this.targetProduct, "selector", selector);
        if (selector != 0) {
            addGlobalAttribute(targetProduct, "seed", seed);
            addGlobalAttribute(targetProduct, "cloudy_clear_threshold_lnd", cloudyClearThresholdLnd);
            addGlobalAttribute(targetProduct, "cloudy_clear_threshold_wtr", cloudyClearThresholdWtr);
            addGlobalAttribute(targetProduct, "randomly_mutant_cloudy_clear", randomlyMutantCloudyClear);
            addGlobalAttribute(targetProduct, "rad_error_correlation_type", radErrorCorrelationType);
            addGlobalAttribute(targetProduct, "rad_error_correlation_coefficient", radErrorCorrelationCoefficient);
            addGlobalAttribute(targetProduct, "rad_use_constant_bias", radUseConstantBias);
            addGlobalAttribute(targetProduct, "rad_bias", radBias);
            addGlobalAttribute(targetProduct, "rad_uncertainty_model_type", radUncertaintyModelType);
            addGlobalAttribute(targetProduct, "cams_error_correlation_type", camsErrorCorrelationType);
            addGlobalAttribute(targetProduct, "cams_error_correlation_coefficient", camsErrorCorrelationCoefficient);
            addGlobalAttribute(targetProduct, "cams_use_constant_bias", camsUseConstantBias);
            addGlobalAttribute(targetProduct, "cams_bias", camsBias);
            addGlobalAttribute(targetProduct, "cams_uncertainty_model_type", camsUncertaintyModelType);
        }
    }

    private Multivariate multivariate(String samplingType) {
        switch (samplingType) {
            case "Latin hypercube":
                return new LatinHypercube(6, simulationCount, selector, new Melg(seed));
            case "Sobol":
                return new Sobol(6).start(6 + selector + seed);
            default:
                return pcg;
        }
    }

    private static void addGlobalAttribute(Product product, String name, boolean value) {
        final MetadataElement root = product.getMetadataRoot();
        if (root.getElement(MetadataUtils.GLOBAL_ATTRIBUTES) == null) {
            root.addElement(new MetadataElement(MetadataUtils.GLOBAL_ATTRIBUTES));
        }
        root.getElement(MetadataUtils.GLOBAL_ATTRIBUTES).addAttribute(createAttribute(name, Boolean.toString(value)));
    }

    private static void addGlobalAttribute(Product product, String name, double value) {
        addGlobalAttribute(product, name, Double.toString(value));
    }

    private static void addGlobalAttribute(Product product, String name, long value) {
        addGlobalAttribute(product, name, Long.toUnsignedString(value));
    }

    private static void addGlobalAttribute(Product product, String name, String value) {
        final MetadataElement root = product.getMetadataRoot();
        if (root.getElement(MetadataUtils.GLOBAL_ATTRIBUTES) == null) {
            root.addElement(new MetadataElement(MetadataUtils.GLOBAL_ATTRIBUTES));
        }
        root.getElement(MetadataUtils.GLOBAL_ATTRIBUTES).addAttribute(createAttribute(name, value));
    }

    @NotNull
    private static MetadataAttribute createAttribute(String name, String value) {
        return new MetadataAttribute(name, ProductData.createInstance(value), true);
    }

    private static double randomCloudyClearThreshold(UniformVariate u, String resource) {
        return new EmpiricVariate(InterpolationFunctionFactory.create("InversePrimitiveStep", resource), u).nextDouble();
    }

    private Product mutateSurfaceReflectance(Product product) {
        return GPF.createProduct(getName(RadianceMutationOp.class), surfaceReflectanceMutationParameterMap(), product);
    }

    private Product performAtmosphericCorrection(Product product) {
        return GPF.createProduct(getName(AtmosphericCorrectionOp.class), atmosphericCorrectionParameterMap(), product);
    }

    private Product prepareAtmosphericCorrection(Product product) {
        return GPF.createProduct(getName(CollocateOp.class), collocationParameterMap(), collocationProductMap(product));
    }

    @NotNull
    private Map<String, Product> collocationProductMap(Product product) {
        final Map<String, Product> map = new HashMap<>();
        if (isMutant()) {
            map.put("reference", computeCloudShadow(mutateCloudMask(identifyPixels(mutateRadiance(product)))));
        } else {  // no mutation
            map.put("reference", computeCloudShadow(identifyPixels(product)));
        }
        map.put("secondary", retrieveAerosol(product));
        return map;
    }

    private boolean isMutant() {
        return selector != 0;
    }

    private Product mutateRadiance(Product product) {
        return GPF.createProduct(getName(RadianceMutationOp.class), radianceMutationParameterMap(), product);
    }

    private Product identifyPixels(Product product) {
        return GPF.createProduct(getName(PixelIdentificationOp.class), pixelIdentificationParameterMap(), product);
    }

    private Product mutateCloudMask(Product product) {
        return GPF.createProduct(getName(CloudMaskMutationOp.class), cloudMaskMutationParameterMap(), product);
    }

    private Product computeCloudShadow(Product product) {
        return GPF.createProduct(getName(CloudShadowOp.class), cloudShadowParameterMap(), product);
    }

    private Product retrieveAerosol(Product product) {
        return GPF.createProduct(getName(AerosolRetrievalOp.class), aerosolRetrievalParameterMap(), product);
    }

    @NotNull
    private Map<String, Object> surfaceReflectanceMutationParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("rngType", MELG);
        map.put("seedNumber", pcg.nextLong());
        map.put("seedString", DATE_AND_TIME_OF_SOURCE);
        map.put("positiveDefinite", true);
        map.put("useUncertaintyModel", true);
        map.put("uncertaintyModelType", "Relative");
        map.put("measurandNames", OLC_SDR_BAND_NAMES);
        return map;
    }

    @NotNull
    private Map<String, Object> radianceMutationParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("rngType", MELG);
        map.put("seedNumber", pcg.nextLong());
        map.put("seedString", DATE_AND_TIME_OF_SOURCE);
        map.put("positiveDefinite", true);
        map.put("errorCorrelationType", radErrorCorrelationType);
        map.put("errorCorrelationCoefficient", radErrorCorrelationCoefficient);
        map.put("useConstantBias", radUseConstantBias);
        map.put("bias", radBias);
        map.put("uncertaintyModelType", radUncertaintyModelType);
        map.put("measurandNames", RAD_BAND_NAMES);
        if (sourceProduct.containsBand(UNC_BAND_NAMES[0])) {
            map.put("measurandUncertaintyNames", UNC_BAND_NAMES);
            map.put("useUncertaintyModel", false);
        } else {
            map.put("useUncertaintyModel", true);
        }
        return map;
    }

    @NotNull
    private Map<String, Object> pixelIdentificationParameterMap() {
        return new HashMap<>();
    }

    @NotNull
    private Map<String, Object> cloudMaskMutationParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("rngType", MELG);
        map.put("seedNumber", pcg.nextLong());
        map.put("seedString", DATE_AND_TIME_OF_SOURCE);
        map.put("cloudyClearThresholdLnd", cloudyClearThresholdLnd);
        map.put("cloudyClearThresholdWtr", cloudyClearThresholdWtr);
        map.put("randomlyMutant", randomlyMutantCloudyClear);
        return map;
    }

    @NotNull
    private Map<String, Object> cloudShadowParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("cloudBufferWidth", cloudBufferWidth);
        map.put("computeCloudBuffer", false);
        map.put("computeCloudShadow", false);
        return map;
    }

    @NotNull
    private Map<String, Object> aerosolRetrievalParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("mutant", isMutant());
        map.put("rngType", MELG);
        map.put("seedNumber", pcg.nextLong());
        map.put("seedString", DATE_AND_TIME_OF_PARENT);
        map.put("repository", camsRepository);
        map.put("regressionCoefficient", camsRegressionCoefficient);
        map.put("regressionConstant", camsRegressionConstant);
        map.put("errorCorrelationType", camsErrorCorrelationType);
        map.put("errorCorrelationCoefficient", camsErrorCorrelationCoefficient);
        map.put("useConstantBias", camsUseConstantBias);
        map.put("bias", camsBias);
        map.put("uncertaintyModelType", camsUncertaintyModelType);
        return map;
    }

    @NotNull
    private Map<String, Object> collocationParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("targetProductType", sourceProduct.getProductType());
        map.put("renameReferenceComponents", DO_NOT_RENAME);
        map.put("renameSecondaryComponents", DO_NOT_RENAME);
        map.put("resamplingType", ResamplingType.BICUBIC_CONVOLUTION);
        return map;
    }

    @NotNull
    private Map<String, Object> atmosphericCorrectionParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("computeSdrEverywhere", computeSdrEverywhere);
        map.put("cloneAll", cloneAll);
        map.put("ignoreAerosols", ignoreAerosols);
        return map;
    }

    private static String getName(Class<? extends Operator> operatorClass) {
        return OperatorSpi.getOperatorAlias(operatorClass);  // returns an alias or simple class name
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SdrOp.class);
        }
    }

}
