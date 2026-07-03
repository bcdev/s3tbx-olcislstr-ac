package org.esa.s3tbx.c3solcislstr.ac;

import org.esa.s3tbx.c3solcislstr.ac.aot.AotConsts;
import org.esa.s3tbx.c3solcislstr.ac.aot.C3sAotMasterOp;
import org.esa.s3tbx.c3solcislstr.mc.Multivariate;
import org.esa.s3tbx.c3solcislstr.mc.generators.LatinHypercube;
import org.esa.s3tbx.c3solcislstr.mc.generators.Melg;
import org.esa.s3tbx.c3solcislstr.mc.generators.Pcg;
import org.esa.s3tbx.c3solcislstr.mc.generators.Sobol;
import org.esa.s3tbx.c3solcislstr.mc.variates.BoxMullerNormalVariate;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.esa.s3tbx.c3solcislstr.ac.OlciSlstrAcConstants.*;

/**
 * GPF operator for atmospheric correction on specific OLCI/SLSTR SYN Idepix product.
 *
 * @author Grit Kirches, Olaf Danne, Marco Peters
 */
@OperatorMetadata(alias = "OlciSlstrAc", version = "0.81",
        authors = "G. Kirches, O.Danne, M.Peters",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018-2022 by Brockmann Consult",
        description = "Performs atmospheric correction on specific OLCI/SLSTR SYN Idepix product.\n" +
                " Uses approach from USwansea/FUB developed in GlobAlbedo and LandCover CCI.")
public class OlciSlstrAcOp extends Operator {

    @Parameter(defaultValue = "false",
            label = "Only compute AOT product",
            description = "If set, only AOT product is generated instead of full SDR product")
    private boolean aotOnly;

    @Parameter(defaultValue = "true",
            label = "Apply mutation on input TOA reflectances",
            description = "If set, input TOA reflectances are mutated ")
    private boolean mutateToa;

    @Parameter(defaultValue = "true",
            label = "Apply mutation on AOT",
            description = "If set, AOT is mutated ")
    private boolean mutateAot;

    @Parameter(defaultValue = "true",
            label = "Apply mutation on SDR reflectances",
            description = "If set, computed SDR are mutated ")
    private boolean mutateSdr;

    @Parameter(defaultValue = "true",
            label = "Copy AOT bands into SDR product",
            description = "If set, bands of AOT and its uncertainty are copied into SDR product")
    private boolean copyAotBands;

    @Parameter(defaultValue = "true",
            label = "Copy geometry bands into SDR product",
            description = "If set, geometry bands are copied into SDR product")
    private boolean copyGeometryBands;

    @Parameter(defaultValue = "true",
            label = "Write SDR uncertainty bands",
            description = "If set, SDR uncertainty bands will be written into SDR product")
    private boolean writeSdrUncertaintyBands;

    @Parameter(defaultValue = "true", label = "Compute SDR everywhere (ignore clouds, water)")
    private boolean computeSdrEverywhere;

    @Parameter(defaultValue = "true", label = "Compute AOT everywhere (ignore clouds, water)")
    private boolean computeAotEverywhere;

    @Parameter(description = "Path to atmospheric parameter LUTs.")
    private String pathToAtmosphericParameterLuts;


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
    private String errorCorrelationType;

    @Parameter(label = "Radiance error correlation coefficient",
            description = "The error correlation coefficient (used to generate a sequence of correlated random numbers).",
            defaultValue = "0.5", interval = "[0.0, 1.0]")
    private double errorCorrelationCoefficient;

    @Parameter(label = "Use constant radiance bias",
            description = "If checked, all random numbers are correlated with a constant bias rather than a random bias (using the specified error correlation coefficient).",
            defaultValue = "true")
    private boolean useConstantBias;

    @Parameter(label = "Radiance uncertainty model",
            description = "The type of uncertainty model",
            defaultValue = "Relative", valueSet = {"Poisson", "Relative"})
    private String uncertaintyModelType;


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


