package org.esa.s3tbx.c3solcislstr.ac;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
public class OlciAcUtilsTest {

    @Test
    public void testGetIndexBefore() {
        float[] values = {1.8f, 2.2f, 4.5f, 5.5f};
        assertEquals(0, OlciSlstrAcUtils.getIndexBefore(1.2f, values));
        assertEquals(1, OlciSlstrAcUtils.getIndexBefore(2.5f, values));
        assertEquals(2, OlciSlstrAcUtils.getIndexBefore(4.6f, values));
        assertEquals(2, OlciSlstrAcUtils.getIndexBefore(7.7f, values));
    }

    @Test
    public void testGetDoyFromYYYYMMDD() {
        String yyyymmdd = "20070101";
        int doy = OlciSlstrAcUtils.getDoyFromYYYYMMDD(yyyymmdd);
        assertEquals(1, doy);

        yyyymmdd = "20071218";
        doy = OlciSlstrAcUtils.getDoyFromYYYYMMDD(yyyymmdd);
        assertEquals(352, doy);
    }

}
