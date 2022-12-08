package org.esa.s3tbx.c3solcislstr.ac.auxdata;

import org.esa.s3tbx.c3solcislstr.ac.Sensor;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.core.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Olaf Danne, Tonio Fincke
 */
public class S3LutUtils {

    public static boolean isValidLut(Sensor sensor, String pathToLut) {
        if (sensor == Sensor.OLCI_SLSTR_S3A) {
            String lutFileName = new File(pathToLut).getName();
            return lutFileName.startsWith("sentinel-3a");
        }
        if (sensor == Sensor.OLCI_SLSTR_NOMINAL) {
            String lutFileName = new File(pathToLut).getName();
            return lutFileName.startsWith("sentinel-3a");
        }
        if (sensor == Sensor.OLCI_SLSTR_S3B) {
            String lutFileName = new File(pathToLut).getName();
            return lutFileName.startsWith("sentinel-3b");
        }
        return false;
    }

    static Properties readPropertiesFromJsonFile(File jsonFile, String[] intPropertyNames) throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(new FileReader(jsonFile));
        return readPropertiesFromJsonFile(bufferedReader, intPropertyNames);
    }

    private static Properties readPropertiesFromJsonFile(BufferedReader bufferedReader, String[] intPropertyNames) throws IOException {
        Properties properties = new Properties();
        final String fileContent = bufferedReader.readLine();
        String bracketContent = fileContent.substring(1, fileContent.length() - 1);
        List<Integer> indexesOfColons = new ArrayList<>();
        int currentColonindex = -1;
        while ((currentColonindex = bracketContent.indexOf(':', currentColonindex + 1)) >= 0) {
            indexesOfColons.add(currentColonindex);
        }
        final int numberOfProperties = indexesOfColons.size();
        int indexOfCurrentComma;
        int indexOfLastComma = -1;
        for (int i = 1; i < numberOfProperties; i++) {
            indexOfCurrentComma = bracketContent.substring(0, indexesOfColons.get(i)).lastIndexOf(',');
            if (indexOfCurrentComma != indexOfLastComma) {
                final String propertyName =
                        bracketContent.substring(indexOfLastComma + 1, indexesOfColons.get(i - 1)).replace("\"", "").trim();
                final String propertyValue =
                        bracketContent.substring(indexesOfColons.get(i - 1) + 1, indexOfCurrentComma).trim();
                putPropertyValue(properties, propertyName, propertyValue, intPropertyNames);
                indexOfLastComma = indexOfCurrentComma;
            }
        }
        final String propertyName =
                bracketContent.substring(indexOfLastComma + 1, indexesOfColons.get(numberOfProperties - 1)).replace("\"", "").trim();
        final String propertyValue =
                bracketContent.substring(indexesOfColons.get(numberOfProperties - 1) + 1).trim();
        putPropertyValue(properties, propertyName, propertyValue, intPropertyNames);
        return properties;
    }

    private static void putPropertyValue(Properties properties, String propertyName, String propertyValue,
                                         String[] intPropertyNames) {
        if (propertyValue.startsWith("[") && propertyValue.endsWith("]")) {
            final String[] propertyValues = propertyValue.substring(1, propertyValue.length() - 1).split(",");
            if (ArrayUtils.isMemberOf(propertyName, intPropertyNames)) {
                putPropertiesAsIntegerArray(properties, propertyName, propertyValues);
            } else if (propertyValues.length > 0 && StringUtils.isNumeric(propertyValues[0], Double.class)) {
                putPropertiesAsFloatArray(properties, propertyName, propertyValues);
            } else {
                putPropertiesAsStringArray(properties, propertyName, propertyValues);
            }
        } else {
            properties.put(propertyName, propertyValue.replace("\"", ""));
        }
    }

    private static void putPropertiesAsIntegerArray(Properties properties, String propertyName, String[] propertyValues) {
        int[] propertyValuesAsInteger = new int[propertyValues.length];
        for (int j = 0; j < propertyValues.length; j++) {
            String stringValue = propertyValues[j].trim();
            propertyValuesAsInteger[j] = Integer.parseInt(stringValue);
        }
        properties.put(propertyName, propertyValuesAsInteger);
    }

    private static void putPropertiesAsFloatArray(Properties properties, String propertyName, String[] propertyValues) {
        float[] propertyValuesAsFloat = new float[propertyValues.length];
        for (int j = 0; j < propertyValues.length; j++) {
            String stringValue = propertyValues[j];
            propertyValuesAsFloat[j] = Float.parseFloat(stringValue);
        }
        properties.put(propertyName, propertyValuesAsFloat);
    }

    private static void putPropertiesAsStringArray(Properties properties, String propertyName, String[] propertyValues) {
        List<String> newPropertyValues = new ArrayList<>();
        for (int j = 0; j < propertyValues.length; j++) {
            StringBuilder propertyValueBuilder = new StringBuilder(propertyValues[j]);
            while (j < propertyValues.length - 1 &&
                    !(propertyValueBuilder.charAt(propertyValueBuilder.length() - 1) == '"') &&
                    !(propertyValues[j + 1].charAt(0) == '"')) {
                propertyValueBuilder.append(", ").append(propertyValues[j + 1]);
                j++;
            }
            String propertyValue = propertyValueBuilder.toString();
            propertyValue = propertyValue.replace("\"", "").trim();
            newPropertyValues.add(propertyValue);
        }
        properties.put(propertyName, newPropertyValues.toArray(new String[0]));
    }

}
