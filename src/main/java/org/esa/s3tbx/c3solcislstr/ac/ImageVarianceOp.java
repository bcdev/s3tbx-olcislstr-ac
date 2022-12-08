/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.c3solcislstr.ac;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.BorderExtender;
import java.awt.*;

/**
 * This function calculates the local-neighbourhood statistical variance.
 * I.e. for each array element a the variance of the neighbourhood of +-
 * halfwidth is calculated. The routine avoids any loops and so is fast
 * and "should" work for any dimension of array.
 *
 * @author marcoz
 */
public class ImageVarianceOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        Product sourceProduct = getSourceProduct();
        Product targetProduct = new Product(getId(),
                                            getClass().getName(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);

        Band[] bands = sourceProduct.getBands();
        for (Band band : bands) {
            if (band.getName().contains("reflectance")) {
                targetProduct.addBand(band.getName(), ProductData.TYPE_FLOAT32);
            }
        }
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        RasterDataNode sourceRaster = sourceProduct.getRasterDataNode(targetBand.getName());
        Rectangle rectangle = targetTile.getRectangle();
        rectangle.grow(1, 1);
        Tile sourceTile = getSourceTile(sourceRaster, rectangle, BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
            for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                targetTile.setSample(x, y, variance(sourceTile, x, y));
            }
        }
    }

    private double variance(Tile sourceTile, int x0, int y0) {
        double sum = 0;
        double sumSq = 0;
        for (int y = y0 - 1; y <= y0 + 1; y++) {
            for (int x = x0 - 1; x <= x0 + 1; x++) {
                double v = sourceTile.getSampleDouble(x, y) * 1.0;
                sum += v;
                sumSq += v * v;
            }
        }
        return Math.sqrt((sumSq / 9) - (sum * sum / 81));
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ImageVarianceOp.class);
        }
    }
}
