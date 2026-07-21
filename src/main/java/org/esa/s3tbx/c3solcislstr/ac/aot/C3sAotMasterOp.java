package org.esa.s3tbx.c3solcislstr.ac.aot;

import org.esa.s3tbx.c3solcislstr.ac.S3OlciSlstrSensor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Master operator for AOT retrieval. Parent of sensor specific operators (here i.e. OLCI, MERIS).
 *
 * @author A. Heckel (USwansea), O. Danne
 */
@OperatorMetadata(alias = "C3sAotMaster", version = "0.8",
        authors = "A. Heckel (USwansea), O. Danne",
        internal = true,
        copyright = "Copyright (C) 2010, 2018 by USwansea, Brockmann Consult",
        description = "Master operator for AOT retrieval. Parent of sensor specific operators (here i.e. OLCI, MERIS).")
public class C3sAotMasterOp extends Operator {

    @Parameter(defaultValue = "OLCI_SLSTR_NOMINAL")
    private S3OlciSlstrSensor sensor;

    @Parameter(defaultValue = "false")
    private boolean copyToaReflBands;

    @Parameter(defaultValue = "false")
    private boolean noFilling;

    @Parameter(defaultValue = "false")
    private boolean noUpscaling;

    @Parameter(defaultValue = "1")
    private int soilSpecId;

    @Parameter(defaultValue = "5")
    private int vegSpecId;

    @Parameter(defaultValue = "9")
    private int scale;

    @Parameter(defaultValue = "0.3")
    private float ndviThr;

    @Parameter(defaultValue = "true",
            label = " If set, AOT retrieval is skipped and a constant value shall be used in AC")
    private boolean useConstantAot;

    @Parameter(defaultValue = "false",
            label = "Copy cloud top pressure")
    private boolean gaCopyCTP;

    @Parameter(defaultValue = "false", label = " If set, AOT are computed everywhere " +
            "(brute force, ignores clouds etc.)")
    private boolean computeAotEverywhere;


    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    public static final Product EMPTY_PRODUCT = new Product("empty", "empty", 0, 0);

    @Override
    public void initialize() throws OperatorException {
        if (sourceProduct.getSceneRasterWidth() < 9 || sourceProduct.getSceneRasterHeight() < 9) {
            setTargetProduct(EMPTY_PRODUCT);
            return;
        }

        if (useConstantAot) {
            setTargetProduct(EMPTY_PRODUCT);
            return;
        }

        Dimension targetTS = ImageManager.getPreferredTileSize(sourceProduct);
        RenderingHints rhTarget = new RenderingHints(GPF.KEY_TILE_SIZE, targetTS);

        Product reflProduct;
        if (sensor == S3OlciSlstrSensor.OLCI_SLSTR_NOMINAL || sensor == S3OlciSlstrSensor.OLCI_SLSTR_S3A ||
                sensor == S3OlciSlstrSensor.OLCI_SLSTR_S3B) {
            C3sAotOlciSlstrOp aotOlciSlstrOp = new C3sAotOlciSlstrOp();
            aotOlciSlstrOp.setSourceProduct(sourceProduct);
            aotOlciSlstrOp.setParameterDefaultValues();
            reflProduct = aotOlciSlstrOp.getTargetProduct();
        } else {
            throw new OperatorException("Sensor '" + sensor.getName() + "' not supported.");
        }
        if (reflProduct == EMPTY_PRODUCT) {
            setTargetProduct(EMPTY_PRODUCT);
            return;
        }

        C3sAotLowresOp aotLowresOp = new C3sAotLowresOp();
        aotLowresOp.setSourceProduct(reflProduct);
        aotLowresOp.setParameterDefaultValues();
        aotLowresOp.setParameter("sensor", sensor);
        aotLowresOp.setParameter("soilSpecId", soilSpecId);
        aotLowresOp.setParameter("vegSpecId", vegSpecId);
        aotLowresOp.setParameter("scale", scale);
        aotLowresOp.setParameter("ndviThreshold", ndviThr);
        aotLowresOp.setParameter("computeAotEverywhere", computeAotEverywhere);

        Product aotDownsclProduct = aotLowresOp.getTargetProduct();

        Product fillAotProduct = aotDownsclProduct;
        if (!noFilling) {
            Map<String, Product> fillSourceProds = new HashMap<>();
            fillSourceProds.put("aotProduct", aotDownsclProduct);
            // fill of AOT gaps on low-resolution grid:
            final String gapFillingOpAlias = OperatorSpi.getOperatorAlias(C3sGapFillingOp.class);
            fillAotProduct = GPF.createProduct(gapFillingOpAlias, GPF.NO_PARAMS, fillSourceProds);
        }

        Product aotFinalProduct = fillAotProduct;
        if (!noUpscaling) {
            Map<String, Product> upsclProducts = new HashMap<>();
            upsclProducts.put("lowresProduct", fillAotProduct);
            upsclProducts.put("hiresProduct", reflProduct);
            Map<String, Object> sclParams = new HashMap<>();
            sclParams.put("sensor", S3OlciSlstrSensor.OLCI_SLSTR_S3B);
            sclParams.put("scale", scale);
            sclParams.put("computeAotEverywhere", computeAotEverywhere);

            final String aotHighresOpAlias = OperatorSpi.getOperatorAlias(C3sAotHighresOp.class);
            Product aotHiresProduct = GPF.createProduct(aotHighresOpAlias, sclParams, upsclProducts, rhTarget);

            Product mergedAotProduct = mergeToTargetProduct(reflProduct, aotHiresProduct);
            ProductUtils.copyPreferredTileSize(reflProduct, mergedAotProduct);
            aotFinalProduct = mergedAotProduct;
        }

        setTargetProduct(aotFinalProduct);
    }

