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

package org.esa.s3tbx.c3solcislstr.mc;

public class UncertaintyModelFactory {
    private final String name;

    public UncertaintyModelFactory(String name) {
        this.name = name;
    }

    public UncertaintyModel newUncertaintyModel() {
        switch (name) {
            case "Poisson":
                return UncertaintyModels.POISSON;
            case "CAMS AOD (2020)":
                return UncertaintyModels.CAMS_AOD_2020;
            case "Relative":
                return UncertaintyModels.RELATIVE_01;
            case "Relative (10%)":
                return UncertaintyModels.RELATIVE_10;
            case "Relative (15%)":
                return UncertaintyModels.RELATIVE_15;
            case "Relative (20%)":
                return UncertaintyModels.RELATIVE_20;
            default:
                return UncertaintyModels.valueOf(name);
        }
    }
}
