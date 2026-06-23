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

/**
 * Describes the pressure levels for atmospheric temperature profiles included with data
 * from OLCI, MERIS and similar sensors.
 */
public interface PressureLevelDescriptor {

    /**
     * Returns the number f pressure levels.
     *
     * @return the number of pressure levels.
     */
    int getLevelCount();

    /**
     * Returns the band name associated with a pressure level.
     *
     * @param i The pressure level.
     * @return the associated band name.
     */
    String getLevelName(int i);

    /**
     * Returns the pressure levels (hPa)
     *
     * @return the pressure levels.
     */
    double[] getLevels();
}