    private Product mergeToTargetProduct(Product reflProduct, Product aotHiresProduct) {
        String pname = reflProduct.getName() + "_AOT";
        String ptype = reflProduct.getProductType() + " GlobAlbedo AOT";
        int rasterWidth = reflProduct.getSceneRasterWidth();
        int rasterHeight = reflProduct.getSceneRasterHeight();
        Product tarP = new Product(pname, ptype, rasterWidth, rasterHeight);
        tarP.setStartTime(reflProduct.getStartTime());
        tarP.setEndTime(reflProduct.getEndTime());
        tarP.setPointingFactory(reflProduct.getPointingFactory());
        ProductUtils.copyMetadata(aotHiresProduct, tarP);
        ProductUtils.copyTiePointGrids(reflProduct, tarP);
        copyTiePointGridsIfBands(reflProduct, tarP);
        ProductUtils.copyGeoCoding(reflProduct, tarP);
        ProductUtils.copyFlagBands(reflProduct, tarP, true);
        ProductUtils.copyFlagBands(aotHiresProduct, tarP, true);
        String sourceBandName;

        for (Band sourceBand : reflProduct.getBands()) {
            sourceBandName = sourceBand.getName();

            boolean copyBand = (copyToaReflBands && !tarP.containsBand(sourceBandName) && sourceBand.getSpectralWavelength() > 0);
            copyBand = copyBand || (sourceBandName.equals("altitude"));     // todo: check if needed!
            copyBand = copyBand || (gaCopyCTP && sourceBandName.equals("cloud_top_press"));

            if (copyBand && !tarP.containsBand(sourceBandName)) {
                ProductUtils.copyBand(sourceBandName, reflProduct, tarP, true);
            }
        }
        for (Band sourceBand : aotHiresProduct.getBands()) {
            sourceBandName = sourceBand.getName();
            if (!sourceBand.isFlagBand() && !tarP.containsBand(sourceBandName)) {
                ProductUtils.copyBand(sourceBandName, aotHiresProduct, tarP, true);
            }
        }
        return tarP;
    }

    private void copyTiePointGridsIfBands(Product reflProduct, Product tarP) {
        // i.e. if we use netcdf product as L1b input
        for (Band sourceBand : reflProduct.getBands()) {
            String sourceBandName = sourceBand.getName();
            if ((Float.isNaN(sourceBand.getSpectralWavelength()) || sourceBand.getSpectralWavelength() <= 0) &&
                    !tarP.containsBand(sourceBandName) && !tarP.containsTiePointGrid(sourceBandName)) {
                ProductUtils.copyBand(sourceBandName, reflProduct, tarP, true);
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
            super(C3sAotMasterOp.class);
        }
    }

}
