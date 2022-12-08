package org.esa.s3tbx.c3solcislstr.ac.auxdata;

import org.esa.s3tbx.c3solcislstr.ac.Sensor;
import org.esa.s3tbx.c3solcislstr.ac.aot.lut.MerisLuts;
import org.esa.snap.core.util.math.LookupTable;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Container to hold LUTs for AOT retrieval
 *
 * @author olafd
 */
public class AotLookupTable {

    private LookupTable lut;
    private float[] wvl;
    private float[] solarIrradiance;

    public LookupTable getLut() {
        return lut;
    }

    private void setLut(LookupTable lut) {
        this.lut = lut;
    }

    public float[] getWvl() {
        return wvl;
    }

    private void setWvl(float[] wvl) {
        this.wvl = wvl;
    }

    public float[] getSolarIrradiance() {
        return solarIrradiance;
    }

    private void setSolarIrradiance(float[] solarIrradiance) {
        this.solarIrradiance = solarIrradiance;
    }

    /**
     * reads an AOT LUT (BBDR breadboard procedure GA_read_LUT_AOD)
     * * This LUT is equivalent to the original IDL LUT:
     * for bd = 0, nm_bnd - 1 do  $
     * for jj = 0, nm_aot - 1 do $
     * for ii = 0, nm_hsf - 1 do $
     * for k = 0, nm_azm - 1 do $
     * for j = 0, nm_asl - 1 do $
     * for i = 0, nm_avs - 1 do begin
     * readu, 1, aux
     * lut[*, i, j, nm_azm - k - 1, ii, jj, bd] = aux
     * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, aotValue, hsfValue, aziValue, szaValue, vzaValue, parameterValue});
     *
     * @param sensor The sensor
     * @return LookupTable
     * @throws IOException when failing to real LUT data
     */
    public static AotLookupTable createAotLookupTable(Sensor sensor) throws IOException {
        try (ImageInputStream iis = MerisLuts.getAotLutData()) {
            // read LUT dimensions and values
            float[] vza = MerisLuts.readDimension(iis);
            int nVza = vza.length;
            float[] sza = MerisLuts.readDimension(iis);
            int nSza = sza.length;
            float[] azi = MerisLuts.readDimension(iis);
            int nAzi = azi.length;
            float[] hsf = MerisLuts.readDimension(iis);
            int nHsf = hsf.length;
            // conversion from surf.pressure to elevation ASL
            for (int i = 0; i < nHsf; i++) {
                if (hsf[i] != -1) {
                    // 1.e-3 * (1.d - (xnodes[3, wh_nod] / 1013.25)^(1./5.25588)) / 2.25577e-5
                    final double a = hsf[i] / 1013.25;
                    final double b = 1. / 5.25588;
                    hsf[i] = (float) (0.001 * (1.0 - Math.pow(a, b)) / 2.25577E-5);
                }
            }
            float[] aot = MerisLuts.readDimension(iis);
            int nAot = aot.length;

            float[] parameters = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
            int nParameters = parameters.length;

            float[] wvl = sensor.getWavelength();
            final int nWvl = wvl.length;

            float[] tgLut = new float[nParameters * nVza * nSza * nAzi * nHsf * nAot * nWvl];

            for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                for (int iAot = 0; iAot < nAot; iAot++) {
                    for (int iHsf = 0; iHsf < nHsf; iHsf++) {
                        for (int iAzi = 0; iAzi < nAzi; iAzi++) {
                            for (int iSza = 0; iSza < nSza; iSza++) {
                                for (int iVza = 0; iVza < nVza; iVza++) {
                                    for (int iParams = 0; iParams < nParameters; iParams++) {
                                        int iAziTemp = nAzi - iAzi - 1;
                                        int i = iParams + nParameters * (iVza + nVza * (iSza + nSza * (iAziTemp + nAzi * (iHsf + nHsf * (iAot + nAot * iWvl)))));
                                        tgLut[i] = iis.readFloat();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MerisLuts.readDimension(iis, nWvl); // skip wavelengths
            float[] solarIrradiances = MerisLuts.readDimension(iis, nWvl);

            // store in original sequence (see breadboard: loop over bd, jj, ii, k, j, i in GA_read_lut_AOD
            AotLookupTable aotLut = new AotLookupTable();
            aotLut.setLut(new LookupTable(tgLut, wvl, aot, hsf, azi, sza, vza, parameters));
            aotLut.setWvl(wvl);
            aotLut.setSolarIrradiance(solarIrradiances);
            return aotLut;
        }
    }

    /**
     * reads an AOT Kx LUT (BBDR breadboard procedure GA_read_LUT_AOD)
     * <p/>
     * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, aotValue, hsfValue, aziValue, szaValue, vzaValue, parameterValue});
     *
     * @param sensor The sensor
     * @return LookupTable
     * @throws IOException when failing to real LUT data
     */
    public static LookupTable createAotKxLookupTable(Sensor sensor) throws IOException {
        try (ImageInputStream iis = MerisLuts.getAotKxLutData()) {
            // read LUT dimensions and values
            float[] vza = MerisLuts.readDimension(iis);
            int nVza = vza.length;
            float[] sza = MerisLuts.readDimension(iis);
            int nSza = sza.length;
            float[] azi = MerisLuts.readDimension(iis);
            int nAzi = azi.length;
            float[] hsf = MerisLuts.readDimension(iis);
            int nHsf = hsf.length;
            float[] aot = MerisLuts.readDimension(iis);
            int nAot = aot.length;

            float[] kx = new float[]{1.0f, 2.0f};
            int nKx = kx.length;

            float[] wvl = sensor.getWavelength();
            final int nWvl = wvl.length;

            float[] lut = new float[nKx * nVza * nSza * nAzi * nHsf * nAot * nWvl];
            iis.readFully(lut, 0, lut.length);

            return new LookupTable(lut, wvl, aot, hsf, azi, sza, vza, kx);
        }
    }

}
