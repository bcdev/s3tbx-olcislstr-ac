package org.esa.s3tbx.c3solcislstr.ac.auxdata;

import org.esa.s3tbx.c3solcislstr.ac.Sensor;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.math.FracIndex;
import org.esa.snap.core.util.math.LookupTable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides SDR auxiliary data handling.
 *
 * @author olafd
 */
public class SdrAuxdata {
    private AotLookupTable aotLut;
    private LookupTable kxAotLut;
    private GasLookupTable gasLookupTable;
    private GasLookupTableHarmonization gasLookupTableHarmonization;
    private double vzaMin;
    private double vzaMax;
    private double szaMin;
    private double szaMax;
    private double aotMin;
    private double aotMax;
    private double hsfMin;
    private double hsfMax;

    private Sensor sensor;

    private SdrAuxdata(Sensor sensor) {
        this.sensor = sensor;
        readAuxdata();
    }

    private static SdrAuxdata instance;

    private static Map<Sensor, SdrAuxdata> instanceMap;

    public static SdrAuxdata getInstance(Sensor sensor) {
        Map<Sensor, SdrAuxdata> instanceMap = getInstanceMap();
        if (!instanceMap.containsKey(sensor)) {
            if (sensor == Sensor.OLCI_SLSTR_NOMINAL || sensor == Sensor.OLCI_SLSTR_S3A || sensor == Sensor.OLCI_SLSTR_S3B) {
                instanceMap.put(sensor, new SdrAuxdata(sensor));
            } else {
                throw new OperatorException(String.format("Sensor '%s' not supported.", sensor));
            }
        }
        return instanceMap.get(sensor);
    }
    private static Map<Sensor, SdrAuxdata> getInstanceMap() {
        if (instanceMap == null) {
            instanceMap = new HashMap<>();
        }
        return instanceMap;
    }
    /**
     * 5-D linear interpolation:
     * returns spectral array [rpw, ttot, sab, rat_tdw, rat_tup, Kx_1, Kx_2]
     * as a function of [vza, sza, phi, hsf, aot] from the interpolation of the MOMO absorption-free LUTs
     *
     * returns null if any lookup fails for given tuple
     */
    public double[][] interpol_lut_MOMO_kx(double vza, double sza, double phi, double hsf, double aot) {
        final LookupTable lut = aotLut.getLut();
        final float[] wvl = aotLut.getWvl();
        final double[] params = aotLut.getLut().getDimension(6).getSequence();
        final double[] kxParams = kxAotLut.getDimension(6).getSequence();
        double[][] result = new double[sensor.getNumBands()][7];

        int lutDimensionCount = lut.getDimensionCount();
        FracIndex[] fracIndexes = FracIndex.createArray(lutDimensionCount);
        double[] v = new double[1 << lutDimensionCount];

        LookupTable.computeFracIndex(lut.getDimension(1), aot, fracIndexes[1]);
        LookupTable.computeFracIndex(lut.getDimension(2), hsf, fracIndexes[2]);
        LookupTable.computeFracIndex(lut.getDimension(3), phi, fracIndexes[3]);
        LookupTable.computeFracIndex(lut.getDimension(4), sza, fracIndexes[4]);
        LookupTable.computeFracIndex(lut.getDimension(5), vza, fracIndexes[5]);

        for (int i = 0; i < result.length; i++) {
            int index = 0;
            LookupTable.computeFracIndex(lut.getDimension(0), wvl[i], fracIndexes[0]);
            for (double param : params) {
                LookupTable.computeFracIndex(lut.getDimension(6), param, fracIndexes[6]);
                try {
                    result[i][index++] = lut.getValue(fracIndexes, v);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            for (double kxParam : kxParams) {
                LookupTable.computeFracIndex(kxAotLut.getDimension(6), kxParam, fracIndexes[6]);
                try {
                    result[i][index++] = kxAotLut.getValue(fracIndexes, v);
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return result;
    }

    private void readAuxdata() {
        try {
            aotLut = AotLookupTable.createAotLookupTable(sensor);
            kxAotLut = AotLookupTable.createAotKxLookupTable(sensor);

            gasLookupTableHarmonization = new GasLookupTableHarmonization(sensor);
            gasLookupTableHarmonization.load();
        } catch (IOException e) {
            throw new OperatorException(e.getMessage());
        }

        LookupTable aotLut = this.aotLut.getLut();

        final double[] vzaArray = aotLut.getDimension(5).getSequence();
        vzaMin = vzaArray[0];
        vzaMax = vzaArray[vzaArray.length - 1];

        final double[] szaArray = aotLut.getDimension(4).getSequence();
        szaMin = szaArray[0];
        szaMax = szaArray[szaArray.length - 1];

        final double[] hsfArray = aotLut.getDimension(2).getSequence();
        hsfMin = 0.001;
        hsfMax = hsfArray[hsfArray.length - 1];

        final double[] aotArray = aotLut.getDimension(1).getSequence();
        aotMin = aotArray[0];
        aotMax = aotArray[aotArray.length - 1];
    }

    public GasLookupTable getGasLookupTable() {
        return gasLookupTable;
    }
    public GasLookupTableHarmonization getGasLookupTableHarmonization() {
        return gasLookupTableHarmonization;
    }
    public double getVzaMin() {
        return vzaMin;
    }

    public double getVzaMax() {
        return vzaMax;
    }

    public double getSzaMin() {
        return szaMin;
    }

    public double getSzaMax() {
        return szaMax;
    }

    public double getAotMin() {
        return aotMin;
    }

    public double getAotMax() {
        return aotMax;
    }

    public double getHsfMin() {
        return hsfMin;
    }

    public double getHsfMax() {
        return hsfMax;
    }

}




