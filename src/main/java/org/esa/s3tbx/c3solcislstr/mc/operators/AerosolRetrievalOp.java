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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.c3solcislstr.mc.NormalVariate;
import org.esa.s3tbx.c3solcislstr.mc.UncertaintyModel;
import org.esa.s3tbx.c3solcislstr.mc.UncertaintyModelFactory;
import org.esa.s3tbx.c3solcislstr.mc.UniformVariateFactory;
import org.esa.s3tbx.c3solcislstr.mc.variates.MarsagliaNormalVariate;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.DateTimeUtils;
import org.esa.snap.core.util.ProductUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;

/**
 * Operator to retrieve CAMS aerosol optical depth for a given OLCI scene.
 * For use in Monte Carlo simulations.
 *
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "AerosolRetriever",
        category = "OLCI",
        version = "0.1",
        authors = "Ralf Quast",
        copyright = "(c) 2020 by Brockmann Consult",
        description = "Reads and mutates a CAMS parent product matching to a given OLCI scene. For use in Monte Carlo simulations.")
public class AerosolRetrievalOp extends Operator {

    private static final String CAMS_FORMAT = "NetCDF";
    private static final String CAMS_BASENAME = "cams-wv-ozone-sp-aerosol";
    public static final String[] CAMS_VARIABLE_NAMES = {
            "aod550", "ssaod550", "duaod550", "omaod550", "suaod550", "bcaod550", "tcwv", "gtco3"
    };

    private static final long MILLISECONDS_PER_HOUR = 60 * 60 * 1000;

    @Parameter(label = "CAMS repository",
            description = "Location of the CAMS aerosol product repository (or of a specific product file)",
            notNull = true, notEmpty = true)
    private File repository;

    @Parameter(label = "Mutant",
            description = "If checked, the aerosol product is mutated randomly.",
            defaultValue = "true")
    private boolean mutant;

    @Parameter(label = "Random number generator",
            description = "The type of random number generator",
            defaultValue = "MELG", valueSet = {"MELG", "PCG"})
    private String rngType;

    @Parameter(label = "Seed number",
            description = "A numeric value to seed the random number generator",
            defaultValue = "5489")
    private long seedNumber;

    @Parameter(label = "Seed string",
            description = "An alphanumeric value to seed the random number generator (US-ASCII character set). If empty, the seed value is determined by the date and time associated with the CAMS parent product.")
    private String seedString;

    @Parameter(label = "Positive definite",
            description = "If checked, the aerosol optical depth is considered positive definite.",
            defaultValue = "true")
    private boolean positiveDefinite;

    @Parameter(label = "Least positive value",
            description = "Tiny number, used if an aerosol optical depth is zero (e.g., due to discretization) even though it is considered positive definite.",
            defaultValue = "1.0E-10")
    private double tiny;

    @Parameter(label = "Regression coefficient",
            description = "The regression coefficient (see score summary statistics https://aerocom.met.no/cgi-bin/surfobs_annualrs.pl)",
            defaultValue = "1.0")
    private double regressionCoefficient;

    @Parameter(label = "Regression constant",
            description = "The regression constant (see score summary statistics https://aerocom.met.no/cgi-bin/surfobs_annualrs.pl)",
            defaultValue = "0.0")
    private double regressionConstant;

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

    @Parameter(label = "Uncertainty model",
            description = "The type of uncertainty model",
            defaultValue = "CAMS AOD (2020)",
            valueSet = {"CAMS AOD (2020)", "Relative (10%)", "Relative (15%)", "Relative (20%)"})
    private String uncertaintyModelType;

    @SourceProduct(label = "Source product", description = "The source product. Only the time information on the product is used.")
    private Product sourceProduct;

    @TargetProduct(label = "Target product", description = "The target product")
    private Product targetProduct;

    private Product parentProduct;
    private UncertaintyModel uncertaintyModel;
    private Cube random;

    @Override
    public void initialize() throws OperatorException {
        final Calendar calendar = getCalendar(sourceProduct);
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH);
        final int day = calendar.get(Calendar.DAY_OF_MONTH);
        try {
            parentProduct = createCamsProduct(getCamsFile(year, month), day);
            final int w = parentProduct.getSceneRasterWidth();
            final int h = parentProduct.getSceneRasterHeight();
            targetProduct = new Product(parentProduct.getName(), parentProduct.getProductType(), w, h);
            ProductUtils.copyMetadata(parentProduct, targetProduct);
            targetProduct.setStartTime(parentProduct.getStartTime());
            targetProduct.setEndTime(parentProduct.getEndTime());
            for (final Band sourceBand : parentProduct.getBands()) {
                final Band targetBand = targetProduct.addBand(sourceBand.getName(), ProductData.TYPE_FLOAT32);
                targetBand.setDescription(sourceBand.getDescription());
                targetBand.setUnit(sourceBand.getUnit());
            }
            ProductUtils.copyGeoCoding(parentProduct, targetProduct);
        } catch (IOException | ParseException e) {
            throw new OperatorException(e);
        }
    }

    private static Calendar getCalendar(Product p) {
        return new ProductData.UTC(getMJD(p)).getAsCalendar();
    }

    private static double getMJD(Product p) {
        return 0.5 * (p.getStartTime().getMJD() + p.getEndTime().getMJD());
    }

    private File getCamsFile(int year, int month) {
        if (repository.isDirectory()) {
            return new File(repository, createCamsFilename(year, month));
        } else {
            return repository;
        }
    }

    private static Product createCamsProduct(File file, int day) throws IOException, ParseException {
        final Product product = createCamsProduct(file, singleDaySubsetDef(day));
        // Strip time suffix from band names
        for (final String name : product.getBandNames()) {
            product.getBand(name).setName(name.substring(0, name.indexOf("_")));
        }
        // Add time information
        final long epoch1900 = DateTimeUtils.stringToUTC("1900-01-01 00:00:00.0").getTime();
        final long millisSince1970 = epoch1900 + millisSince1900(product, day);
        final ProductData.UTC time = ProductData.UTC.create(new Date(millisSince1970), 0);
        product.setStartTime(time);
        product.setEndTime(time);

        return product;
    }

    private static Product createCamsProduct(File file, ProductSubsetDef def) throws IOException {
        final ProductReader reader = ProductIO.getProductReader(CAMS_FORMAT);
        if (reader == null) {
            throw new OperatorException("No reader found for format '" + CAMS_FORMAT + "'.");
        }
        final Product parent = reader.readProductNodes(file, def);
        final boolean owner = true;
        return ProductSubsetBuilder.createProductSubset(parent, owner, def, null, null);
    }

    @NotNull
    private static ProductSubsetDef singleDaySubsetDef(int day) {
        final ProductSubsetDef def = new ProductSubsetDef();
        for (final String name : CAMS_VARIABLE_NAMES) {
            def.addNodeName(String.format("%s_time%d", name, day));
        }
        return def;
    }

    private static long millisSince1900(Product product, int day) {
        final MetadataElement variableAttributes = product.getMetadataRoot().getElement("Variable_Attributes");
        final MetadataElement timeVariable = variableAttributes.getElement("time");
        final ProductData timeData = timeVariable.getElement("Values").getAttribute("data").getData();

        return timeData.getElemLongAt(day - 1) * MILLISECONDS_PER_HOUR;
    }

    @Override
    public void doExecute(ProgressMonitor pm) {
        if (mutant) {
            try {
                initializeRandomNumbers();
            } catch (Exception e) {
                throw new OperatorException("Random noise could not be initialized.", e);
            }
            initializeUncertaintyModel();
        }
    }

    private void initializeRandomNumbers() {
        try {
            final int h = targetProduct.getSceneRasterHeight();
            final int w = targetProduct.getSceneRasterWidth();
            final int n = 1;  // only the total aerosol optical depth will be mutated
            final long[] seeds = {seedNumber, anotherSeedNumber(seedString, seedNumber)};
            final NormalVariate normal = new MarsagliaNormalVariate(new UniformVariateFactory(rngType).newUniformVariate(seeds));
            if (!useConstantBias) {
                bias = normal.nextDouble();
            } else {
                normal.nextDouble();  // to preserve consistency
            }
            final double[] doubles = normal.nextDoubles(new double[h * w * n]);
            final CorrelatorFactory correlatorFactory = new CorrelatorFactory(errorCorrelationType);
            random = correlatorFactory.newCorrelator(new DefaultCube(h, w, n, doubles), bias, errorCorrelationCoefficient);
        } catch (Exception e) {
            throw new OperatorException("Random numbers could not be initialized.", e);
        }
    }

    private long anotherSeedNumber(String seedString, long seedNumber) {
        if (seedString != null) {
            for (byte b : seedString.getBytes(StandardCharsets.US_ASCII)) {
                seedNumber = 31 * seedNumber + Byte.toUnsignedLong(b);
            }
        }
        if (parentProduct.getStartTime() != null) {
            seedNumber = 31 * seedNumber + Double.doubleToLongBits(parentProduct.getStartTime().getMJD());
        }
        return seedNumber;
    }

    private void initializeUncertaintyModel() {
        uncertaintyModel = new UncertaintyModelFactory(uncertaintyModelType).newUncertaintyModel();
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        final List<Tile> tileList = new ArrayList<>();
        final List<ProductData> dataList = new ArrayList<>();

        for (final String name : CAMS_VARIABLE_NAMES) {
            final Band parentBand = parentProduct.getBand(name);
            final Band targetBand = targetProduct.getBand(name);
            final Tile tile = targetTiles.get(targetBand);
            final ProductData data = tile.getRawSamples();
            readData(parentBand, targetRectangle, data, pm);
            tileList.add(tile);
            dataList.add(data);
        }
        if (mutant) {  // mutate aerosol optical depths
            final int aerosolSpeciesCount = 5;
            for (int y = targetRectangle.y, p = 0; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++, p++) {
                    final double[] parentValues = new double[aerosolSpeciesCount + 1];
                    final double[] targetValues = new double[aerosolSpeciesCount + 1];

                    // get original aerosol optical depth values
                    for (int i = 0; i < aerosolSpeciesCount + 1; i++) {
                        parentValues[i] = getSampleValue(tileList.get(i).getRasterDataNode(), dataList.get(i), p);
                    }
                    // mutate total aerosol optical depth value
                    final double u = uncertaintyModel.getUncertainty(parentValues[0]);
                    final double z = random.get(x, y, 0);
                    targetValues[0] = getMutatedValue(correctedValue(parentValues[0]), u, z);
                    // mutate (i.e. scale) specific aerosol optical depth values correspondingly
                    for (int i = 1; i < aerosolSpeciesCount + 1; i++) {
                        targetValues[i] = parentValues[i] * (targetValues[0] / parentValues[0]);
                    }
                    // set mutated aerosol optical depth values
                    for (int i = 0; i < aerosolSpeciesCount + 1; i++) {
                        setSampleValue(tileList.get(i).getRasterDataNode(), dataList.get(i), p, targetValues[i]);
                    }
                }
            }
        }
        for (int i = 0; i < CAMS_VARIABLE_NAMES.length; i++) {
            tileList.get(i).setRawSamples(dataList.get(i));
        }
    }

    private double correctedValue(double x) {
        return regressionCoefficient * x + regressionConstant;
    }

    private static void readData(Band band, Rectangle rectangle, ProductData targetData, ProgressMonitor pm) {
        final ProductData sourceData = ProductData.createInstance(band.getDataType(), targetData.getNumElems());
        try {
            band.getProductReader().readBandRasterData(band, rectangle.x, rectangle.y, rectangle.width, rectangle.height, sourceData, pm);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
        for (int i = 0; i < targetData.getNumElems(); i++) {
            final double sourceValue = sourceData.getElemDoubleAt(i);
            if (band.isNoDataValueUsed() && !(band.getNoDataValue() != sourceValue)) {
                targetData.setElemDoubleAt(i, Double.NaN);
            } else {
                targetData.setElemDoubleAt(i, band.scale(sourceValue));
            }
        }
    }

    private double getMutatedValue(double x, double u, double z) {
        if (positiveDefinite) {
            return getMutatedValueLognormal(Math.max(x, tiny), u, z);
        }
        return getMutatedValueNormal(x, u, z);
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

    private static double getSampleValue(RasterDataNode node, ProductData data, int p) {
        final double value = data.getElemDoubleAt(p);
        if (node.isNoDataValueUsed() && !(value != node.getNoDataValue())) {
            return Double.NaN;
        }
        return node.scale(value);
    }

    private static void setSampleValue(RasterDataNode node, ProductData data, int p, double value) {
        if (Double.isNaN(value) && node.isNoDataValueUsed()) {
            data.setElemDoubleAt(p, node.getNoDataValue());
        } else {
            data.setElemDoubleAt(p, node.scaleInverse(value));
        }
    }

    @Override
    public void dispose() {
        random = null;
        try {
            parentProduct.closeIO();
        } catch (IOException ignored) {
        }
        super.dispose();
    }

    @NotNull
    private static String createCamsFilename(int year, int month) {
        return String.format("%s-%s-%s.nc", AerosolRetrievalOp.CAMS_BASENAME, toYear(year), toMonth(month));
    }

    private static String toMonth(int month) {
        switch (month) {
            case Calendar.JANUARY:
                return "01";
            case Calendar.FEBRUARY:
                return "02";
            case Calendar.MARCH:
                return "03";
            case Calendar.APRIL:
                return "04";
            case Calendar.MAY:
                return "05";
            case Calendar.JUNE:
                return "06";
            case Calendar.JULY:
                return "07";
            case Calendar.AUGUST:
                return "08";
            case Calendar.SEPTEMBER:
                return "09";
            case Calendar.OCTOBER:
                return "10";
            case Calendar.NOVEMBER:
                return "11";
            case Calendar.DECEMBER:
                return "12";
        }
        throw new OperatorException(MessageFormat.format("Invalid month ''{0}''.", month));
    }

    private static String toYear(int year) {
        if (year < 100) {
            year = year + 2000;
        }
        return Integer.toString(year);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AerosolRetrievalOp.class);
        }
    }

}
