package org.esa.s3tbx.c3solcislstr.ac;

import org.esa.s3tbx.c3solcislstr.mc.Multivariate;
import org.esa.s3tbx.c3solcislstr.mc.generators.LatinHypercube;
import org.esa.s3tbx.c3solcislstr.mc.generators.Melg;
import org.esa.s3tbx.c3solcislstr.mc.generators.Pcg;
import org.esa.s3tbx.c3solcislstr.mc.generators.Sobol;
import org.esa.s3tbx.c3solcislstr.mc.operators.ToaL1bMutationOp;
import org.esa.s3tbx.c3solcislstr.mc.variates.BoxMullerNormalVariate;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.common.PassThroughOp;
import org.esa.snap.core.util.ProductUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static org.esa.s3tbx.c3solcislstr.ac.OlciSlstrAcConstants.*;

/**
 * GPF operator for atmospheric correction on specific OLCI/SLSTR SYN Idepix product.
 *
 * @author Grit Kirches, Olaf Danne, Marco Peters
 */
@OperatorMetadata(alias = "OlciSlstrCollocationMutation", version = "0.81",
        authors = "G. Kirches, O.Danne, M.Peters",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018-2022 by Brockmann Consult",
        description = "Performs atmospheric correction on specific OLCI/SLSTR SYN Idepix product.\n" +
                " Uses approach from USwansea/FUB developed in GlobAlbedo and LandCover CCI.")
public class OlciSlstrCollocationMutationOp extends Operator {

    @Parameter(defaultValue = "true",
            label = "Copy geometry bands into SDR product",
            description = "If set, geometry bands are copied into SDR product")
    private boolean copyGeometryBands;

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


    @Parameter(label = "Regression coefficient",
            description = "The regression coefficient (see score summary statistics https://aerocom.met.no/cgi-bin/surfobs_annualrs.pl)",
            defaultValue = "1.0")
    private double regressionCoefficient;

    @Parameter(label = "Regression constant",
            description = "The regression constant (see score summary statistics https://aerocom.met.no/cgi-bin/surfobs_annualrs.pl)",
            defaultValue = "0.0")
    private double regressionConstant;

    @Parameter(label = "Error correlation",
            description = "The type of error correlation",
            defaultValue = "Constant", valueSet = {"None", "Constant"})
    private String camsErrorCorrelationType;

    @Parameter(label = "Error correlation coefficient",
            description = "The error correlation coefficient (used to generate a sequence of correlated random numbers).",
            defaultValue = "0.5", interval = "[0.0, 1.0]")
    private double camsErrorCorrelationCoefficient;

    @Parameter(label = "Use constant bias",
            description = "If checked, all random numbers are correlated with a constant bias rather than a random bias (using the specified error correlation coefficient).",
            defaultValue = "true")
    private boolean camsUseConstantBias;

    @Parameter(label = "Ucertainty model",
            description = "The type of uncertainty model",
            defaultValue = "Relative (10%)",
            valueSet = {"Relative (10%)", "Relative (15%)", "Relative (20%)"})
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


        // generation of radiance mutants...
        Product mutatedOlciProduct;
        Product mutatedSlstrProduct;
        Product mutatedCollocationProduct;
        if (mutant) {
            if (useConstantBias) {
                radBias = new BoxMullerNormalVariate(mv.get(0), mv.get(1)).nextDouble();
            }
            mutatedCollocationProduct = mutateOlciRadiance(sourceProduct);
            mutatedSlstrProduct = mutateSlstrRadiance(sourceProduct);
            for (int i=0; i< SLSTR_TOA_RAD_BAND_NAMES.length; i++) {
                final Band origSlstrBand = mutatedCollocationProduct.getBand(SLSTR_TOA_RAD_BAND_NAMES[i]);
                final Band mutatedSlstrBand = mutatedSlstrProduct.getBand(SLSTR_TOA_RAD_BAND_NAMES[i]);
                if (!mutatedCollocationProduct.containsBand(mutatedSlstrBand.getName())) {
                    ProductUtils.copyBand(mutatedSlstrBand.getName(),mutatedSlstrProduct, mutatedCollocationProduct, true);
                }
            }
        } else {
            mutatedCollocationProduct = GPF.createProduct(getName(PassThroughOp.class), GPF.NO_PARAMS, sourceProduct);
        }

        setTargetProduct(mutatedCollocationProduct);

        if (copyGeometryBands) {
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
        if (rasterDataNodeOlci != null && !(getTargetProduct().containsBand(geomBandNameOlci))) {
            ProductUtils.copyBand(geomBandNameOlci, sourceProduct, getTargetProduct(), true);
        } else {
            rasterDataNodeOlci = sourceProduct.getTiePointGrid(geomBandNameOlci);
            if (rasterDataNodeOlci != null && !(getTargetProduct().containsTiePointGrid(geomBandNameOlci))) {
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

    private Product mutateOlciRadiance(Product product) {
        return GPF.createProduct(getName(ToaL1bMutationOp.class), olciRadianceMutationParameterMap(), product);
    }

    private Product mutateSlstrRadiance(Product product) {
        return GPF.createProduct(getName(ToaL1bMutationOp.class), slstrRadianceMutationParameterMap(), product);
    }


    @NotNull
    private Map<String, Object> olciRadianceMutationParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("rngType", MELG);
        map.put("seedNumber", pcg.nextLong());
        map.put("seedString", DATE_AND_TIME_OF_SOURCE);
        map.put("positiveDefinite", true);
        map.put("errorCorrelationType", errorCorrelationType);
        map.put("errorCorrelationCoefficient", errorCorrelationCoefficient);
        map.put("useConstantBias", useConstantBias);
        map.put("bias", radBias);
        map.put("uncertaintyModelType", uncertaintyModelType);
        map.put("measurandNames", OLCI_TOA_RAD_BAND_NAMES);
        map.put("measurandUncertaintyNames", OLCI_TOA_RAD_UNC_BAND_NAMES);
        map.put("useUncertaintyModel", false);
        return map;
    }

    private Map<String, Object> slstrRadianceMutationParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("rngType", MELG);
        map.put("seedNumber", pcg.nextLong());
        map.put("seedString", DATE_AND_TIME_OF_SOURCE);
        map.put("positiveDefinite", true);
        map.put("errorCorrelationType", errorCorrelationType);
        map.put("errorCorrelationCoefficient", errorCorrelationCoefficient);
        map.put("useConstantBias", useConstantBias);
        map.put("bias", radBias);
        map.put("uncertaintyModelType", uncertaintyModelType);
        map.put("measurandNames", SLSTR_TOA_RAD_BAND_NAMES);
        map.put("useUncertaintyModel", true);
        return map;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciSlstrCollocationMutationOp.class);
        }
    }
}
