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

package org.esa.s3tbx.c3solcislstr.ac.aot.lut;

import org.esa.s3tbx.c3solcislstr.ac.auxdata.lut.Array;
import org.esa.s3tbx.c3solcislstr.ac.auxdata.lut.IntervalPartition;
import org.esa.s3tbx.c3solcislstr.ac.auxdata.lut.MultivariateLookupTable;
import org.esa.s3tbx.c3solcislstr.ac.auxdata.lut.Remapped;
import org.esa.s3tbx.c3solcislstr.ac.auxdata.lut.VectorLookupTable;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;

public class HyLutSlstr implements Lut {

    private final MultivariateLookupTable lutRP;
    private final MultivariateLookupTable lutTD;
    private final MultivariateLookupTable lutTU;
    private final MultivariateLookupTable lutSA;
    private final MultivariateLookupTable lutDD;
    private final MultivariateLookupTable lutDU;
    private final MultivariateLookupTable lutTG;

    private HyLutSlstr(MultivariateLookupTable lutRP,
                       MultivariateLookupTable lutTD,
                       MultivariateLookupTable lutTU,
                       MultivariateLookupTable lutSA,
                       MultivariateLookupTable lutDD,
                       MultivariateLookupTable lutDU,
                       MultivariateLookupTable lutTG) {
        this.lutRP = lutRP;
        this.lutTD = lutTD;
        this.lutTU = lutTU;
        this.lutSA = lutSA;
        this.lutDD = lutDD;
        this.lutDU = lutDU;
        this.lutTG = lutTG;
    }

    public static HyLutSlstr read(String location, double[] arrayMinMax) throws IOException {

        try (final NetcdfFile ncfile = NetcdfFiles.open(location)) {
            final Array aWav = readData(getVariable(ncfile, "wvl_c"));
            final int nWav = aWav.getLength();

            final Array vzaArray = readData(getVariable(ncfile, "vza"));
            arrayMinMax[0] = vzaArray.getValue(0);
            arrayMinMax[1] = vzaArray.getValue(vzaArray.getLength() - 1);

            final Array szaArray = readData(getVariable(ncfile, "sza"));
            arrayMinMax[2] = szaArray.getValue(0);
            arrayMinMax[3] = szaArray.getValue(szaArray.getLength() - 1);

            final Array hsfArray = readData(getVariable(ncfile, "hsf"));
            arrayMinMax[4] = 0.001;
            arrayMinMax[5] = hsfArray.getValue(hsfArray.getLength() - 1);

            final Array aotArray = readData(getVariable(ncfile, "tauaer"));
            arrayMinMax[6] = aotArray.getValue(0);
            arrayMinMax[7] = aotArray.getValue(aotArray.getLength() - 1);

            final Array ozoArray = readData(getVariable(ncfile, "Uo3"));
            arrayMinMax[8] = ozoArray.getValue(0);
            arrayMinMax[9] = ozoArray.getValue(ozoArray.getLength() - 1);

            final Array cwvArray = readData(getVariable(ncfile, "Uh2o"));
            arrayMinMax[10] = cwvArray.getValue(0);
            arrayMinMax[11] = cwvArray.getValue(cwvArray.getLength() - 1);

            final Array amfArray = readData(getVariable(ncfile, "amf"));
            arrayMinMax[12] = amfArray.getValue(0);
            arrayMinMax[13] = amfArray.getValue(amfArray.getLength() - 1);

            final Variable vRP = getVariable(ncfile, "reflectance_toa");
            final Variable vTD = getVariable(ncfile, "transmission_down");
            final Variable vTU = getVariable(ncfile, "transmission_up");
            final Variable vSA = getVariable(ncfile, "spherical_albedo");
            final Variable vDD = getVariable(ncfile, "diffuse_to_global_down");
            final Variable vDU = getVariable(ncfile, "diffuse_to_global_up");
            final Variable vTG = getVariable(ncfile, "Tg");

            final int aerosolModel = 21;
            final Array aRP = readDataReversed(vRP, 6, aerosolModel);
            final Array aTD = readDataReversed(vTD, 4, aerosolModel);
            final Array aTU = readDataReversed(vTU, 4, aerosolModel);
            final Array aSA = readDataReversed(vSA, 3, aerosolModel);
            final Array aDD = readDataReversed(vDD, 4, aerosolModel);
            final Array aDU = readDataReversed(vDU, 4, aerosolModel);
            final Array aTG = readDataReversed(vTG, 4, 0);

            final IntervalPartition[] dRP = readDimensionsReversed(ncfile, vRP, 1, 1);
            final IntervalPartition[] dTD = readDimensionsReversed(ncfile, vTD, 1, 1);
            final IntervalPartition[] dTU = readDimensionsReversed(ncfile, vTU, 1, 1);
            final IntervalPartition[] dSA = readDimensionsReversed(ncfile, vSA, 1, 1);
            final IntervalPartition[] dDD = readDimensionsReversed(ncfile, vDD, 1, 1);
            final IntervalPartition[] dDU = readDimensionsReversed(ncfile, vDU, 1, 1);
            final IntervalPartition[] dTG = readDimensionsReversed(ncfile, vTG, 1, 1);

            final int[] remapping = {0, 1, 2, 3, 4, 5};
            final MultivariateLookupTable lutRP = new Remapped(new VectorLookupTable(nWav, aRP, dRP), remapping);
            final MultivariateLookupTable lutTD = new Remapped(new VectorLookupTable(nWav, aTD, dTD), remapping);
            final MultivariateLookupTable lutTU = new Remapped(new VectorLookupTable(nWav, aTU, dTU), remapping);
            final MultivariateLookupTable lutSA = new Remapped(new VectorLookupTable(nWav, aSA, dSA), remapping);
            final MultivariateLookupTable lutDD = new Remapped(new VectorLookupTable(nWav, aDD, dDD), remapping);
            final MultivariateLookupTable lutDU = new Remapped(new VectorLookupTable(nWav, aDU, dDU), remapping);
            final MultivariateLookupTable lutTG = new Remapped(new VectorLookupTable(nWav, aTG, dTG), remapping);

            return new HyLutSlstr(lutRP, lutTD, lutTU, lutSA, lutDD, lutDU, lutTG);
        }
    }