    @SourceProduct(description = "C3S SYN OLCI SLSTR product",
            label = "C3S SYN OLCI SLSTR L1b product")
    private Product sourceProduct;

    private Pcg pcg;
    private double camsBias;
    @SuppressWarnings("FieldCanBeLocal")
    private Multivariate mv;
    private boolean mutant;

    private S3OlciSlstrSensor sensor;
    private double radBias = 0.0;

    @Override
    public void initialize() throws OperatorException {
        sensor = determineSensor(sourceProduct);

        pcg = new Pcg(seed, selector);
        mv = multivariate(samplingType);
        if (useConstantBias) {
            radBias = new BoxMullerNormalVariate(mv.get(0), mv.get(1)).nextDouble();
        }
        mutant = isMutant();


        // generation of AOT mutant is triggered in C3sAotMasterOp...
        if (camsUseConstantBias) {
            camsBias = new BoxMullerNormalVariate(mv.get(2), mv.get(3)).nextDouble();
        }
        Product aotProduct;
        aotProduct = processAot(sourceProduct);
        if (aotProduct == C3sAotMasterOp.EMPTY_PRODUCT) {
            Logger.getLogger(getClass().getName()).warning("aotProduct is empty");
            setTargetProduct(C3sAotMasterOp.EMPTY_PRODUCT);
            return;
        }

        if (aotOnly) {
            // mainly for debugging/verification
            setTargetProduct(aotProduct);
        } else {
            if (mutant && (mutateToa || mutateSdr)) {
                setTargetProduct(processSdrMutated(sourceProduct, aotProduct));
            } else {
                setTargetProduct(processSdr(sourceProduct, aotProduct));
            }
        }

        // generation of SDR mutant
//        if (mutant && !aotOnly && mutateSdr) {
//            setTargetProduct(mutateSurfaceReflectance(getTargetProduct()));
//        }

        if (copyAotBands && !aotOnly) {
            ProductUtils.copyBand(OlciSlstrAcConstants.AOT_BAND_NAME, aotProduct, getTargetProduct(), true);
            ProductUtils.copyBand(OlciSlstrAcConstants.AOT_ERR_BAND_NAME, aotProduct, getTargetProduct(), true);
            if (aotProduct.containsBand(AotConsts.aotFlags.name)) {
                ProductUtils.copyBand(AotConsts.aotFlags.name, aotProduct, getTargetProduct(), true);
            }

        }

        if (copyGeometryBands && !aotOnly) {
            for (String geomBandNameOlci : sensor.getGeomBandNamesOlci()) {
                copySourceBands(geomBandNameOlci);
            }
            for (String geomBandNameSlstr : sensor.getGeomBandNamesSlstrNadir()) {
                copySourceBands(geomBandNameSlstr);
            }
        }
    }

    private void copySourceBands(String geomBandNameOlci) {
        RasterDataNode rasterDataNodeOlci = sourceProduct.getBand(geomBandNameOlci);
        if (rasterDataNodeOlci != null) {
            ProductUtils.copyBand(geomBandNameOlci, sourceProduct, getTargetProduct(), true);
        } else {
            rasterDataNodeOlci = sourceProduct.getTiePointGrid(geomBandNameOlci);
            if (rasterDataNodeOlci != null) {
                ProductUtils.copyTiePointGrid(geomBandNameOlci, sourceProduct, getTargetProduct());
            }
        }
    }

    private S3OlciSlstrSensor determineSensor(Product l1bProduct) {
        if (l1bProduct.getName().contains("SY_1_")) {
            if (l1bProduct.getName().startsWith("S3A_SY_1_SYN")) {
                return S3OlciSlstrSensor.OLCI_SLSTR_S3A;
            } else {
                return S3OlciSlstrSensor.OLCI_SLSTR_S3B;
            }
        } else {
            throw new OperatorException(String.format("Product of type '%s' not supported.",
                    l1bProduct.getProductType()));
        }
    }

    private Product processAot(Product productSourceAot) {
        return GPF.createProduct(getName(C3sAotMasterOp.class), aerosolRetrievalParameterMap(), productSourceAot);
    }

