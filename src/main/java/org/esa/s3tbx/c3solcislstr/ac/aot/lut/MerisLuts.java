/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.c3solcislstr.ac.aot.lut;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * Access to the LookUpTables
 */
public class MerisLuts {
    private static final String merisAotLutFileName = "MERIS/MERIS_LUT_MOMO_ContinentalI_80_SDR_noG_v2.bin";
    private static final String merisCwvLutFileName = "MERIS/MERIS_LUT_6S_Tg_CWV_OZO.bin";
    public static ImageInputStream getAotLutData() {
        return openStream(merisAotLutFileName);
    }

    public static ImageInputStream getCwvLutData() {
        return openStream(merisCwvLutFileName);
    }

    private static ImageInputStream openStream(String path) {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(openResource(path));
        ImageInputStream imageInputStream = new MemoryCacheImageInputStream(bufferedInputStream);
        imageInputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        return imageInputStream;
    }

    private static InputStream openResource(String path) {
        //InputPixelData.class.getResourceAsStream(fname)
        InputStream inputStream = MerisLuts.class.getResourceAsStream(path);
        if (inputStream == null) {
            throw new IllegalArgumentException("Could not find resource: " + path);
        }
        return inputStream;
    }

}
