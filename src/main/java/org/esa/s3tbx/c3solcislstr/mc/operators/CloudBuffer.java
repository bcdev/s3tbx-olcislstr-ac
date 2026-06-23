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


import org.esa.snap.core.gpf.Tile;
import org.esa.snap.idepix.core.IdepixConstants;

import java.awt.*;

/**
 * cloud buffer algorithms
 */
class CloudBuffer {

    static void setCloudBuffer(Tile targetTile, Rectangle srcRectangle, Tile sourceFlagTile, int cloudBufferWidth) {
        for (int y = srcRectangle.y; y < srcRectangle.y + srcRectangle.height; y++) {
            for (int x = srcRectangle.x; x < srcRectangle.x + srcRectangle.width; x++) {
                final boolean isCloud = sourceFlagTile.getSampleBit(x, y, IdepixConstants.IDEPIX_CLOUD);
                if (isCloud) {
                    computeSimpleCloudBuffer(x, y,
                                             targetTile,
                                             srcRectangle,
                                             cloudBufferWidth
                    );
                }
            }
        }
    }

    static void computeSimpleCloudBuffer(int x, int y,
                                         Tile targetTile,
                                         Rectangle extendedRectangle,
                                         int cloudBufferWidth) {
        Rectangle rectangle = targetTile.getRectangle();
        int LEFT_BORDER = Math.max(x - cloudBufferWidth, extendedRectangle.x);
        int RIGHT_BORDER = Math.min(x + cloudBufferWidth, extendedRectangle.x + extendedRectangle.width - 1);
        int TOP_BORDER = Math.max(y - cloudBufferWidth, extendedRectangle.y);
        int BOTTOM_BORDER = Math.min(y + cloudBufferWidth, extendedRectangle.y + extendedRectangle.height - 1);

        for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
            for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                if (rectangle.contains(i, j)) {
                    targetTile.setSample(i, j, IdepixConstants.IDEPIX_CLOUD_BUFFER, true);
                }
            }
        }
    }

}
