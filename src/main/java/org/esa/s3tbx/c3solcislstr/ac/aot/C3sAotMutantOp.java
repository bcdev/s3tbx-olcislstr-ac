package org.esa.s3tbx.c3solcislstr.ac.aot;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.c3solcislstr.ac.MutantProvider;
import org.esa.s3tbx.c3solcislstr.ac.OlciSlstrAcConstants;
import org.esa.s3tbx.c3solcislstr.mc.UncertaintyModel;
import org.esa.s3tbx.c3solcislstr.mc.operators.Cube;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

@OperatorMetadata(alias = "C3sAotMutant", version = "0.8",
        authors = "O. Danne",
        internal = true,
        copyright = "Copyright (C) 2026 by Brockmann Consult",
        description = "Operator for Monte Carlo mutations of AOT retrievals.")
public class C3sAotMutantOp extends Operator {

    // MC related parameters...
    @Parameter(label = "Random number generator",
            description = "The type of random number generator",
            defaultValue = "MELG", valueSet = {"MELG", "PCG"})
    private String rngType;

    @Parameter(label = "Seed number",
            description = "A numeric value to seed the random number generator",
            defaultValue = "5489")
    private long seedNumber;

    @Parameter(label = "Seed string",
            description = "An alphanumeric value to seed the random number generator (US-ASCII character set). If empty, the seed value is determined by the date and time associated with the CAMS parent product.")
    private String seedString;

    @Parameter(label = "Positive definite",
            description = "If checked, the aerosol optical depth is considered positive definite.",
            defaultValue = "true")
    private boolean positiveDefinite;

    @Parameter(label = "Least positive value",
            description = "Tiny number, used if an aerosol optical depth is zero (e.g., due to discretization) even though it is considered positive definite.",
            defaultValue = "1.0E-10")
    private double tiny;

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
            defaultValue = "None", valueSet = {"None", "Constant"})
    private String errorCorrelationType;

    @Parameter(label = "Error correlation coefficient",
            description = "The error correlation coefficient (used to generate a sequence of correlated random numbers).",
            defaultValue = "0.0", interval = "[0.0, 1.0]")
    private double errorCorrelationCoefficient;

    @Parameter(label = "Error codec",
            description = "The type of error codec used",
            defaultValue = "Linear", valueSet = {"Laplace", "Linear", "Normal"})
    private String errorCodecType;

    @Parameter(label = "Use constant bias",
            description = "If checked, all random numbers are correlated with a constant bias rather than a random bias (using the specified error correlation coefficient).",
            defaultValue = "false")
    private boolean useConstantBias;

    @Parameter(label = "Constant bias",
            description = "A constant bias value, which must be a draw from a standard normal distribution.",
            defaultValue = "0.0")
    private double bias;

    @Parameter(label = "Uncertainty model",
            description = "The type of uncertainty model",
            defaultValue = "CAMS AOD (2020)",
            valueSet = {"CAMS AOD (2020)", "Relative (10%)", "Relative (15%)", "Relative (20%)"})
    private String uncertaintyModelType;

    // end MC related parameters

    @SourceProduct(label = "Source product", description = "The source product.")
    private Product sourceProduct;

    @TargetProduct(label = "Target product", description = "The target product")
    private Product targetProduct;


    private UncertaintyModel uncertaintyModel;
    private Cube random;

    private Band aotSourceBand;

    @Override
    public void initialize() throws OperatorException {
        aotSourceBand = sourceProduct.getBand(OlciSlstrAcConstants.AOT_BAND_NAME);
        final int w = sourceProduct.getSceneRasterWidth();
        final int h = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), w, h);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        final Band aotBand = targetProduct.addBand(OlciSlstrAcConstants.AOT_BAND_NAME, ProductData.TYPE_FLOAT32);
        aotBand.setDescription(aotSourceBand.getDescription());
        aotBand.setUnit(aotSourceBand.getUnit());

        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);

        uncertaintyModel = MutantProvider.initializeUncertaintyModel(uncertaintyModelType);

        try {
            random = MutantProvider.initializeRandomNumbers(sourceProduct, 1,
                    seedNumber, seedString, rngType, useConstantBias, bias, errorCodecType,
                    errorCorrelationType, errorCorrelationCoefficient);
        } catch (Exception e) {
            throw new OperatorException("Random noise for TOA could not be initialized.", e);
        }
    }

    private double correctedValue(double x) {
        return regressionCoefficient * x + regressionConstant;
    }


    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();

        final Tile aotSourceTile = getSourceTile(aotSourceBand, targetRectangle);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {

                final double parentValue = aotSourceTile.getSampleDouble(x, y);
                // mutate total aerosol optical depth value
                final double u = uncertaintyModel.getUncertainty(parentValue);
                final double z = random.get(x, y, 0);
                double targetValue = MutantProvider.getMutatedValue(correctedValue(parentValue), u, z, positiveDefinite);
                // mutate (i.e. scale) specific aerosol optical depth values correspondingly
                final double aotMutant = parentValue * (targetValue / parentValue);
                // set mutated aerosol optical depth values
                targetTile.setSample(x, y, aotMutant);
            }
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(C3sAotMutantOp.class);
        }
    }
}
