package org.esa.s3tbx.c3solcislstr.ac.aot;

import org.esa.s3tbx.c3solcislstr.ac.OlciSlstrAcConstants;
import org.esa.s3tbx.c3solcislstr.ac.aot.util.AerosolUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Operator for C3S OLCI/SLSTR AOT retrieval.
 *
 * @author A. Heckel (USwansea), O. Danne
 */
@OperatorMetadata(alias = "AotOlciSlstsr", version = "0.8",
        authors = "A. Heckel (USwansea), O. Danne",
        internal = true,
        copyright = "Copyright (C) 2010, 2018 by USwansea, Brockmann Consult",
        description = "Operator for OLCI/SLSTR AOT retrieval.")
public class AotOlciSlstrOp extends Operator {

    @Parameter(defaultValue = "false", description = "Compute a cloud shadow")
    private boolean computeCloudShadow;

    @Parameter(defaultValue = "false", description = "Compute a cloud buffer")
    private boolean computeCloudBuffer;


    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;


    private static final String ALTITUDE_BAND_NAME = "altitude";
    private static final String ELEVATION_BAND_NAME = "elevation";

    @Override
    public void initialize() throws OperatorException {
        final boolean needElevation = !sourceProduct.containsBand("altitude");
        final boolean needSurfacePres = !sourceProduct.containsBand("surface_pressure_tx");

        //general SzaSubset to less 70 degree
        // todo: check performance and possibly get rid of this in a future version!
        Product szaSubProduct;
        Rectangle szaRegion = AerosolUtils.getSzaRegion(sourceProduct.getRasterDataNode("SZA"), false, 69.99);
        if (szaRegion.x == 0 && szaRegion.y == 0 &&
                szaRegion.width == sourceProduct.getSceneRasterWidth() &&
                szaRegion.height == sourceProduct.getSceneRasterHeight()) {
            szaSubProduct = sourceProduct;
        } else if (szaRegion.width < 2 || szaRegion.height < 2) {
            targetProduct = AotMasterOp.EMPTY_PRODUCT;
            return;
        } else {
            Map<String, Object> subsetParam = new HashMap<>(3);
            subsetParam.put("region", szaRegion);
            Dimension targetTS = ImageManager.getPreferredTileSize(sourceProduct);
            RenderingHints rhTarget = new RenderingHints(GPF.KEY_TILE_SIZE, targetTS);
            szaSubProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SubsetOp.class), subsetParam, sourceProduct, rhTarget);
            ProductUtils.copyMetadata(sourceProduct, szaSubProduct);
        }


        // subset might have set ptype to null, thus:
        if (szaSubProduct.getDescription() == null) {
            szaSubProduct.setDescription("C3S SYN OLCI SLSTR Reflectance product");
        }

        // setup target product primarily as copy of sourceProduct
        final int rasterWidth = szaSubProduct.getSceneRasterWidth();
        final int rasterHeight = szaSubProduct.getSceneRasterHeight();
        targetProduct = new Product(szaSubProduct.getName(),
                szaSubProduct.getProductType(),
                rasterWidth, rasterHeight);
        targetProduct.setStartTime(szaSubProduct.getStartTime());
        targetProduct.setEndTime(szaSubProduct.getEndTime());
        targetProduct.setPointingFactory(szaSubProduct.getPointingFactory());
        ProductUtils.copyTiePointGrids(szaSubProduct, targetProduct);
        ProductUtils.copyFlagBands(szaSubProduct, targetProduct, true);
        ProductUtils.copyGeoCoding(szaSubProduct, targetProduct);

        // create elevation product if band is missing in sourceProduct
        Product elevProduct = null;
        if (needElevation && !szaSubProduct.containsBand(ALTITUDE_BAND_NAME)) {
            elevProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(CreateElevationBandOp.class), GPF.NO_PARAMS, szaSubProduct);
        }

        // create surface pressure estimate product if band is missing in sourceProduct
        VirtualBand surfPresBand = null;
        if (needSurfacePres) {
            String presExpr = "(1013.25 * exp(-elevation/8400))";
            surfPresBand = new VirtualBand("surfPressEstimate",
                    ProductData.TYPE_FLOAT32,
                    rasterWidth, rasterHeight, presExpr);
            surfPresBand.setDescription("estimated sea level pressure (p0=1013.25hPa, hScale=8.4km)");
            surfPresBand.setNoDataValue(0);
            surfPresBand.setNoDataValueUsed(true);
            surfPresBand.setUnit("hPa");
        }

        // copy all non-radiance bands from sourceProduct and
        // copy reflectance bands from reflProduct
        for (Band srcBand : szaSubProduct.getBands()) {
            String srcName = srcBand.getName();
            if (!srcBand.isFlagBand()) {
                if (srcName.endsWith("reflectance")) {
                    // so far, only copy MERIS heritage bands for further use
                    if (isMerisHeritage(srcBand)) {
                        ProductUtils.copyBand(srcName, szaSubProduct, targetProduct, true);
                    }
                } else if (!srcName.startsWith("lambda") &&
                        !srcName.startsWith("frame") &&
                        !srcName.startsWith("FWHM") &&
                        !srcName.startsWith("solar_flux")) {
                    if (!targetProduct.containsBand(srcName)) {
                        ProductUtils.copyBand(srcName, szaSubProduct, targetProduct, true);
                    }
                }
            }
        }

        // add elevation band if needed
        if (needElevation) {
            if (elevProduct != null) {
                ProductUtils.copyBand(ELEVATION_BAND_NAME, elevProduct, targetProduct, true);
            } else if (szaSubProduct.containsBand(ALTITUDE_BAND_NAME)) {
                ProductUtils.copyBand(ALTITUDE_BAND_NAME, szaSubProduct, ELEVATION_BAND_NAME, targetProduct, true);
            }
        }

        // add vitrual surface pressure band if needed
        if (needSurfacePres) {
            targetProduct.addBand(surfPresBand);
        }
        ProductUtils.copyPreferredTileSize(szaSubProduct, targetProduct);

    }

    private boolean isMerisHeritage(Band srcBand) {
        String srcName = srcBand.getName();
        for (String i : OlciSlstrAcConstants.NOT_MERIS_HERITAGE) {
            if (srcName.equals(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AotOlciSlstrOp.class);
        }
    }
}