    private Product processSdr(Product sourceProduct, Product aotProduct) {
        Map<String, Product> sdrSourceProducts = new HashMap<>();
        sdrSourceProducts.put("sourceProduct", sourceProduct);
        sdrSourceProducts.put("aotProduct", aotProduct);
        return GPF.createProduct(getName(C3sSdrOlciSlstrOp.class), sdrParameterMap(), sdrSourceProducts);
    }

    private Product processSdrMutated(Product sourceProduct, Product aotProduct) {
        Map<String, Product> sdrSourceProducts = new HashMap<>();
        sdrSourceProducts.put("sourceProduct", sourceProduct);
        sdrSourceProducts.put("aotProduct", aotProduct);
        return GPF.createProduct(getName(C3sSdrOlciSlstrMutantOp.class), sdrMutantParameterMap(), sdrSourceProducts);
    }

    private String[] getAtmosphericParametersLutFilePaths() {
        if (sensor == S3OlciSlstrSensor.OLCI_SLSTR_S3A) {
            return new String[]{
                    pathToAtmosphericParameterLuts + File.separator + S3_A_OLCI_ATM_PARAMS_LUT_NAME,
                    pathToAtmosphericParameterLuts + File.separator + S3_A_SLSTR_ATM_PARAMS_LUT_NAME
            };
        } else {
            return new String[]{
                    pathToAtmosphericParameterLuts + File.separator + S3_B_OLCI_ATM_PARAMS_LUT_NAME,
                    pathToAtmosphericParameterLuts + File.separator + S3_B_SLSTR_ATM_PARAMS_LUT_NAME
            };
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

    private boolean isMutant() {
        return selector != 0;
    }

    private static String getName(Class<? extends Operator> operatorClass) {
        return OperatorSpi.getOperatorAlias(operatorClass);  // returns an alias or simple class name
    }

    @NotNull
    private Map<String, Object> aerosolRetrievalParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("sensor", sensor);
        map.put("useConstantAot", false);
        map.put("constantAotValue", 0.15f);
        map.put("computeAotEverywhere", computeAotEverywhere);
        map.put("mutant", mutant);
        map.put("mutateAot", mutateAot);
        map.put("rngType", MELG);
        map.put("positiveDefinite", true);
        map.put("seedNumber", pcg.nextLong());
        map.put("seedString", DATE_AND_TIME_OF_PARENT);
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
    private Map<String, Object> sdrParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("sensor", sensor);
        map.put("computeSdrEverywhere", computeSdrEverywhere);
        map.put("writeSdrUncertaintyBands", writeSdrUncertaintyBands);
        final String[] s3aLutNames = getAtmosphericParametersLutFilePaths();
        map.put("pathToLutOlci", s3aLutNames[0]);
        map.put("pathToLutSlstr", s3aLutNames[1]);

        return map;
    }

    @NotNull
    private Map<String, Object> sdrMutantParameterMap() {
        final Map<String, Object> map = sdrParameterMap();
        map.put("rngType", MELG);
        map.put("toaSeedNumber", pcg.nextLong());
        map.put("sdrSeedNumber", pcg.nextLong());
        map.put("seedString", DATE_AND_TIME_OF_SOURCE);
        map.put("positiveDefinite", true);
        map.put("errorCorrelationType", errorCorrelationType);
        map.put("errorCorrelationCoefficient", errorCorrelationCoefficient);
        map.put("useConstantBias", useConstantBias);
        map.put("bias", radBias);
        map.put("uncertaintyModelType", uncertaintyModelType);
        map.put("toaMmeasurandNames", OLCI_SLSTR_TOA_BAND_NAMES);
        map.put("sdrMmeasurandNames", OLCI_SLSTR_SDR_BAND_NAMES);
        map.put("mutateToa", mutateSdr);
        map.put("mutateSdr", mutateSdr);

        return map;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciSlstrAcOp.class);
        }
    }
}
