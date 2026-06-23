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

import java.util.stream.IntStream;

/**
 * Encodes and decodes measurements errors. The encoding is lossy.
 *
 * @author Ralf Quast
 */
public interface ErrorCodec {

    /**
     * Encodes and decodes measurement errors, which satisfy a standard normal distribution.
     * The encoding is symmetric and the absolute accuracy loss is less for smaller than for
     * larger absolute values. The encoding is truncated at about 4.84.
     */
    ErrorCodec LAPLACE = new ErrorCodec() {
        @Override
        public byte encode(double d) {
            return (byte) Math.copySign(Math.min(126.0, Math.round(127.0 * forward(d))), d);
        }

        @Override
        public byte[] encode(double[] decoded, byte[] encoded) {
            IntStream.range(0, decoded.length).forEach(i -> encoded[i] = encode(decoded[i]));
            return encoded;
        }

        @Override
        public double decode(byte b) {
            return Math.copySign(reverse(b / 127.0), b);
        }

        @Override
        public double[] decode(byte[] encoded, double[] decoded) {
            IntStream.range(0, encoded.length).forEach(i -> decoded[i] = decode(encoded[i]));
            return decoded;
        }

        private double forward(double x) {
            return -StrictMath.expm1(-Math.abs(x));
        }

        private double reverse(double y) {
            return -StrictMath.log1p(-Math.abs(y));
        }
    };

    /**
     * Encodes and decodes measurement errors, which satisfy a standard normal distribution.
     * The encoding is symmetric. The absolute accuracy loss is constant. The encoding is
     * truncated at 5.0.
     */
    ErrorCodec LINEAR = new ErrorCodec() {
        final double TRUNCATION_LIMIT = 5.0;

        @Override
        public byte encode(double d) {
            return (byte) Math.copySign(Math.min(127.0, Math.round(127.0 * forward(d))), d);
        }

        @Override
        public byte[] encode(double[] decoded, byte[] encoded) {
            IntStream.range(0, decoded.length).forEach(i -> encoded[i] = encode(decoded[i]));
            return encoded;
        }

        @Override
        public double decode(byte b) {
            return Math.copySign(reverse(b / 127.0), b);
        }

        @Override
        public double[] decode(byte[] encoded, double[] decoded) {
            IntStream.range(0, encoded.length).forEach(i -> decoded[i] = decode(encoded[i]));
            return decoded;
        }

        private double forward(double x) {
            return Math.abs(x) / TRUNCATION_LIMIT;
        }

        private double reverse(double y) {
            return Math.abs(y) * TRUNCATION_LIMIT;
        }
    };

    /**
     * Encodes and decodes measurement errors, which satisfy a standard normal distribution.
     * The encoding is symmetric and the absolute accuracy loss is less for intermediate than
     * for lower or larger absolute values. The encoding is truncated at about 3.11.
     */
    ErrorCodec NORMAL = new ErrorCodec() {
        @Override
        public byte encode(double d) {
            return (byte) Math.copySign(Math.round(Math.min(126.0, 127.0 * forward(d))), d);
        }

        @Override
        public byte[] encode(double[] decoded, byte[] encoded) {
            IntStream.range(0, decoded.length).forEach(i -> encoded[i] = encode(decoded[i]));
            return encoded;
        }

        @Override
        public double decode(byte b) {
            return Math.copySign(reverse(b / 127.0), b);
        }

        @Override
        public double[] decode(byte[] encoded, double[] decoded) {
            IntStream.range(0, encoded.length).forEach(i -> decoded[i] = decode(encoded[i]));
            return decoded;
        }

        private double forward(double x) {
            return -StrictMath.expm1(-square(x) / 2.0);
        }

        private double reverse(double y) {
            return StrictMath.sqrt(-2.0 * StrictMath.log1p(-Math.abs(y)));
        }

        private double square(double x) {
            return x == 0.0 ? 0.0 : x * x;
        }
    };

    /**
     * Encodes a single original value.
     *
     * @param d The original value.
     * @return the encoded value.
     */
    byte encode(double d);

    /**
     * Encodes multiple original values.
     *
     * @param decoded The original values.
     * @param encoded The storage used for the encoded values.
     * @return the encoded values.
     */
    byte[] encode(double[] decoded, byte[] encoded);

    /**
     * Decodes a singe encoded value.
     *
     * @param b The encoded value.
     * @return the decoded value.
     */
    double decode(byte b);

    /**
     * Decodes multiple encoded values.
     * @param encoded The encoded values.
     * @param decoded The storage used for the decoded values.
     * @return the decoded values.
     */
    double[] decode(byte[] encoded, double[] decoded);

}
