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

package org.esa.s3tbx.c3solcislstr.mc.variates;

import org.esa.s3tbx.c3solcislstr.mc.NormalVariate;
import org.esa.s3tbx.c3solcislstr.mc.UniformVariate;

/**
 * Uses the Box_Muller method to generate standard normally distributed random variates
 * from uniformly distributed random variates.
 */
public final class BoxMullerNormalVariate extends AbstractRandomVariate implements NormalVariate {

    private final UniformVariate u;
    private final UniformVariate v;

    /**
     * Creates a new instance of this class.
     *
     * @param u A strategy to generate uniform random variates.
     */
    public BoxMullerNormalVariate(UniformVariate u) {
        this(u, u);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param u A strategy to generate uniform random variates.
     * @param v A strategy to generate uniform random variates.
     */
    public BoxMullerNormalVariate(UniformVariate u, UniformVariate v) {
        this.u = u;
        this.v = v;
    }

    @Override
    public double nextDouble() {
        synchronized (this) {
            return f(u.nextDouble()) * g(v.nextDouble());
        }
    }

    private double f(double u) {
        return StrictMath.cos(2.0 * StrictMath.PI * u);  // could have used sin(...) as well
    }

    private double g(double v) {
        return StrictMath.sqrt(-2.0 * StrictMath.log(v));
    }
}
