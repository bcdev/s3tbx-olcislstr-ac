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

public class EncodedVector implements Vector {

    private final byte[] bytes;
    private final ErrorCodec codec;

    public EncodedVector(byte[] bytes, ErrorCodec codec) {
        this.bytes = bytes;
        this.codec = codec;
    }

    @Override
    public final int size() {
        return bytes.length;
    }

    @Override
    public final double get(int i) {
        return codec.decode(bytes[i]);
    }
}