    @Override
    public double[][] getRT(double aot, double sza, double vza, double raa, double hsf) {
        final double[][] matrix = new double[5][];

        matrix[0] = lutRP.getValues(hsf, aot, raa, vza, sza);
        final double[] dn = lutTD.getValues(hsf, aot, sza);
        final double[] up = lutTU.getValues(hsf, aot, vza);
        matrix[1] = multiply(dn, up);
        matrix[2] = lutSA.getValues(hsf, aot);
        matrix[3] = lutDD.getValues(hsf, aot, sza);
        matrix[4] = lutDU.getValues(hsf, aot, vza);
        return matrix;
    }

    private static double[] multiply(double[] a, double[] b) {
        for (int i = 0; i < b.length; i++) {
            a[i] = a[i] * b[i];
        }
        return a;
    }

    @Override
    public double[] getTG(double cwv, double ozo, double amf) {
        return lutTG.getValues(cwv, ozo, amf);
    }

    private static Variable getVariable(NetcdfFile ncFile, String name) throws IOException {
        final Variable v = ncFile.findVariable(name);
        if (v == null) {
            throw new IOException(MessageFormat.format("Variable ''{0}'' not found.", name));
        }
        return v;
    }

    private static int[] getCardinals(Variable v) {
        return v.getDimensions().stream().mapToInt(Dimension::getLength).toArray();
    }

    private static int[] getCardinals(Variable v, int dimensionIndex) {
        final int[] cardinals = new int[v.getRank() - 1];
        for (int i = 0, j = 0; i < v.getRank(); i++) {
            if (i != dimensionIndex) {
                cardinals[j++] = v.getDimension(i).getLength();
            }
        }
        return cardinals;
    }

    private static Array readData(Variable v, int dimensionIndex, int coordinateIndex) throws IOException {
        final int[] cardinals = getCardinals(v);
        final int[] start = new int[cardinals.length];
        final int[] sizes = new int[cardinals.length];
        for (int i = 0; i < cardinals.length; i++) {
            if (i == dimensionIndex) {
                start[i] = coordinateIndex;
                sizes[i] = 1;
            } else {
                sizes[i] = cardinals[i];
            }
        }
        try {
            return Array.create(v.read(start, sizes).get1DJavaArray(v.getDataType()));
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    private static Array readData(Variable v, int dimensionIndex, int coordinateIndex, int[] reordering) throws IOException {
        return readData(v, dimensionIndex, coordinateIndex).reordered(reordering, getCardinals(v, dimensionIndex));
    }

    @SuppressWarnings("SameParameterValue")
    private static Array readDataReversed(Variable v, int dimensionIndex, int coordinateIndex) throws IOException {
        return readData(v, dimensionIndex, coordinateIndex, reverseOrdering(v.getRank() - 1));
    }

    private static Array readData(Variable v) throws IOException {
        return Array.create(v.read().get1DJavaArray(v.getDataType()));
    }

    private static IntervalPartition[] readDimensions(NetcdfFile ncfile, Variable v, int[] reordering, int skip, int drop) throws IOException {
        if (reordering.length != v.getRank()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Reordering must be an array of length '{0}'.", v.getRank()));
        }
        final IntervalPartition[] partitions = new IntervalPartition[v.getRank() - skip - drop];
        for (int i = 0; i < v.getRank() - skip - drop; i++) {
            final String dimensionName = v.getDimension(reordering[i + skip]).getShortName();
            partitions[i] = new IntervalPartition(readData(getVariable(ncfile, dimensionName)));
        }
        return partitions;
    }

    @SuppressWarnings("SameParameterValue")
    private static IntervalPartition[] readDimensionsReversed(NetcdfFile ncfile, Variable v, int skip, int drop) throws IOException {
        return readDimensions(ncfile, v, reverseOrdering(v.getRank()), skip, drop);
    }

    private static int[] reverseOrdering(int n) {
        final int[] ordering = new int[n];
        for (int i = 0, k = n - 1; i < n; i++, k--) {
            ordering[i] = k;
        }
        return ordering;
    }

}
