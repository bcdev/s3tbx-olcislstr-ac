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

import org.esa.s3tbx.c3solcislstr.mc.lut.*;
import org.esa.snap.core.util.math.MatrixFactory;
import org.jetbrains.annotations.NotNull;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.IntStream;

class GlobAlbedoLookupTable implements AtmosphereLookupTable {

    private static final String RT_LUT_RESOURCE =
            "MERIS_LUT_MOMO_ContinentalI_80_SDR_noG_v2.bin";
    private static final String TG_LUT_RESOURCE =
            "MERIS_LUT_6S_Tg_CWV_OZO.bin";
    private static final int N_PAR = 5;
    private static final int N_WAV = 15;

    private final MultivariateLookupTable lutRT;
    private final MultivariateLookupTable lutTG;
    private final MatrixFactory matrixFactory = new IndexedRowMajorMatrixFactory(new int[]{0, 1, 2, 3});

    private GlobAlbedoLookupTable(MultivariateLookupTable lutRT, MultivariateLookupTable lutTG) {
        this.lutRT = lutRT;
        this.lutTG = lutTG;
    }

    public static AtmosphereLookupTable read() throws IOException {
        final VectorLookupTable lutRT;
        final VectorLookupTable lutTG;

        try (final ImageInputStream iis = openResource(RT_LUT_RESOURCE)) {
            final int[] cardinals = {N_WAV, 0, 0, 0, 0, 0, N_PAR};
            // wav, aot, hsf, raa, sza, vza, par   <- original
            // aot, hsf, raa, sza, vza, par, wav   <- wanted
            final int[] reordering = {1, 2, 3, 4, 5, 6, 0};

            final IntervalPartition[] dRT = readDimensionsRt(iis, cardinals, reordering, 2);
            final Array aRT = readData(iis, cardinals, reordering);
            lutRT = new VectorLookupTable(N_WAV * N_PAR, aRT, dRT);
        }
        try (final ImageInputStream iis = openResource(TG_LUT_RESOURCE)) {
            final int nAmf = iis.readInt();
            final int nCwv = iis.readInt();
            final int nOzo = iis.readInt();
            final int[] cardinals = {nAmf, nCwv, nOzo, N_WAV};
            final int[] reordering = {0, 1, 2, 3};

            final IntervalPartition[] dTG = readDimensionsTg(iis, cardinals, reordering, 0);
            final Array aTG = readData(iis, cardinals, reordering);
            lutTG = new VectorLookupTable(N_WAV, aTG, dTG);
        }
        return new GlobAlbedoLookupTable(lutRT, lutTG);
    }

    @NotNull
    static ImageInputStream openResource(String name) {
        final InputStream is = GlobAlbedoLookupTable.class.getResourceAsStream(name);
        if (is == null) {
            throw new IllegalArgumentException(MessageFormat.format("Resource ''{0}'' not found.", name));
        }
        final ImageInputStream iis = new MemoryCacheImageInputStream(new BufferedInputStream(is));
        iis.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        return iis;
    }

    @Override
    public double[][] getRT(double aot, double hsf, double raa, double sza, double vza) {
        final double[][] matrix = matrixFactory.createMatrix(N_PAR, N_WAV, lutRT.getValues(aot, hsf, raa, sza, vza));

        final double sec = 1.0 / Math.cos(Math.toRadians(sza));
        for (int i = 0; i < N_WAV; i++) {
            matrix[0][i] *= sec * Math.PI;
            matrix[1][i] *= sec;
        }

        return matrix;
    }

    @Override
    public double[] getTG(double amf, double cwv, double ozo) {
        return lutTG.getValues(amf, cwv, ozo);
    }

    @Override
    public MultivariateLookupTable lutRT() {
        return lutRT;
    }

    @Override
    public MultivariateLookupTable lutTG() {
        return lutTG;
    }

    @SuppressWarnings("SameParameterValue")
    private static IntervalPartition[] readDimensionsRt(ImageInputStream iis, int[] cardinals, int[] reordering, int hsf) throws IOException {
        final float[][] dimensions = new float[cardinals.length][];
        for (int i = cardinals.length - 2; i > 0; i--) {
            if (cardinals[i] == 0) {
                cardinals[i] = iis.readInt();
                dimensions[i] = new float[cardinals[i]];
                iis.readFully(dimensions[i], 0, cardinals[i]);
                if (i == hsf) {
                    toHsf(dimensions[i]);
                }
            }
        }  // here, cardinals *and* dimensions are in original order (as required by the lookup table)

        final IntervalPartition[] partitions = new IntervalPartition[cardinals.length - 2];
        for (int i = 0; i < partitions.length; i++) {
            partitions[i] = new IntervalPartition(dimensions[reordering[i]]);
        }  // here, dimensions are reordered while cardinals are still in original order

        return partitions;
    }

    @SuppressWarnings("SameParameterValue")
    private static IntervalPartition[] readDimensionsTg(ImageInputStream iis, int[] cardinals, int[] reordering, int amf) throws IOException {
        final float[][] dimensions = new float[cardinals.length - 1][];
        for (int i = 0; i < cardinals.length - 1; i++) {
            dimensions[i] = new float[cardinals[i]];
            iis.readFully(dimensions[i], 0, cardinals[i]);
            if (i == amf) {
                toAmf(dimensions[i]);
            }
        }
        final IntervalPartition[] partitions = new IntervalPartition[dimensions.length];
        for (int i = 0; i < dimensions.length; i++) {
            partitions[i] = new IntervalPartition(dimensions[reordering[i]]);
        }
        return partitions;
    }

    private static Array readData(ImageInputStream iis, int[] cardinals, int[] reordering) throws IOException {
        final int n = Arrays.stream(cardinals).reduce(1, (a, b) -> a * b);
        final float[] data = new float[n];
        iis.readFully(data, 0, n);
        return new Array.Float(data).reordered(reordering, cardinals);
    }

    private static void toAmf(float[] a) {  // converts angle to AMF
        IntStream.range(0, a.length).forEach(i -> a[i] = (float) (2.0 / Math.cos(Math.toRadians(a[i]))));
    }

    private static void toHsf(float[] p) {  // converts pressure to HSF
        final double c = 1.0 / 5.25588;
        IntStream.range(0, p.length).filter(i -> p[i] != -1.0).forEach(i -> p[i] = (float) ((1.0 - Math.pow(p[i] / 1013.25, c)) / 2.25577E-02));
    }

    private static class IndexedRowMajorMatrixFactory implements MatrixFactory {
        private final int[] indices;

        public IndexedRowMajorMatrixFactory(int[] indices) {
            this.indices = indices;
        }

        @Override
        public double[][] createMatrix(int m, int n, double[] values) {
            final double[][] matrix = new double[indices.length][n];

            for (int i = 0; i < indices.length; i++) {
                System.arraycopy(values, indices[i] * n, matrix[i], 0, n);
            }

            return matrix;
        }
    }

}
