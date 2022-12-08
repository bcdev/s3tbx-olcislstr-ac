package org.esa.s3tbx.c3solcislstr.ac.auxdata;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.math.LookupTable;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author Olaf Danne, Tonio Fincke
 */
class S3LutAccessor {

    private File lutFile;

    private int[][] minMaxIndices;

    private Properties properties;
    private final static String lutshape_name = "lutshape";
    private final static String dimnames_name = "dimnames";
    private final String[] intPropertyNames = {lutshape_name};

    private static final String[] modelTypes = {"MidLatitudeSummer"};
    private static final String[] aerosolTypes = {"___rural", "maritime", "___urban", "__desert"};

    private static final Map<String, String[]> STRING_PROPERTY_MAP;

    public final static String SUN_ZENITH = "sun_zenith_angle";
    public final static String VIEW_ZENITH = "view_zenith_angle";
    public final static String ALTITUDE = "altitude";
    public final static String AEROSOL_DEPTH = "aerosol_depth";
    private static final String[] DIM_NAMES =
            new String[]{"water_vapour",
                    AEROSOL_DEPTH,
                    SUN_ZENITH,
                    VIEW_ZENITH,
                    "relative_azimuth",
                    ALTITUDE,
                    "aerosol_type",
                    "model_type",
                    "ozone_content",
                    "co2_mixing_ratio",
                    "wavelengths"};

    static {
        STRING_PROPERTY_MAP = new HashMap<>();
        STRING_PROPERTY_MAP.put("model_type", modelTypes);
        STRING_PROPERTY_MAP.put("aerosol_type", aerosolTypes);
    }


    S3LutAccessor(File inputFile) throws IOException {

        boolean lutExists = false;
        properties = null;
        if (inputFile.exists()) {
            File lutDescriptionFile = null;
            if (inputFile.getPath().endsWith("memmap.d")) {
                final String lutDescriptionPath = inputFile.getAbsolutePath().replace("memmap.d", "dims.jsn");
                lutDescriptionFile = new File(lutDescriptionPath);
                this.lutFile = inputFile;
            } else if (inputFile.getPath().endsWith("dims.jsn")) {
                final String lutFilePath = inputFile.getAbsolutePath().replace("dims.jsn", "memmap.d");
                lutDescriptionFile = inputFile;
                this.lutFile = new File(lutFilePath);
            }
            if (lutDescriptionFile != null && lutDescriptionFile.exists()) {
                properties = S3LutUtils.readPropertiesFromJsonFile(lutDescriptionFile, intPropertyNames);
                lutExists = true;
            }
        }
        if (!lutExists) {
            throw new IOException("Could not read LUT description file");
        }

        try {
            validate();
        } catch (ValidationException e) {
            Logger.getLogger(getClass().getName()).warning(e.getMessage());
        }
        initIndices();
    }

    LookupTable readLut(ProgressMonitor progressMonitor) throws IOException {
        // See sentinel-3a_lut_smsi_v0.6.dims.jsn. We have:
//        "water_vapour": [500, 1000, 1500, 2000, 3000, 4000, 5000],
//        "aerosol_depth": [0.05, 0.075, 0.1, 0.125, 0.15, 0.175, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2],
//        "sun_zenith_angle": [0, 10, 20, 30, 40, 50, 60, 70],
//        "view_zenith_angle": [0, 10, 20, 30, 40, 50, 60],
//        "relative_azimuth": [0, 30, 60, 90, 120, 150, 180],
//        "altitude": [0.0, 0.5, 1.0, 2.0, 3.0, 4.0],
//        "aerosol_type": ["___rural", "maritime", "___urban", "__desert"],
//        "model_type": ["MidLatitudeSummer"],
//        "ozone_content": [0.33176],
//        "co2_mixing_ratio": [380],
//        "wavelengths": [0.443, 0.49, 0.56, 0.665, 0.705, 0.74, 0.783, 0.842, 0.865, 0.945, 1.375, 1.61, 2.19],

        final float[] wvp = getDimValues(DIM_NAMES[0]);
        final float[] ad = getDimValues(DIM_NAMES[1]);
        final float[] sza = getDimValues(DIM_NAMES[2]);
        final float[] vza = getDimValues(DIM_NAMES[3]);
        final float[] ra = getDimValues(DIM_NAMES[4]);
        final float[] alt = getDimValues(DIM_NAMES[5]);
        final float[] at = getDimValues(DIM_NAMES[6]);
        final float[] wvl = getDimValues(DIM_NAMES[10]);

        float[] params = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f};
        final int nParams = params.length;

        float[] lutArray = new float[nParams * getLutLength()];

        int numLutParts = wvp.length + ad.length;
        int lutPartLength = lutArray.length / numLutParts;
        progressMonitor.beginTask("Reading Look-Up-Table", numLutParts);
        ImageInputStream iis = openLUTStream();
        for (int i = 0; i < numLutParts; i++) {
            iis.readFully(lutArray, i * lutPartLength, lutPartLength);
            progressMonitor.worked(1);
        }
        progressMonitor.done();
        iis.close();

        return new LookupTable(lutArray, wvp, ad, sza, vza, ra, alt, at, wvl, params);
    }

    private int getNumberOfNonSpectralProperties() {
        final String[] dimnames = (String[]) properties.get(dimnames_name);
        return dimnames.length - 1;
    }

    private String[] getTargetNames() {
        final String[] dimNames = (String[]) properties.get(dimnames_name);
        String[] targetNames = new String[dimNames.length - 1];
        System.arraycopy(dimNames, 0, targetNames, 0, targetNames.length);
        return targetNames;
    }

    public float[] getDimValues(String dimName) {
        final Object property = properties.get(dimName);
        if (property instanceof float[]) {
            return (float[]) property;
        }
        if (property instanceof String[]) {
            float[] indices = new float[STRING_PROPERTY_MAP.get(dimName).length];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = (float) i;
            }
            return indices;
        }
        throw new IllegalArgumentException("Cannot find values for dimension " + dimName);
    }

    private int[] getLUTShapes() {
        return ((int[]) properties.get(lutshape_name));
    }

    private void validate() throws ValidationException {
        final int[] lutShapes = getLUTShapes();
        final String[] targetNames = getTargetNames();
        if (lutShapes.length - 1 != targetNames.length) {
            throw new ValidationException("Look-Up-Table invalid: Parameter " + lutshape_name + " does not match " +
                                                  "parameter " + dimnames_name);
        }
    }

    private int getLutLength() {
        int lutLength = 1;
        for (int[] minMaxIndex : minMaxIndices) {
            lutLength *= (minMaxIndex[1] - minMaxIndex[0] + 1);
        }
        return lutLength;
    }

    private void initIndices() {
        minMaxIndices = new int[getNumberOfNonSpectralProperties()][2];
        for (int i = 0; i < minMaxIndices.length; i++) {
            minMaxIndices[i][0] = 0;
            float[] dimValues = getDimValues(DIM_NAMES[i]);
            minMaxIndices[i][1] = dimValues.length - 1;
        }
    }

    private ImageInputStream openLUTStream() throws IOException {
        final FileImageInputStream imageInputStream = new FileImageInputStream(lutFile);
        imageInputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        return imageInputStream;
    }


}
