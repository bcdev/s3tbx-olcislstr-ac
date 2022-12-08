package org.esa.s3tbx.c3solcislstr.ac.auxdata;

import org.esa.s3tbx.c3solcislstr.ac.OlciSlstrAcConstants;
import org.esa.s3tbx.c3solcislstr.ac.OlciSlstrAcUtils;
import org.esa.s3tbx.c3solcislstr.ac.Sensor;
import org.esa.s3tbx.c3solcislstr.ac.aot.lut.MerisLuts;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Container to hold LUTs for gaseous correction part
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class GasLookupTableHarmonization {

    private float gas2val = 1.5f; // keep variable name from breadboard
    private final Sensor sensor;

    private float[][][] lutGas;

    private float[] amfArray;
    private float[] cwvArray;
    private float[] gasArray;
    private float[] ozoArray;

    public GasLookupTableHarmonization(Sensor sensor) {
        this.sensor = sensor;
    }

    public void load() throws IOException {
        setGasVal(OlciSlstrAcConstants.CWV_CONSTANT_VALUE);
        loadCwvOzoLookupTableArray(sensor);
    }

    //TODO why not CWV -current value
    private void setGasVal(float value) {
        gas2val = value;
    }

    public float[][][] getLutGas() {
        return lutGas;
    }

    public float[] getAmfArray() {
        return amfArray;
    }

    public float[] getCwvArray() {
        return cwvArray;
    }

    public float[] getGasArray() {
        return gasArray;
    }

    private void loadCwvOzoLookupTableArray(Sensor sensor) throws IOException {
        try (ImageInputStream iis = MerisLuts.getCwvLutData()) {
            int nAng = iis.readInt();
            int nCwv = iis.readInt();
            int nOzo = iis.readInt();

            final float[] angArr = MerisLuts.readDimension(iis, nAng);
            cwvArray = MerisLuts.readDimension(iis, nCwv);
            ozoArray = MerisLuts.readDimension(iis, nOzo);

            float[] wvl = sensor.getWavelength();
            final int nWvl = wvl.length;
            float[][][][] cwvOzoLutArray = new float[nWvl][nOzo][nCwv][nAng];
            for (int iAng = 0; iAng < nAng; iAng++) {
                for (int iCwv = 0; iCwv < nCwv; iCwv++) {
                    for (int iOzo = 0; iOzo < nOzo; iOzo++) {
                        for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                            cwvOzoLutArray[iWvl][iOzo][iCwv][iAng] = iis.readFloat();
                        }
                    }
                }
            }
            lutGas = new float[nWvl][nCwv][nAng];
            amfArray = convertAngArrayToAmfArray(angArr);

            int iCwv = OlciSlstrAcUtils.getIndexBefore(gas2val, cwvArray);
            float term = (gas2val - cwvArray[iCwv]) / (cwvArray[iCwv + 1] - cwvArray[iCwv]);
            for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                for (int iOzo = 0; iOzo < nOzo; iOzo++) {
                    for (int iAng = 0; iAng < nAng; iAng++) {
                        lutGas[iWvl][iOzo][iAng] = cwvOzoLutArray[iWvl][iOzo][iCwv][iAng] + (cwvOzoLutArray[iWvl][iOzo][iCwv + 1][iAng] - cwvOzoLutArray[iWvl][iOzo][iCwv][iAng]) * term;
                    }
                }
            }
            gasArray = ozoArray;
        }
    }

    public float[] getTg(float amf_olci, float amf_slstr, float gas, float cwv) {
        int ind_amf_olci = OlciSlstrAcUtils.getIndexBefore(amf_olci, amfArray);
        float amf_p_olci = (amf_olci - amfArray[ind_amf_olci]) / (amfArray[ind_amf_olci + 1] - amfArray[ind_amf_olci]);
        int ind_amf_slstr = OlciSlstrAcUtils.getIndexBefore(amf_slstr, amfArray);
        float amf_p_slstr = (amf_slstr - amfArray[ind_amf_slstr]) / (amfArray[ind_amf_slstr + 1] - amfArray[ind_amf_slstr]);
        int ind_gas = OlciSlstrAcUtils.getIndexBefore(gas, gasArray);
        float gas_p = (gas - gasArray[ind_gas]) / (gasArray[ind_gas + 1] - gasArray[ind_gas]);

        float[] tg = new float[sensor.getNumBands()];
        for (int iWvl = 0; iWvl < tg.length; iWvl++) {
            if (iWvl <21) {
                tg[iWvl] = (1.0f - amf_p_olci) * (1.0f - gas_p) * lutGas[iWvl][ind_gas][ind_amf_olci] +
                        gas_p * (1.0f - amf_p_olci) * lutGas[iWvl][ind_gas + 1][ind_amf_olci] +
                        (1.0f - gas_p) * amf_p_olci * lutGas[iWvl][ind_gas][ind_amf_olci + 1] +
                        amf_p_olci * gas_p * lutGas[iWvl][ind_gas + 1][ind_amf_olci + 1];
            }else{
                tg[iWvl] = (1.0f - amf_p_slstr) * (1.0f - gas_p) * lutGas[iWvl][ind_gas][ind_amf_slstr] +
                        gas_p * (1.0f - amf_p_slstr) * lutGas[iWvl][ind_gas + 1][ind_amf_slstr] +
                        (1.0f - gas_p) * amf_p_slstr * lutGas[iWvl][ind_gas][ind_amf_slstr + 1] +
                        amf_p_slstr * gas_p * lutGas[iWvl][ind_gas + 1][ind_amf_slstr + 1];
            }
        }
        return tg;
    }


    /**
     * converts ang values to geomAmf values (BBDR breadboard l.890)
     *
     * @param ang - array of angles
     *
     * @return array of air mass factors
     */
    public static float[] convertAngArrayToAmfArray(float[] ang) {
        float[] geomAmf = new float[ang.length];
        for (int i = 0; i < geomAmf.length; i++) {
            geomAmf[i] = (float) (2.0 / Math.cos(Math.toRadians(ang[i])));
        }
        return geomAmf;
    }


}
