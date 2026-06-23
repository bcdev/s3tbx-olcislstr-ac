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
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.EvalException;
import org.esa.snap.core.jexp.Function;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.impl.AbstractFunction;
import org.esa.snap.runtime.Activator;

public abstract class MonteCarloBandArithmeticActivator implements Activator {
    private UniformVariate uniform;
    private NormalVariate normal;
    private Function functionUniform;
    private Function functionNormal;

    private final String nameUniform;
    private final String nameNormal;

    public MonteCarloBandArithmeticActivator(String nameUniform, String nameNormal) {
        this.nameUniform = nameUniform;
        this.nameNormal = nameNormal;
    }

    protected abstract UniformVariate createUniformVariate();

    protected abstract NormalVariate createNormalVariate();

    @Override
    public final void start() {
        uniform = createUniformVariate();
        normal = createNormalVariate();

        functionUniform = new AbstractFunction.D(nameUniform, -1) {
            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return uniform.nextDouble();
            }
        };
        BandArithmetic.registerFunction(functionUniform);

        functionNormal = new AbstractFunction.D(nameNormal, -1) {
            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return normal.nextDouble();
            }
        };
        BandArithmetic.registerFunction(functionNormal);
    }

    @Override
    public final void stop() {
        if (functionNormal!= null) {
            BandArithmetic.deregisterFunction(functionNormal);
        }
        functionNormal = null;
        normal = null;

        if (functionUniform != null) {
            BandArithmetic.deregisterFunction(functionUniform);
        }
        functionUniform = null;
        uniform = null;
    }
}
