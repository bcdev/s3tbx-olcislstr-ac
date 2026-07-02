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

public class MutantPreparator {

    public static Cube initializeRandomNumbers(Product sourceProduct,
                                               String[] measurandNames, long seedNumber, String seedString,
                                               String rngType, boolean useConstantBias, double bias,
                                               String errorCodecType, String errorCorrelationType,
                                               double errorCorrelationCoefficient) {
        try {
            final int h = sourceProduct.getSceneRasterHeight();
            final int w = sourceProduct.getSceneRasterWidth();
            final int n = measurandNames.length;

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

    public static UncertaintyModel initializeUncertaintyModel(String uncertaintyModelType) {
        return new UncertaintyModelFactory(uncertaintyModelType).newUncertaintyModel();
    }

    public static double[][] initializeUncertaintyModelCoefficients(UncertaintyModel uncertaintyModel,
                                                                    String[] measurandNames,
                                                                    File uncertaintyModelCoefficientFile) {
        final int coefficientCount = uncertaintyModel.getCoefficientCount();
        double[][] coefficients = new double[measurandNames.length][coefficientCount];

        if (uncertaintyModel.getCoefficientCount() > 0) {
            final InputStream is;
            if (uncertaintyModelCoefficientFile == null) {
                is = SdrMutationOp.class.getResourceAsStream("olci_radiometry_uncertainty_model_coefficients.dat");
            } else {
                try {
                    is = new FileInputStream(uncertaintyModelCoefficientFile);
                } catch (FileNotFoundException e) {
                    throw new OperatorException("File not found.", e);
                }
            }
            try (final Scanner scanner = new Scanner(is)) {
                for (int i = 0; i < measurandNames.length; i++) {
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
        } else {
            throw new OperatorException("Cannot initialize coefficients for uncertainty model. Please check.");
        }

    }

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
