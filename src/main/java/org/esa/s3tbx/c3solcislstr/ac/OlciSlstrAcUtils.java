package org.esa.s3tbx.c3solcislstr.ac;


import Jama.Matrix;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.gpf.pointop.WritableSample;

import java.util.Calendar;

/**
 * Utility class for BBDR retrieval
 *
 * @author Olaf Danne
 */
public class OlciSlstrAcUtils {

    public static int getIndexBefore(float value, float[] array) {
        for (int i = 0; i < array.length; i++) {
            if (value < array[i]) {
                if (i != 0) {
                    return i - 1;
                } else {
                    return i;
                }
            }
        }
        return (array.length - 2);
    }

    public static int getDoyFromYYYYMMDD(String yyyymmdd) {
        Calendar cal = Calendar.getInstance();
        int doy = -1;
        try {
            final int year = Integer.parseInt(yyyymmdd.substring(0, 4));
            final int month = Integer.parseInt(yyyymmdd.substring(4, 6)) - 1;
            final int day = Integer.parseInt(yyyymmdd.substring(6, 8));
            cal.set(year, month, day);
            doy = cal.get(Calendar.DAY_OF_YEAR);
        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            e.printStackTrace();
        }
        return doy;
    }

    public static void fillTargetSampleWithNoDataValue(WritableSample[] targetSamples) {
        for (WritableSample targetSample : targetSamples) {
            if (targetSample.getIndex() != -1) {
                targetSample.set(Float.NaN);
            }
        }
    }


    public static Matrix matrixSquare(double[] doubles) {
        Matrix matrix = new Matrix(doubles.length, doubles.length);
        for (int i = 0; i < doubles.length; i++) {
            for (int j = 0; j < doubles.length; j++) {
                matrix.set(i, j, doubles[i] * doubles[j]);
            }
        }
        return matrix;
    }

    public static Band createBooleanExpressionBand(String expression, Product sourceProduct) {
        BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
        bandDescriptor.name = "band1";
        bandDescriptor.expression = expression;
        bandDescriptor.type = ProductData.TYPESTRING_INT8;

        BandMathsOp bandMathsOp = new BandMathsOp();
        bandMathsOp.setParameterDefaultValues();
        bandMathsOp.setSourceProduct(sourceProduct);
        bandMathsOp.setTargetBandDescriptors(bandDescriptor);
        return bandMathsOp.getTargetProduct().getBandAt(0);
    }


}
