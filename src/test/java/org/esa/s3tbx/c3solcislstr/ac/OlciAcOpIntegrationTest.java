package org.esa.s3tbx.c3solcislstr.ac;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;


public class OlciAcOpIntegrationTest {

    @Test
    @Ignore // todo make test work
    public void testOlciAcOpIntegration() throws IOException {
        String olci_product_name = "S3A_OL_1_ERR____20190522T082035_20190522T090500_20190523T132735_2665_045_064______LN1_O_NT_002.dim";
        String olciProductPath = getClass().getResource(olci_product_name).getPath();
        Product olciProduct = ProductIO.readProduct(olciProductPath);
        Map<String, Object> parametersMap = new HashMap<>();
        String pathToLut = getClass().getResource("resources/org/esa/s3tbx/c3solcislstr/ac/ac/lut/S3B_OLCI_lut_glob_c3s_v2.1.nc").getPath();
        parametersMap.put("pathToLut", pathToLut);
        parametersMap.put("computeCloudShadow", false);
        Map<String, Product> sourceProductMap = new HashMap<>();
        sourceProductMap.put("sourceProduct", olciProduct);
        Product acProduct = GPF.createProduct("OlciSlstrAc", parametersMap, sourceProductMap);

        assertNotNull(acProduct);
    }

}
