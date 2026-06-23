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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.idepix.olci.IdepixOlciOp;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Operator to identify pixels. Wraps OLCI IdePix and ensures that longitude, latitude and altitude
 * bands are copied from source to target. For use in Monte Carlo simulations.
 *
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "PixelIdentifier",
        category = "OLCI",
        version = "0.1",
        authors = "Ralf Quast",
        copyright = "(c) 2020 by Brockmann Consult",
        description = "Identifies pixels (i.e. clouds, snow, ice, land, water). For use in Monte Carlo simulations.")
public class PixelIdentificationOp extends Operator {

    @Parameter(label = "Clone all ancillary data",
            description = "If checked, all ancillary data are copied from source to target, if not conflicting.",
            defaultValue = "true")
    private boolean cloneAllAncillary;

    @SourceProduct(description = "The source product")
    private Product sourceProduct;

    @TargetProduct(description = "The target product")
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        targetProduct = identifyPixels(sourceProduct);

        if (cloneAllAncillary) {
            copyMissingAncillaryGrids(sourceProduct, targetProduct);
            copyMissingAncillaryBands(sourceProduct, targetProduct);
        }

        ProductUtils.copyMasks(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(String.format("%s:Oa*_reflectance", sourceProduct.getAutoGrouping().toString()));
    }

    private Product identifyPixels(Product product) {
        return GPF.createProduct(getName(IdepixOlciOp.class), pixelIdentificationParameterMap(), product);
    }

    @NotNull
    private Map<String, Object> pixelIdentificationParameterMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("reflBandsToCopy", AtmosphericCorrectionOp.TOA_RFL_BAND_NAMES);
        map.put("outputSchillerNNValue", true);
        map.put("computeMountainShadow", true);
        map.put("computeCloudShadow", true);
        map.put("outputCtp", true);
        map.put("computeCloudBuffer", false);
        map.put("useSrtmLandWaterMask", true);
        return map;
    }

    private void copyMissingAncillaryBands(Product sourceProduct, Product targetProduct) {
        for (Band band : sourceProduct.getBands()) {
            final boolean ancillaryBand = band.getSpectralBandIndex() == -1;
            if (ancillaryBand) {
                final boolean missing = !targetProduct.containsBand(band.getName());
                if (missing) {
                    ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
                }
            }
        }
    }

    private void copyMissingAncillaryGrids(Product sourceProduct, Product targetProduct) {
        for (TiePointGrid grid : sourceProduct.getTiePointGrids()) {
            final boolean missing = !targetProduct.containsTiePointGrid(grid.getName());
            if (missing) {
                ProductUtils.copyTiePointGrid(grid.getName(), sourceProduct, targetProduct);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static String getName(Class<? extends Operator> operatorClass) {
        return OperatorSpi.getOperatorAlias(operatorClass);  // returns an alias or simple class name
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PixelIdentificationOp.class);
        }
    }

}
