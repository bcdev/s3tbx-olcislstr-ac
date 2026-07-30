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

import org.esa.s3tbx.c3solcislstr.mc.RandomVariate;
import org.esa.s3tbx.c3solcislstr.mc.UncertaintyModel;
import org.esa.s3tbx.c3solcislstr.mc.UncertaintyModelFactory;
import org.esa.s3tbx.c3solcislstr.mc.UniformVariateFactory;
import org.esa.s3tbx.c3solcislstr.mc.variates.MarsagliaNormalVariate;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.core.util.ProductUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

import static org.esa.s3tbx.c3solcislstr.ac.OlciSlstrAcConstants.SLSTR_TOA_RAD_BAND_NAMES;
import static org.esa.s3tbx.c3solcislstr.ac.OlciSlstrAcConstants.SLSTR_TOA_RAD_BAND_UNCERTAINTIES;

/**
 * Operator to add Gaussian noise to measurements of spectral radiance (or brightness
 * temperature or reflectance). For use in Monte Carlo simulations.
 *
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "ToaL1bMutator",
        category = "OLCI",
        version = "0.1",
        authors = "Ralf Quast",
        copyright = "(c) 2020 by Brockmann Consult",
        description = "Adds Gaussian noise to measurement values (i.e. spectral radiance). For use in Monte Carlo simulations.")
public class ToaL1bMutationOp extends PixelOperator {

    public static final double MU_SYS = 0.3;
    public static final double SIGMA_SYS = 0.15;
    public static final double MU_RAND = 0.1;
    public static final double SIGMA_RAND = 0.05;

    @Parameter(label = "Random number generator",
            description = "The type of random number generator",
            defaultValue = "MELG", valueSet = {"MELG", "PCG"})
    private String rngType;

    @Parameter(label = "Seed number",
            description = "A numeric value to seed the random number generator",
            defaultValue = "5489")
    private long seedNumber;

    @Parameter(label = "Seed string",
            description = "An alphanumeric value to seed the random number generator (US-ASCII character set). If empty, the seed value is determined by the date and time associated with the source product.")
    private String seedString;

    @Parameter(label = "Positive definite",
            description = "If checked, the measurand is considered positive definite (like, e.g., radiance, reflectance or brightness temperature).",
            defaultValue = "true")
    private boolean positiveDefinite;

    @Parameter(label = "Error codec",
            description = "The type of error codec used",
            defaultValue = "Linear", valueSet = {"Laplace", "Linear", "Normal"})
    private String errorCodecType;

    @Parameter(label = "Error correlation",
            description = "The type of error correlation",
            defaultValue = "None", valueSet = {"None", "Constant"})
    private String errorCorrelationType;

    @Parameter(label = "Error correlation coefficient",
            description = "The error correlation coefficient (used to generate a sequence of correlated random numbers).",
            defaultValue = "0.0", interval = "[0.0, 1.0]")
    private double errorCorrelationCoefficient;

    @Parameter(label = "Use constant bias",
            description = "If checked, all random numbers are correlated with a constant bias rather than a random bias (using the specified error correlation coefficient).",
            defaultValue = "false")
    private boolean useConstantBias;

    @Parameter(label = "Constant bias",
            description = "A constant bias value, which must be a draw from a standard normal distribution.",
            defaultValue = "0.0")
    private double bias;

    @Parameter(label = "Mutate geolcation",
            description = "If checked, geolocation is mutated as well).",
            defaultValue = "true")
    private boolean mutateGeolocation;

    @Parameter(label = "Measurands",
            description = "The measured quantities", notNull = true, notEmpty = true,
            rasterDataNodeType = Band.class,
            converter = NameConverter.class)
    private String[] measurandNames;

    @Parameter(label = "Measurand uncertainties",
            description = "The uncertainties associated with the measured quantities",
            rasterDataNodeType = Band.class,
            converter = NameConverter.class)
    private String[] measurandUncertaintyNames;

    @Parameter(label = "Use an uncertainty model",
            description = "Facilitates the use of a custom uncertainty model.",
            defaultValue = "true")
    private boolean useUncertaintyModel;

    @Parameter(label = "Uncertainty model",
            description = "The type of uncertainty model",
            defaultValue = "Poisson", valueSet = {"Poisson", "Relative"})
    private String uncertaintyModelType;

    @Parameter(label = "Uncertainty model coefficients",
            description = "Specifies the path to the uncertainty model coefficient file (if empty, an internal coefficient set is used).")
    private File uncertaintyModelCoefficientFile;

    @Parameter(label = "Clone all ancillary data",
            description = "If checked, all ancillary data are copied from source to target, if not conflicting.",
            defaultValue = "true")
    private boolean cloneAllAncillary;

    @Parameter(label = "Activate test mode",
            description = "If checked, mere random noise is put out (for testing only).",
            defaultValue = "false")
    private boolean activateTestMode;

    @Parameter(label = "Activate debug mode",
            description = "If checked, more output is written.",
            defaultValue = "false")
    private boolean debug;

    @SourceProduct(label = "Source product", description = "The source product")
    private Product sourceProduct;

    private UncertaintyModel uncertaintyModel;
    private double[][] coefficients;
    private Cube random;
    private Random randGeolocation;

    @Override
    protected void prepareInputs() throws OperatorException {
        try {
            initializeRandomNumbers();
        } catch (Exception e) {
            throw new OperatorException("Random noise could not be initialized.", e);
        }
        if (useUncertaintyModel) {
            initializeUncertaintyModel();
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer c) {
        c.getTargetProduct().setProductType(c.getSourceProduct().getProductType());
//        c.copyMetadata();
        c.copyTimeCoding();
        c.copyTiePointGrids();
        for (String name : measurandNames) {
            final Band sourceBand = c.getSourceProduct().getBand(name);
            final Band targetBand = c.addBand(sourceBand.getName(), sourceBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        }
        if (!useUncertaintyModel)
            if (measurandUncertaintyNames == null || measurandUncertaintyNames.length != measurandNames.length) {
                throw new OperatorException("The number of measurand uncertainty bands does not correspond to the number of measurands.");
            }
        if (cloneAllAncillary) {
            c.copyBands(band -> !c.getTargetProduct().containsBand(band.getName()) && !band.getName().contains("radiance"));
        }

        if (debug) {
            c.addBand("lat_mc", c.getSourceProduct().getBand("latitude").getDataType());
            c.addBand("lon_mc", c.getSourceProduct().getBand("longitude").getDataType());
            c.addBand("lat_mc_pixerr", c.getSourceProduct().getBand("longitude").getDataType());
            c.addBand("lon_mc_pixerr", c.getSourceProduct().getBand("longitude").getDataType());
        }

        c.copyGeoCoding();
        c.copyMasks();
        c.copyMetadata();
        c.getTargetProduct().setAutoGrouping(c.getSourceProduct().getAutoGrouping());
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final double[] z = random.spectrum(x, y);

        if (useUncertaintyModel) {
            for (int i = 0; i < measurandNames.length; i++) {
                final double measurement = getSampleValue(sourceSamples[i], x, y);
                final double uncertainty = uncertaintyModel.getUncertainty(measurement, coefficients[i]);
                setSampleValue(targetSamples[i], getMutatedValue(measurement, uncertainty, z[i]));
            }
        } else {
            for (int i = 0, j = 0; i < measurandNames.length; i++) {
                final double measurement = getSampleValue(sourceSamples[j++], x, y);
                double uncertainty;
                if (measurandNames.length == SLSTR_TOA_RAD_BAND_NAMES.length) {
                    // SLSTR case
                    uncertainty = SLSTR_TOA_RAD_BAND_UNCERTAINTIES[i];
                } else {
                    // OLCI case
                    uncertainty = getSampleValue(sourceSamples[j++], x, y);
                }
                setSampleValue(targetSamples[i], getMutatedValue(measurement, uncertainty, z[i]));
            }
        }

        // randomize geolocation...
        if (mutateGeolocation) {
            final double[] latLonRd = randomizeGeolocation(x, y);
            final GeoCoding geoCoding = getTargetProduct().getSceneGeoCoding();
            geoCoding.getGeoPos(new PixelPos(x, y), null).setLocation(latLonRd[0], latLonRd[1]);
            if (debug) {
                setSampleValue(targetSamples[measurandNames.length], latLonRd[0]);
                setSampleValue(targetSamples[measurandNames.length + 1], latLonRd[1]);
                setSampleValue(targetSamples[measurandNames.length + 2], latLonRd[2]);
                setSampleValue(targetSamples[measurandNames.length + 3], latLonRd[3]);
            }
        }

    }

    private double[] randomizeGeolocation(double x, double y) {
        final PixelPos pixelPosCenter = new PixelPos();
        final PixelPos pixelPosLeft = new PixelPos();
        final PixelPos pixelPosRight = new PixelPos();
        final PixelPos pixelPosLower = new PixelPos();
        final PixelPos pixelPosUpper = new PixelPos();

        pixelPosCenter.setLocation(x, y);
        pixelPosLeft.setLocation(Math.max(0, x - 1), y);
        pixelPosRight.setLocation(Math.min(sourceProduct.getSceneRasterWidth() - 1, x + 1), y);
        pixelPosUpper.setLocation(x, Math.max(0, y - 1));
        pixelPosLower.setLocation(x, Math.min(sourceProduct.getSceneRasterHeight() - 1, y + 1));

        final GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
        final GeoPos geoPos = new GeoPos();

        final double latCenter = geoCoding.getGeoPos(pixelPosCenter, geoPos).lat;
        final double lonCenter = geoCoding.getGeoPos(pixelPosCenter, geoPos).lon;
        final double latLeft = geoCoding.getGeoPos(pixelPosLeft, geoPos).lat;
        final double lonLeft = geoCoding.getGeoPos(pixelPosLeft, geoPos).lon;
        final double latRight = geoCoding.getGeoPos(pixelPosRight, geoPos).lat;
        final double lonRight = geoCoding.getGeoPos(pixelPosRight, geoPos).lon;
        final double latUpper = geoCoding.getGeoPos(pixelPosUpper, geoPos).lat;
        final double lonUpper = geoCoding.getGeoPos(pixelPosUpper, geoPos).lon;
        final double latLower = geoCoding.getGeoPos(pixelPosLower, geoPos).lat;
        final double lonLower = geoCoding.getGeoPos(pixelPosLower, geoPos).lon;

        final double latRdPixel = calcLatLonError();  // Gaussian distributed around +0.3 or -0.3 with sigma = 0.1
        final double lonRdPixel = calcLatLonError();  // Gaussian distributed around +0.3 or -0.3 with sigma = 0.1

        final double xShiftLat = 0.5 * latRdPixel * (latRight - latLeft);
        final double yshiftlat = 0.5 * latRdPixel * (latUpper - latLower);
        final double shiftLat = 0.5 * (xShiftLat + yshiftlat);

        final double xShiftLon = 0.5 * lonRdPixel * (lonRight - lonLeft);
        final double yshiftlon = 0.5 * lonRdPixel * (lonUpper - lonLower);
        final double shiftLon = 0.5 * (xShiftLon + yshiftlon);

        final double latRandomized = latCenter + shiftLat;
        final double lonRandomized = lonCenter + shiftLon;

        return new double[]{latRandomized, lonRandomized, latRdPixel, lonRdPixel};
    }

    double calcLatLonError() {
        final double nextGaussianSys = randGeolocation.nextGaussian(MU_SYS, SIGMA_SYS);
        double sysLatLonErr = Math.max(1.E-6, nextGaussianSys);
        if (sysLatLonErr >= 1.0) sysLatLonErr = 0.099;
        int nextInt = randGeolocation.nextInt(2);  // gives int in [0, 2), i.e. 0 or 1
        final int sgnSys = nextInt == 0 ? 1 : -1;

        final double nextGaussianRand = randGeolocation.nextGaussian(MU_RAND, SIGMA_RAND);
        double rdLatLonErr = Math.max(1.E-6, nextGaussianRand);
        if (rdLatLonErr >= 1.0) rdLatLonErr = 0.099;
        nextInt = randGeolocation.nextInt(2);  // gives int in [0, 2), i.e. 0 or 1
        final int sgnRd = nextInt == 0 ? 1 : -1;

        return sysLatLonErr * sgnSys + rdLatLonErr * sgnRd;
    }

    private double getMutatedValue(double x, double u, double z) {
        if (positiveDefinite && x > 0) {
            return getMutatedValueLognormal(x, u, z);
        }
        return getMutatedValueNormal(x, u, z);
    }

    private double getMutatedValueLognormal(double x, double u, double z) {
        final double v = Math.log(1.0 + square(u / x));
        final double e = Math.log(x) - 0.5 * v;
        return Math.exp(getMutatedValueNormal(e, Math.sqrt(v), z));
    }

    private double getMutatedValueNormal(double x, double u, double z) {
        if (activateTestMode) {
            return z;
        }
        return x + u * z;
    }

    private static double square(double x) {
        return x == 0.0 ? 0.0 : x * x;
    }

    private static double getSampleValue(Sample sample, int x, int y) {
        if (sample.getNode().isPixelValid(x, y)) {
            return sample.getDouble();
        }
        return Double.NaN;
    }

    private static void setSampleValue(WritableSample sample, double value) {
        if (Double.isNaN(value) && sample.getNode().isNoDataValueUsed()) {
            sample.set(sample.getNode().getGeophysicalNoDataValue());
        } else {
            sample.set(value);
        }
    }

    private void initializeRandomNumbers() {
        try {
            final int h = sourceProduct.getSceneRasterHeight();
            final int w = sourceProduct.getSceneRasterWidth();
            final int n = measurandNames.length;
            final long[] seeds = {seedNumber, anotherSeedNumber(seedString, seedNumber)};
            final RandomVariate normal = new MarsagliaNormalVariate(new UniformVariateFactory(rngType).newUniformVariate(seeds));
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
            random = correlatorFactory.newCorrelator(new EncodedCube(h, w, n, encoded, codec), bias, errorCorrelationCoefficient);
        } catch (Exception e) {
            throw new OperatorException("Random numbers could not be initialized.", e);
        }

        randGeolocation = new Random(anotherSeedNumber(seedString, seedNumber));
    }

    private long anotherSeedNumber(String seedString, long seedNumber) {
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

    private void initializeUncertaintyModel() {
        uncertaintyModel = new UncertaintyModelFactory(uncertaintyModelType).newUncertaintyModel();
        final int coefficientCount = uncertaintyModel.getCoefficientCount();
        coefficients = new double[measurandNames.length][coefficientCount];

        if (uncertaintyModel.getCoefficientCount() > 0) {
            final InputStream is;
            if (uncertaintyModelCoefficientFile == null) {
                is = ToaL1bMutationOp.class.getResourceAsStream("olci_radiometry_uncertainty_model_coefficients.dat");
            } else try {
                is = new FileInputStream(uncertaintyModelCoefficientFile);
            } catch (FileNotFoundException e) {
                throw new OperatorException("File not found.", e);
            }
            try (final Scanner scanner = new Scanner(is)) {
                for (int i = 0; i < measurandNames.length; i++) {
                    for (int j = 0; j < coefficientCount; j++) {
                        coefficients[i][j] = scanner.nextDouble();
                    }
                }
            } catch (InputMismatchException e) {
                throw new OperatorException("Element does not comply with the required format.", e);
            } catch (NoSuchElementException e) {
                throw new OperatorException("Requested element does not exist.", e);
            }
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer c) throws OperatorException {
        if (useUncertaintyModel) {
            for (int i = 0; i < measurandNames.length; i++) {
                c.defineSample(i, measurandNames[i]);
            }
        } else {
            for (int i = 0, j = 0; i < measurandNames.length; i++) {
                c.defineSample(j++, measurandNames[i]);
                c.defineSample(j++, measurandUncertaintyNames[i]);
            }
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer c) throws OperatorException {
        int j = 0;
        for (String name : measurandNames) {
            c.defineSample(j++, name);
        }

        if (debug) {
            c.defineSample(j++, "lat_mc");
            c.defineSample(j++, "lon_mc");
            c.defineSample(j++, "lat_mc_pixerr");
            c.defineSample(j, "lon_mc_pixerr");
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        coefficients = null;
        random = null;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ToaL1bMutationOp.class);
        }
    }
}
