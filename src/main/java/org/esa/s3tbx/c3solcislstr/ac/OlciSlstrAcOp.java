package org.esa.s3tbx.c3solcislstr.ac;

import org.esa.s3tbx.c3solcislstr.ac.aot.AotConsts;
import org.esa.s3tbx.c3solcislstr.ac.aot.AotMasterOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.io.File;
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


    @SourceProduct(description = "C3S SYN OLCI SLSTR product",
            label = "C3S SYN OLCI SLSTR L1b product")
    private Product sourceProduct;

    private Sensor sensor;

    @Override
    public void initialize() throws OperatorException {
        sensor = determineSensor(sourceProduct);
        Product aotProduct;
        aotProduct = processAot(sourceProduct);
        if (aotProduct == AotMasterOp.EMPTY_PRODUCT) {
            Logger.getLogger(getClass().getName()).warning("aotProduct is empty");
            setTargetProduct(AotMasterOp.EMPTY_PRODUCT);
            return;
        }

        if (aotOnly) {
            setTargetProduct(aotProduct);
        } else {
            setTargetProduct(processSdr(sourceProduct, aotProduct));
        }

        if (copyAotBands && !aotOnly) {
            ProductUtils.copyBand(OlciSlstrAcConstants.AOT_BAND_NAME, aotProduct, getTargetProduct(), true);
            ProductUtils.copyBand(OlciSlstrAcConstants.AOT_ERR_BAND_NAME, aotProduct, getTargetProduct(), true);
            if (aotProduct.containsBand(AotConsts.aotFlags.name)) {
                ProductUtils.copyBand(AotConsts.aotFlags.name, aotProduct, getTargetProduct(), true);
            }

        }

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
        if (rasterDataNodeOlci != null) {
            ProductUtils.copyBand(geomBandNameOlci, sourceProduct, getTargetProduct(), true);
        } else {
            rasterDataNodeOlci = sourceProduct.getTiePointGrid(geomBandNameOlci);
            if (rasterDataNodeOlci != null) {
                ProductUtils.copyTiePointGrid(geomBandNameOlci, sourceProduct, getTargetProduct());
            }
        }
    }


    private Sensor determineSensor(Product l1bProduct) {
        if (l1bProduct.getName().contains("SY_1_")) {
            if (l1bProduct.getName().startsWith("S3A_SY_1_SYN")) {
                return Sensor.OLCI_SLSTR_S3A;
            } else {
                return Sensor.OLCI_SLSTR_S3B;
            }
        } else {
            throw new OperatorException(String.format("Product of type '%s' not supported.",
                                                      l1bProduct.getProductType()));
        }
    }

    private Product processAot(Product productSourceAot) {
        AotMasterOp aotMasterOp = new AotMasterOp();
        aotMasterOp.setParameterDefaultValues();
        aotMasterOp.setParameter("sensor", sensor);
        aotMasterOp.setParameter("useConstantAot", false);
        aotMasterOp.setParameter("constantAotValue", 0.15f);
        aotMasterOp.setParameter("computeAotEverywhere", computeAotEverywhere);
        aotMasterOp.setSourceProduct(productSourceAot);

        return aotMasterOp.getTargetProduct();
    }


    private Product processSdr(Product sourceProduct, Product aotProduct) {
        SdrOlciSlstrOp sdrOp;
        switch (sensor) {
            case OLCI_SLSTR_S3A:
            case OLCI_SLSTR_S3B:
                sdrOp = new SdrOlciSlstrOp();
                break;
            default:
                throw new OperatorException("Sensor '" + sensor.getName() + "' not supported.");
        }
        sdrOp.setParameterDefaultValues();
        sdrOp.setSourceProduct("sourceProduct", sourceProduct);
        sdrOp.setSourceProduct("aotProduct", aotProduct);
        sdrOp.setParameter("sensor", sensor);
        sdrOp.setParameter("computeSdrEverywhere", computeSdrEverywhere);
        sdrOp.setParameter("writeSdrUncertaintyBands", writeSdrUncertaintyBands);
        switch (sensor) {
            case OLCI_SLSTR_S3A:
                final String olciALutName =
                        pathToAtmosphericParameterLuts + File.separator + S3_A_OLCI_ATM_PARAMS_LUT_NAME;
                sdrOp.setParameter("pathToLutOlci", olciALutName);
                final String slstrALutName =
                        pathToAtmosphericParameterLuts + File.separator + S3_A_SLSTR_ATM_PARAMS_LUT_NAME;
                sdrOp.setParameter("pathToLutSlstr", slstrALutName);
                break;
            case OLCI_SLSTR_S3B:
                final String olciBLutName =
                        pathToAtmosphericParameterLuts + File.separator + S3_B_OLCI_ATM_PARAMS_LUT_NAME;
                sdrOp.setParameter("pathToLutOlci", olciBLutName);
                final String slstrBLutName =
                        pathToAtmosphericParameterLuts + File.separator + S3_B_SLSTR_ATM_PARAMS_LUT_NAME;
                sdrOp.setParameter("pathToLutSlstr", slstrBLutName);
                break;
            default:
                throw new OperatorException("Sensor '" + sensor.getName() + "' not supported.");
        }

        return sdrOp.getTargetProduct();
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciSlstrAcOp.class);
        }
    }
}
