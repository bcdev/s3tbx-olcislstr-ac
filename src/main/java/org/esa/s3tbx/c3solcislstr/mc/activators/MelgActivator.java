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

package org.esa.s3tbx.c3solcislstr.mc.activators;

import org.esa.s3tbx.c3solcislstr.mc.NormalVariate;
import org.esa.s3tbx.c3solcislstr.mc.UniformVariate;
import org.esa.s3tbx.c3solcislstr.mc.generators.Melg;
import org.esa.s3tbx.c3solcislstr.mc.variates.MarsagliaNormalVariate;

import java.util.Random;

public class MelgActivator extends MonteCarloBandArithmeticActivator {

    public MelgActivator() {
        super("melg_uniform", "melg_normal");
    }

    @Override
    public UniformVariate createUniformVariate() {
        return new Melg(new Random().longs(7).toArray());
    }

    @Override
    protected NormalVariate createNormalVariate() {
        return new MarsagliaNormalVariate(new Melg(new Random().longs(7).toArray()));
    }

}
