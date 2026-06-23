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

import java.util.Arrays;

enum OlciPressureLevelDescriptor implements PressureLevelDescriptor {
    INSTANCE;

    @Override
    public int getLevelCount() {
        return levels.length;
    }

    @Override
    public String getLevelName(int i) {
        return levelNames[i];
    }

    @Override
    public double[] getLevels() {
        return Arrays.copyOf(levels, getLevelCount());
    }

    private final double[] levels;
    private final String[] levelNames;

    OlciPressureLevelDescriptor() {
        levels = new double[]{1000.0, 950.0, 925.0, 900.0, 850.0, 800.0, 700.0, 600.0, 500.0, 400.0, 300.0, 250.0, 200.0, 150.0, 100.0, 70.0, 50.0, 30.0, 20.0, 10.0, 7.0, 5.0, 3.0, 2.0, 1.0};
        levelNames = new String[levels.length];

        final String basename = "atmospheric_temperature_profile_pressure_level_%d";
        for (int i = 0; i < levels.length; i++) {
            levelNames[i] = String.format(basename, i + 1);
        }
    }
}
