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

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public class NameConverter implements Converter<String[]> {
    @Override
    public Class<? extends String[]> getValueType() {
        return String[].class;
    }

    @Override
    public String[] parse(String text) throws ConversionException {
        final String[] components = text.split(",");
        for (int i = 0; i < components.length; i++) {
            components[i] = components[i].trim();
        }
        return components;
    }

    @Override
    public String format(String[] components) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < components.length; i++) {
            builder.append(components[i].trim());
            if (i + 1 < components.length) {
                builder.append(",");
            }
        }
        return builder.toString();
    }
}
