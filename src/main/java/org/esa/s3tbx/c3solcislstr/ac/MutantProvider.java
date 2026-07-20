package org.esa.s3tbx.c3solcislstr.ac;

import org.esa.s3tbx.c3solcislstr.mc.RandomVariate;
import org.esa.s3tbx.c3solcislstr.mc.UncertaintyModel;
import org.esa.s3tbx.c3solcislstr.mc.UncertaintyModelFactory;
import org.esa.s3tbx.c3solcislstr.mc.UniformVariateFactory;
import org.esa.s3tbx.c3solcislstr.mc.operators.*;
import org.esa.s3tbx.c3solcislstr.mc.variates.MarsagliaNormalVariate;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Provides preparation steps and utility methods for MC mutations.
 *
 * @author olafd
 */
public class MutantProvider {

    /**
     * Provides a {@link Cube} of random numbers.
     *
     * @param sourceProduct:
     * @param measurandNamesLength:
     * @param seedNumber:
     * @param seedString:
     * @param rngType:
     * @param useConstantBias:
     * @param bias:
     * @param errorCodecType:
     * @param errorCorrelationType:
     * @param errorCorrelationCoefficient:
     *
     * @return {@link Cube}
     */
    public static Cube initializeRandomNumbers(Product sourceProduct,
                                               int measurandNamesLength, long seedNumber, String seedString,
                                               String rngType, boolean useConstantBias, double bias,
                                               String errorCodecType, String errorCorrelationType,
                                               double errorCorrelationCoefficient) {
        try {
            final int h = sourceProduct.getSceneRasterHeight();
            final int w = sourceProduct.getSceneRasterWidth();
            final int n = measurandNamesLength;

            final long[] seeds = {seedNumber, anotherSeedNumber(sourceProduct, seedString, seedNumber)};
            final RandomVariate normal =
                    new MarsagliaNormalVariate(new UniformVariateFactory(rngType).newUniformVariate(seeds));

            if (!useConstantBias) {
                bias = normal.nextDouble();
            } else {
                normal.nextDouble();  // to preserve consistency
            }

            final byte[] encoded = new byte[h * w * n];
            final ErrorCodec codec = new ErrorCodecFactory(errorCodecType).newErrorCodec();
            for (int i = 0; i < encoded.length; i++) {
                encoded[i] = codec.encode(normal.nextDouble());
            }
            final CorrelatorFactory correlatorFactory = new CorrelatorFactory(errorCorrelationType);
            return correlatorFactory.newCorrelator(new EncodedCube(h, w, n, encoded, codec), bias, errorCorrelationCoefficient);
        } catch (Exception e) {
            throw new OperatorException("Random numbers could not be initialized.", e);
        }
    }

    /**
     * Provides an {@link UncertaintyModel} to use for MC mutants.
     *
     * @param uncertaintyModelType typy of the {@link UncertaintyModel} ("Poisson" or "Relative")
     *
     * @return {@link UncertaintyModel}
     */
    public static UncertaintyModel initializeUncertaintyModel(String uncertaintyModelType) {
        return new UncertaintyModelFactory(uncertaintyModelType).newUncertaintyModel();
    }

    /**
     * Initializes coefficients for an uncertainty model.
     *
     * @param uncertaintyModel The {@link UncertaintyModel} used.
     * @param measurandNamesLength length of array of measurands to modify
     * @param uncertaintyModelCoefficientFile file with uncertainty model coefficients
     *
     * @return double[][]
     */
    public static double[][] initializeUncertaintyModelCoefficients(UncertaintyModel uncertaintyModel,
                                                                    int measurandNamesLength,
                                                                    File uncertaintyModelCoefficientFile) {
        final int coefficientCount = uncertaintyModel.getCoefficientCount();
        double[][] coefficients = new double[measurandNamesLength][coefficientCount];

        if (uncertaintyModel.getCoefficientCount() > 0) {
            final InputStream is;
            if (uncertaintyModelCoefficientFile == null) {
                is = ToaL1bMutationOp.class.getResourceAsStream("olci_radiometry_uncertainty_model_coefficients.dat");
            } else {
                try {
                    is = new FileInputStream(uncertaintyModelCoefficientFile);
                } catch (FileNotFoundException e) {
                    throw new OperatorException("File not found.", e);
                }
            }
            try (final Scanner scanner = new Scanner(is)) {
                for (int i = 0; i < measurandNamesLength; i++) {
                    for (int j = 0; j < coefficientCount; j++) {
                        coefficients[i][j] = scanner.nextDouble();
                    }
                }
                return coefficients;
            } catch (InputMismatchException e) {
                throw new OperatorException("Element does not comply with the required format.", e);
            } catch (NoSuchElementException e) {
                throw new OperatorException("Requested element does not exist.", e);
            }
        }

        return coefficients;
//        else {
//            throw new OperatorException("Cannot initialize coefficients for uncertainty model. Please check.");
//        }

    }

    /**
     * Provides a mutated input value.
     *
     * @param x input value
     * @param u uncertainty value provided by the model
     * @param z Random numbers taken from the {@link Cube} associated with the {@link UncertaintyModel}.
     * @param positiveDefinite if true, return value is positive definite
     *
     * @return the mutated value
     */
    public static double getMutatedValue(double x, double u, double z, boolean positiveDefinite) {
        if (positiveDefinite) {
            return getMutatedValueLognormal(x, u, z);
        }
        return getMutatedValueNormal(x, u, z);
    }

    private static long anotherSeedNumber(Product sourceProduct, String seedString, long seedNumber) {
        if (seedString != null) {
            for (final byte b : seedString.getBytes(StandardCharsets.US_ASCII)) {
                seedNumber = 31 * seedNumber + Byte.toUnsignedLong(b);
            }
        }
        if (sourceProduct.getStartTime() != null) {
            seedNumber = 31 * seedNumber + Double.doubleToLongBits(sourceProduct.getStartTime().getMJD());
        }
        return seedNumber;
    }

    private static double getMutatedValueLognormal(double x, double u, double z) {
        final double v = Math.log(1.0 + square(u / x));
        final double e = Math.log(x) - 0.5 * v;
        return Math.exp(getMutatedValueNormal(e, Math.sqrt(v), z));
    }

    private static double getMutatedValueNormal(double x, double u, double z) {
        return x + u * z;
    }

    private static double square(double x) {
        return x == 0.0 ? 0.0 : x * x;
    }

}
