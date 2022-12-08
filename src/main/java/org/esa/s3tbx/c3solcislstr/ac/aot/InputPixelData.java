/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.c3solcislstr.ac.aot;

import org.esa.s3tbx.c3solcislstr.ac.aot.util.PixelGeometry;

/**
 *
 * @author akheckel
 */
public class InputPixelData {
    public final PixelGeometry geomOlci;
    public final double surfPressure;
    public final double o3du;
    public final double wvCol;
    public final int nSpecWvl;
    public final float[] specWvl;
    public final double[] toaReflec;
    public final double[] surfReflec;
    public final double[] diffuseFrac;

    public InputPixelData(PixelGeometry geomOlci,  double surfPressure, double o3du,
                          double wvCol, float[] specWvl, double[] toaReflecOlci) {
        this.geomOlci = geomOlci;
        this.surfPressure = surfPressure;
        this.o3du = o3du;
        this.wvCol = wvCol;
        this.specWvl = specWvl;
        this.nSpecWvl = specWvl.length;
        this.toaReflec = toaReflecOlci;
        this.surfReflec = new double[nSpecWvl];
        this.diffuseFrac = new double[nSpecWvl];
    }

    public synchronized double[] getDiffuseFrac() {
        return diffuseFrac;
    }

    public synchronized double[] getSurfReflec() {
        return surfReflec;
    }

    public synchronized double[] getToaReflec() {
        return toaReflec;
    }

}
