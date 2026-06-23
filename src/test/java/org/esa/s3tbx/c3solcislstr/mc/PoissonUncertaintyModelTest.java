/*
 * Copyright (C) 2021 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, see http://www.gnu.org/licenses/.
 */

package org.esa.s3tbx.c3solcislstr.mc;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PoissonUncertaintyModelTest {

    private UncertaintyModel model;

    @Before
    public void setUp() {
        model = UncertaintyModels.POISSON;
    }

    @Test
    public void testZeroTerm() {
        assertEquals(1.0, model.getUncertainty(1.0, new double[]{1.0, 0.0, 0.0}), 0.0);
        assertEquals(1.0, model.getUncertainty(2.0, new double[]{1.0, 0.0, 0.0}), 0.0);
    }

    @Test
    public void testUnitTerm() {
        assertEquals(1.0, model.getUncertainty(1.0, new double[]{0.0, 1.0, 0.0}), 0.0);
        assertEquals(2.0, model.getUncertainty(2.0, new double[]{0.0, 2.0, 0.0}), 0.0);
    }

    @Test
    public void testQuadraticTerm() {
        assertEquals(1.0, model.getUncertainty(1.0, new double[]{0.0, 0.0, 1.0}), 0.0);
        assertEquals(2.0, model.getUncertainty(1.0, new double[]{0.0, 0.0, 2.0}), 0.0);
        assertEquals(4.0, model.getUncertainty(2.0, new double[]{0.0, 0.0, 2.0}), 0.0);
    }
}
