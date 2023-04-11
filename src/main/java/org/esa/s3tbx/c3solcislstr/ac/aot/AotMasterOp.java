package org.esa.s3tbx.c3solcislstr.ac.aot;

import org.esa.s3tbx.c3solcislstr.ac.Sensor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
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

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

/**
 * Master operator for AOT retrieval. Parent of sensor specific operators (here i.e. OLCI, MERIS).
 *
 * @author A. Heckel (USwansea), O. Danne
 */
@OperatorMetadata(alias = "AotMaster", version = "0.8",
        authors = "A. Heckel (USwansea), O. Danne",
        internal = true,
        copyright = "Copyright (C) 2010, 2018 by USwansea, Brockmann Consult",
        description = "Master operator for AOT retrieval. Parent of sensor specific operators (here i.e. OLCI, MERIS).")
public class AotMasterOp extends Operator {

    @Parameter(defaultValue = "OLCI_SLSTR_NOMINAL")
    private Sensor sensor;

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


    @Parameter(defaultValue = "false",
            label = " If set, AOT retrieval is skipped and a constant value shall be used in AC")
    private boolean useConstantAot;

    @Parameter(defaultValue = "0.15",
            label = " Constant AOT value",
            description = "Constant AOT value which is used if the retrieval is skipped")
    private float constantAotValue;

    @Parameter(defaultValue = "true",
            label = " Compute cloud shadow")
    private boolean computeCloudShadow;

    @Parameter(defaultValue = "true",
            label = "Compute cloud buffer")
    private boolean computeCloudBuffer;


    @Parameter(defaultValue = "false",
            label = "Copy cloud top pressure")
    private boolean gaCopyCTP;

    @Parameter(defaultValue = "false", label = " If set, AOT are computed everywhere (brute force, ignores clouds etc.)")
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
        Dimension targetTS = ImageManager.getPreferredTileSize(sourceProduct);
        RenderingHints rhTarget = new RenderingHints(GPF.KEY_TILE_SIZE, targetTS);

        Product reflProduct;
        if (sensor == Sensor.OLCI_SLSTR_NOMINAL || sensor == Sensor.OLCI_SLSTR_S3A || sensor == Sensor.OLCI_SLSTR_S3B) {
            AotOlciSlstrOp aotOlciSlstrOp = new AotOlciSlstrOp();
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

        if (useConstantAot) {
            Band aotBand = reflProduct.addBand("aot", ProductData.TYPE_FLOAT32);
            Band aotErrBand = reflProduct.addBand("aot_err", ProductData.TYPE_FLOAT32);

            ParameterBlockJAI pb = new ParameterBlockJAI("constant");
            pb.setParameter("width", (float) sourceProduct.getSceneRasterWidth());
            pb.setParameter("height", (float) sourceProduct.getSceneRasterHeight());

            pb.setParameter("bandvalues", new Float[]{constantAotValue});
            RenderedOp constImageAot = JAI.create("constant", pb);
            aotBand.setSourceImage(constImageAot);

            pb.setParameter("bandvalues", new Float[]{0.0f});
            RenderedOp constImageAotErr = JAI.create("constant", pb);
            aotErrBand.setSourceImage(constImageAotErr);

            setTargetProduct(reflProduct);
            return;
        }

        AotLowresOp aotLowresOp = new AotLowresOp();
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
            Map<String, Product> fillSourceProds = new HashMap<>(2);
            fillSourceProds.put("aotProduct", aotDownsclProduct);
            // fill of AOT gaps on low-resolution grid:
            fillAotProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(GapFillingOp.class), GPF.NO_PARAMS, fillSourceProds);
        }

        targetProduct = fillAotProduct;
        if (!noUpscaling) {
            Map<String, Product> upsclProducts = new HashMap<>(2);
            upsclProducts.put("lowresProduct", fillAotProduct);
            upsclProducts.put("hiresProduct", reflProduct);
            Map<String, Object> sclParams = new HashMap<>(1);
            sclParams.put("sensor", sensor);
            sclParams.put("scale", scale);
            sclParams.put("computeAotEverywhere", computeAotEverywhere);
            Product aotHiresProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AotHighresOp.class), sclParams, upsclProducts, rhTarget);

            targetProduct = mergeToTargetProduct(reflProduct, aotHiresProduct);
            ProductUtils.copyPreferredTileSize(reflProduct, targetProduct);
        }
        setTargetProduct(targetProduct);
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
            super(AotMasterOp.class);
        }
    }

}
