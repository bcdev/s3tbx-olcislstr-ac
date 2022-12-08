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
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.io.File;
import java.util.logging.Logger;

/**
 * GPF operator for OLCI and MERIS AC.
 *
 * @author Olaf Danne, Marco Peters
 */
@OperatorMetadata(alias = "OlciSlstrAc", version = "0.81",
        authors = "O.Danne, M.Peters",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018 by Brockmann Consult",
        description = "Performs atmospheric correction on OLCI or MERIS L1b product.\n" +
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

    @Parameter(label = "Path to an OLCI LUT from which atmospheric parameters can be derived. Required for S3AOLCI SDR.",
    defaultValue = "D:/grit/Workspaces/IdeaProjects/snap/s3tbx/s3tbx-c3solcislstr-ac/luts_backup/ac/ac/lut/SENTINEL3_1_OLCI_lut_glob_c3s_v2.nc")
    private File pathToLutOlciS3a;

    @Parameter(label = "Path to an OLCI LUT from which atmospheric parameters can be derived. Required for S3B OLCI SDR.",
            defaultValue = "D:/grit/Workspaces/IdeaProjects/snap/s3tbx/s3tbx-c3solcislstr-ac/luts_backup/ac/ac/lut/SENTINEL3_2_OLCI_lut_glob_c3s_v2.nc")
    private File pathToLutOlciS3b;

    @Parameter(label = "Path to an OLCI LUT from which atmospheric parameters can be derived. Required for S3A SLSTR SDR.",
            defaultValue = "D:/grit/Workspaces/IdeaProjects/snap/s3tbx/s3tbx-c3solcislstr-ac/luts_backup/ac/ac/lut/SENTINEL3_1_SLSTR_lut_glob_c3s_v2.nc")
    private File pathToLutSlstrS3a;

    @Parameter(label = "Path to an OLCI LUT from which atmospheric parameters can be derived. Required for S3B SLSTR SDR.",
            defaultValue = "D:/grit/Workspaces/IdeaProjects/snap/s3tbx/s3tbx-c3solcislstr-ac/luts_backup/ac/ac/lut/SENTINEL3_2_SLSTR_lut_glob_c3s_v2.nc")
    private File pathToLutSlstrS3b;



    @SourceProduct(description = "C3S SYN OLCI SLSTR product",
            label = "C3S SYN OLCI SLSTR L1b product")
    private Product sourceProduct;

//    @SourceProduct(description = "Atmospheric auxiliary product", optional = true,
//            label = "Atmospheric auxiliary product")
//    private Product atmAuxProduct;

    @TargetProduct
    private Product targetProduct;

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
            if (aotProduct.containsBand(AotConsts.aotFlags.name)){
                ProductUtils.copyBand(AotConsts.aotFlags.name, aotProduct, getTargetProduct(),true);
            }

        }

        if (copyGeometryBands) {
            for (String geomBandNameOlci : sensor.getGeomBandNamesOlci()) {
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
            for (String geomBandNameSlstr : sensor.getGeomBandNamesSlstrNadir()) {
                RasterDataNode rasterDataNodeSlstr = sourceProduct.getBand(geomBandNameSlstr);
                if (rasterDataNodeSlstr != null) {
                    ProductUtils.copyBand(geomBandNameSlstr, sourceProduct, getTargetProduct(), true);
                } else {
                    rasterDataNodeSlstr = sourceProduct.getTiePointGrid(geomBandNameSlstr);
                    if (rasterDataNodeSlstr != null) {
                        ProductUtils.copyTiePointGrid(geomBandNameSlstr, sourceProduct, getTargetProduct());
                    }
                }
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
        }    else {
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
                sdrOp.setParameter("pathToLutOlci", pathToLutOlciS3a);
                sdrOp.setParameter("pathToLutSlstr", pathToLutSlstrS3a);
                break;
            case OLCI_SLSTR_S3B:
                sdrOp.setParameter("pathToLutOlci", pathToLutOlciS3b);
                sdrOp.setParameter("pathToLutSlstr", pathToLutSlstrS3b);
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
