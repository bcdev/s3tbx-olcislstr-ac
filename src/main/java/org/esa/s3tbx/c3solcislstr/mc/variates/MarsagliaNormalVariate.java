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
 * Uses the Marsaglia polar method to generate standard normally distributed random variates
 * from uniformly distributed random variates.
 */
public final class MarsagliaNormalVariate extends AbstractRandomVariate implements NormalVariate {

    private final UniformVariate u;
    private final UniformVariate v;

    /**
     * Creates a new instance of this class.
     *
     * @param u A strategy to generate uniform random variates.
     */
    public MarsagliaNormalVariate(UniformVariate u) {
        this(u, u);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param u A strategy to generate uniform random variates.
     * @param v A strategy to generate uniform random variates.
     */
    public MarsagliaNormalVariate(UniformVariate u, UniformVariate v) {
        this.u = u;
        this.v = v;
    }

    @Override
    public double nextDouble() {
        synchronized (this) {
            double r;
            double x;
            double y;
            do {
                x = 2.0 * u.nextDouble() - 1.0;
                y = 2.0 * v.nextDouble() - 1.0;
                r = x * x + y * y;
            } while (r >= 1.0 || r == 0.0);

            return x * f(r);  // could have used y as well
        }
    }

    private double f(double r) {
        return StrictMath.sqrt(-2.0 * (StrictMath.log(r) / r));
    }
}
