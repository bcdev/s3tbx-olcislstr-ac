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
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.RectangleExtender;
import org.esa.snap.idepix.core.IdepixConstants;
import org.esa.snap.idepix.core.util.IdepixUtils;

import java.awt.*;

/**
 * Operator to compute  cloud shadow and cloud buffer from the OLCI IdePix cloud mask.
 * For use in Monte carlo simulations.
 *
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "CloudShadowComputer",
        category = "OLCI",
        version = "0.1",
        authors = "Ralf Quast",
        copyright = "(c) 2021 by Brockmann Consult",
        description = "Computes cloud shadow from the OLCI IdePix cloud mask. For use in Monte Carlo simulations.")
public class CloudShadowOp extends Operator {

    @Parameter(defaultValue = "true",
            label = "Compute cloud buffer",
            description = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0, 100]",
            description = "The width (pixels) of the 'safety buffer' around a pixel, which is classified as cloudy.",
            label = "Width of the cloud buffer")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "true",
            label = " Compute cloud shadow",
            description = " Compute a cloud shadow.")
    private boolean computeCloudShadow;

    @Parameter(label = "Clone all data",
            description = "If checked, all data are copied from source to target, if not conflicting.",
            defaultValue = "true")
    private boolean cloneAll;

    @SourceProduct(label = "Cloud mask product", description = "The cloud mask product")
    private Product sourceProduct;

    @TargetProduct(description = "The target product")
    private Product targetProduct;

    private RectangleExtender extender;
    private RasterDataNode szaNode;
    private RasterDataNode saaNode;
    private RasterDataNode ozaNode;
    private RasterDataNode oaaNode;
    private RasterDataNode slpNode;
    private RasterDataNode altNode;
    private RasterDataNode[] pressureLevelNodes;
    private RasterDataNode ctpNode;

    private Band classifBand;

    @Override
    public void initialize() throws OperatorException {
        final int w = sourceProduct.getSceneRasterWidth();
        final int h = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), w, h);

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        classifBand = sourceProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME);
        final Band targetBand = targetProduct.addBand(classifBand.getName(), classifBand.getDataType());
        ProductUtils.copyRasterDataNodeProperties(classifBand, targetBand);

        if (cloneAll) {
            copyBands(band -> !targetProduct.containsBand(band.getName()));
        }
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        copyMasks();
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());

        if (computeCloudShadow) {
            classifBand = getSourceProduct().getBand(IdepixConstants.CLASSIF_BAND_NAME);

            szaNode = sourceProduct.getRasterDataNode("SZA");
            saaNode = sourceProduct.getRasterDataNode("SAA");
            ozaNode = sourceProduct.getRasterDataNode("OZA");
            oaaNode = sourceProduct.getRasterDataNode("OAA");
            slpNode = sourceProduct.getRasterDataNode("sea_level_pressure");
            altNode = sourceProduct.getRasterDataNode("altitude");
            pressureLevelNodes = getPressureLevelNodes(sourceProduct);
            ctpNode = sourceProduct.getRasterDataNode("ctp");
            if (ctpNode == null) {
                throw new OperatorException("Cloud top pressure data not found.");
            }

            final int extendedWidth;
            final int extendedHeight;
            if (sourceProduct.getName().contains("FR____")) {  // todo: check these values
                extendedWidth = 64;
                extendedHeight = 64;
            } else {
                extendedWidth = 16;
                extendedHeight = 16;
            }
            extender = new RectangleExtender(new Rectangle(w, h), extendedWidth, extendedHeight);
        } else {
            //noinspection SuspiciousNameCombination
            extender = new RectangleExtender(new Rectangle(w, h), cloudBufferWidth, cloudBufferWidth);
        }
    }

    private void copyBands(ProductNodeFilter<Band> filter) {
        for (Band band : sourceProduct.getBands()) {
            if (filter.accept(band)) {
                ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
            }
        }
    }

    private void copyMasks() {
        ProductUtils.copyMasks(sourceProduct, targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (!computeCloudShadow && !computeCloudShadow) {
            return;
        }

        final Rectangle targetRectangle = targetTile.getRectangle();
        final Rectangle sourceRectangle = extender.extend(targetRectangle);
        final Tile classifTile = getSourceTile(classifBand, sourceRectangle);
        for (int y = sourceRectangle.y; y < sourceRectangle.y + sourceRectangle.height; y++) {
            checkForCancellation();
            for (int x = sourceRectangle.x; x < sourceRectangle.x + sourceRectangle.width; x++) {
                if (targetRectangle.contains(x, y)) {
                    targetTile.setSample(x, y, classifTile.getSampleInt(x, y));
                    targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_BUFFER, false);
                    targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SHADOW, false);
                }
            }
        }
        if (computeCloudBuffer) {
            CloudBuffer.setCloudBuffer(targetTile, sourceRectangle, classifTile, cloudBufferWidth);
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    IdepixUtils.consolidateCloudAndBuffer(targetTile, x, y);
                }
            }
        }
        if (computeCloudShadow) {
            final Tile sza = getSourceTile(szaNode, sourceRectangle);
            final Tile saa = getSourceTile(saaNode, sourceRectangle);
            final Tile oza = getSourceTile(ozaNode, sourceRectangle);
            final Tile oaa = getSourceTile(oaaNode, sourceRectangle);
            final Tile ctp = getSourceTile(ctpNode, sourceRectangle);
            final Tile slp = getSourceTile(slpNode, sourceRectangle);
            final Tile alt = getSourceTile(altNode, targetRectangle);
            final Tile[] pressureLevelTiles = getSourceTiles(pressureLevelNodes, sourceRectangle);
            final GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
            final CloudShadowAlgorithm algorithm =
                    new OlciCloudShadowAlgorithm(geoCoding, sza, saa, oza, oaa, ctp, slp, pressureLevelTiles, alt);
            algorithm.computeCloudShadow(classifTile, targetTile);
        }
    }

    private RasterDataNode[] getPressureLevelNodes(Product product) {
        final PressureLevelDescriptor descriptor = OlciPressureLevelDescriptor.INSTANCE;
        final RasterDataNode[] nodes = new RasterDataNode[descriptor.getLevelCount()];
        for (int i = 0; i < descriptor.getLevelCount(); i++) {
            nodes[i] = product.getRasterDataNode(descriptor.getLevelName(i));
        }
        return nodes;
    }

    private Tile[] getSourceTiles(RasterDataNode[] nodes, Rectangle sourceRectangle) {
        final Tile[] tiles = new Tile[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            tiles[i] = getSourceTile(nodes[i], sourceRectangle);
        }
        return tiles;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CloudShadowOp.class);
        }
    }

}
