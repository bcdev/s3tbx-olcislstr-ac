package org.esa.s3tbx.c3solcislstr.ac;


/**
 * Constants defined for SDR retrieval
 *
 * @author Olaf Danne
 */
public class OlciSlstrAcConstants {

    public static int MODELTYPE_CONSTANT_VALUE = 0;
    public static double CO2_MIX_CONSTANT_VALUE = 380.0;
    public static float CWV_CONSTANT_VALUE = 1.5f; // used for MERIS
    public final static String AOT_BAND_NAME = "aot";
    public final static String AOT_ERR_BAND_NAME = "aot_err";
    public final static String OLCI_SLSTR_SURF_PRESS_TP_NAME = "surface_pressure_tx";
    public final static String OLCI_SLSTR_WATERVAPOR_TP_NAME = "total_column_water_vapour_tx";
    public final static String OLCI_SLSTR_NDVI_BAND_NAME = "toaNdvi";
    public final static String OLCI_SLSTR_VZA_TP_NAME = "OZA";
    public final static String OLCI_SLSTR_VAA_TP_NAME = "OAA";
    public final static String OLCI_SLSTR_SZA_TP_NAME = "SZA";
    public final static String OLCI_SLSTR_SAA_TP_NAME = "SAA";

    public final static String OLCI_SLSTR_SZA_TP_NADIR_NAME = "solar_zenith_tn";
    public final static String OLCI_SLSTR_SAA_TP_NADIR_NAME = "solar_azimuth_tn";
    public final static String OLCI_SLSTR_VZA_TP_NADIR_NAME = "sat_zenith_tn";
    public final static String OLCI_SLSTR_VAA_TP_NADIR_NAME = "sat_azimuth_tn";

    public final static String OLCI_SLSTR_OZO_TP_NAME = "total_column_ozone_tx";
    public final static String OLCI_SLSTR_ALTITUDE_NAME = "altitude";

    public final static String OLCI_SLSTR_ALTITUDE_NADIR_NAME = "elevation_an";

    final static float[] OLCI_SLSTR_WAVELENGHTS_NOMINAL = {
            400.00f, 412.50f, 442.50f, 490.00f, 510.00f,
            560.00f, 620.00f, 665.00f, 673.75f, 681.25f,
            708.75f, 753.75f, 761.25f, 764.375f, 767.50f,
            778.75f, 865.00f, 885.00f, 900.00f, 940.00f,
            1020.00f,
            554.27f, 659.47f, 868.00f,
            1374.80f, 1613.40f, 2255.70f};

    final static float[] OLCI_SLSTR_WAVELENGHTS_S3A = {
            400.30f, 411.80f, 443.00f, 490.50f, 510.50f,
            560.50f, 620.40f, 665.30f, 674.00f, 681.60f,
            709.10f, 754.20f, 761.70f, 764.80f, 767.90f,
            779.30f, 865.40f, 884.30f, 899.30f, 939.00f,
            1015.8f,
            554.27f, 659.47f, 868.00f,
            1374.80f, 1613.40f, 2255.70f};

    final static float[] OLCI_SLSTR_WAVELENGHTS_S3B = {
            400.60f, 412.00f, 443.00f, 490.40f, 510.40f,
            560.40f, 620.30f, 665.10f, 673.90f, 681.40f,
            709.00f, 754.00f, 761.60f, 764.70f, 767.80f,
            779.10f, 865.30f, 884.10f, 899.10f, 938.80f,
            1015.7f,
            554.27f, 659.47f, 868.00f,
            1374.80f, 1613.40f, 2255.70f};


    final static float[] OLCI_SLSTR_CALIBRATION_COEFFS = {
            1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f};

    // TODO SLSTR bd1 & bd2 estimated
    final static double K_OZONE_OLCI_SLSTR[] = {
            0.000, 0.000, 0.002, 0.021, 0.040,
            0.103, 0.104, 0.052, 0.043, 0.035,
            0.019, 0.008, 0.006, 0.006, 0.005,
            0.001, 0.000, 0.000, 0.000, 0.000,
            0.000,
            0.103, 0.052, 0.000,
            0.000, 0.000, 0.000};

    public final static int[] SPECTRAL_INDICES_OLCI_ONLY = {0, 8, 13, 14, 19, 20};
    public final static int[] SPECTRAL_INDICES_SLSTR_ONLY = {21, 22, 23, 24, 25, 26};

    public final static String[] NOT_MERIS_HERITAGE = new String[]{"Oa01_reflectance", "Oa09_reflectance",
            "Oa14_reflectance", "Oa15_reflectance", "Oa20_reflectance", "Oa21_reflectance",
            "S1_reflectance_an", "S2_reflectance_an", "S3_reflectance_an",
            "S4_reflectance_an", "S5_reflectance_an", "S6_reflectance_an"};

    final static String[] OLCI_SLSTR_TOA_BAND_NAMES = new String[]{
            "Oa01_reflectance", "Oa02_reflectance", "Oa03_reflectance", "Oa04_reflectance", "Oa05_reflectance",
            "Oa06_reflectance", "Oa07_reflectance", "Oa08_reflectance", "Oa09_reflectance", "Oa10_reflectance",
            "Oa11_reflectance", "Oa12_reflectance", "Oa13_reflectance", "Oa14_reflectance", "Oa15_reflectance",
            "Oa16_reflectance", "Oa17_reflectance", "Oa18_reflectance", "Oa19_reflectance", "Oa20_reflectance",
            "Oa21_reflectance",
            "S1_reflectance_an", "S2_reflectance_an", "S3_reflectance_an",
            "S4_reflectance_an", "S5_reflectance_an", "S6_reflectance_an"};

    // SLSTR S1 – S5, S6 channels
    //All OLCI channels except Oa13, Oa14, Oa15, Oa19 and Oa20
    final static String[] OLCI_SLSTR_TOA_BAND_NAMES_TO_CORRECTD = new String[]{
            "Oa01_reflectance", "Oa02_reflectance", "Oa03_reflectance", "Oa04_reflectance", "Oa05_reflectance",
            "Oa06_reflectance", "Oa07_reflectance", "Oa08_reflectance", "Oa09_reflectance", "Oa10_reflectance",
            "Oa11_reflectance", "Oa12_reflectance",
            "Oa16_reflectance", "Oa17_reflectance", "Oa18_reflectance",
            "Oa21_reflectance",
            "S1_reflectance_an", "S2_reflectance_an", "S3_reflectance_an",
            "S5_reflectance_an", "S6_reflectance_an"};

//    final static String[] OLCI_SLSTR_SDR_BAND_NAMES = new String[]{
//            "sdr_1", "sdr_2", "sdr_3", "sdr_4", "sdr_5",
//            "sdr_6", "sdr_7", "sdr_8", "sdr_9", "sdr_10",
//            "sdr_11", "sdr_12",
//            "sdr_13", "sdr_14", "sdr_15",
//            "sdr_16",
//            "sdr_17", "sdr_18", "sdr_19",
//            "sdr_20", "sdr_21"};

    final static String[] OLCI_SLSTR_SDR_BAND_NAMES = new String[]{
            "sdr_Oa01", "sdr_Oa02", "sdr_Oa03", "sdr_Oa04", "sdr_Oa05",
            "sdr_Oa06", "sdr_Oa07", "sdr_Oa08", "sdr_Oa09", "sdr_Oa10",
            "sdr_Oa11", "sdr_Oa12",
            "sdr_Oa16", "sdr_Oa17", "sdr_Oa18",
            "sdr_Oa21",
            "sdr_Sl01", "sdr_Sl02", "sdr_Sl03",
            "sdr_Sl05", "sdr_Sl06"};

    final static int[] OLCI_SLSTR_TOA_BAND_NAMES_TO_CORRECTED_BINAER = {
            1, 1, 1, 1, 1,
            1, 1, 1, 1, 1,
            1, 1, 0, 0, 0,
            1, 1, 1, 0, 0,
            1,
            1, 1, 1,
            0, 1, 1};

    final static String[] OLCI_SLSTR_TOA_BAND_NAMES_MERIS_HERITTAGE = new String[]{
            "Oa02_reflectance", "Oa03_reflectance", "Oa04_reflectance", "Oa05_reflectance",
            "Oa06_reflectance", "Oa07_reflectance", "Oa08_reflectance",
            "Oa10_reflectance", "Oa11_reflectance", "Oa12_reflectance", "Oa13_reflectance",
            "Oa16_reflectance", "Oa17_reflectance", "Oa18_reflectance", "Oa19_reflectance"};

    final static String[] OLCI_SLSTR_TOA_BAND_NAMES_OLCI = new String[]{
            "Oa01_reflectance", "Oa02_reflectance", "Oa03_reflectance", "Oa04_reflectance", "Oa05_reflectance",
            "Oa06_reflectance", "Oa07_reflectance", "Oa08_reflectance", "Oa09_reflectance", "Oa10_reflectance",
            "Oa11_reflectance", "Oa12_reflectance", "Oa13_reflectance", "Oa14_reflectance", "Oa15_reflectance",
            "Oa16_reflectance", "Oa17_reflectance", "Oa18_reflectance", "Oa19_reflectance", "Oa20_reflectance",
            "Oa21_reflectance"};

    final static String[] OLCI_SLSTR_TOA_BAND_NAMES_SLSTR_NADIR = new String[]{
            "S1_reflectance_an", "S2_reflectance_an", "S3_reflectance_an",
            "S4_reflectance_an", "S5_reflectance_an", "S6_reflectance_an"};

//    final static String[] OLCI_SLSTR_SDR_ERROR_BAND_NAMES = new String[]{
//            "sdr_error_1", "sdr_error_2", "sdr_error_3", "sdr_error_4", "sdr_error_5",
//            "sdr_error_6", "sdr_error_7", "sdr_error_8", "sdr_error_9", "sdr_error_10",
//            "sdr_error_11", "sdr_error_12", "sdr_error_13", "sdr_error_14", "sdr_error_15",
//            "sdr_error_16", "sdr_error_17", "sdr_error_18", "sdr_error_19", "sdr_error_20",
//            "sdr_error_21"};
    final static String[] OLCI_SLSTR_SDR_ERROR_BAND_NAMES = new String[]{
            "sdr_error_Oa01", "sdr_error_Oa02", "sdr_error_Oa03", "sdr_error_Oa04", "sdr_error_Oa05",
            "sdr_error_Oa06", "sdr_error_Oa07", "sdr_error_Oa08", "sdr_error_Oa09", "sdr_error_Oa10",
            "sdr_error_Oa11", "sdr_error_Oa12",
            "sdr_error_Oa16", "sdr_error_Oa17", "sdr_error_Oa18",
            "sdr_error_Oa21",
            "sdr_error_Sl01", "sdr_error_Sl02", "sdr_error_Sl03",
            "sdr_error_Sl05", "sdr_error_Sl06"};
    public final static String idepixFlagBandName = "pixel_classif_flags";

    //    OLCI_SPEC_WEIGHTS,
    final static double[] OLCI_SLSTR_SPEC_WEIGHTS = {
            1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0,
            1.0,
            1.0, 1.0, 1.0,
            1.0, 1.0, 1.0};

    private static final String COMMON_LAND_EXPR = "pixel_classif_flags.IDEPIX_LAND && " +
            "(!pixel_classif_flags.IDEPIX_CLOUD AND " +
            "!pixel_classif_flags.IDEPIX_CLOUD_BUFFER AND" +
            "!pixel_classif_flags.IDEPIX_CLOUD_SHADOW) OR pixel_classif_flags.IDEPIX_SNOW_ICE";

    private static final String COMMON_LAND_EXPR_IGNORE_CLOUDS = "pixel_classif_flags.IDEPIX_LAND || " +
            "pixel_classif_flags.IDEPIX_SNOW_ICE";

    final static String[] OLCI_SLSTR_ANCILLARY_BAND_NAMES = new String[]{
            OLCI_SLSTR_VZA_TP_NAME, OLCI_SLSTR_VAA_TP_NAME, OLCI_SLSTR_SZA_TP_NAME, OLCI_SLSTR_SAA_TP_NAME,
            OLCI_SLSTR_SZA_TP_NADIR_NAME, OLCI_SLSTR_SAA_TP_NADIR_NAME, OLCI_SLSTR_VZA_TP_NADIR_NAME,
            OLCI_SLSTR_VAA_TP_NADIR_NAME, OLCI_SLSTR_ALTITUDE_NADIR_NAME,
            OLCI_SLSTR_ALTITUDE_NAME, AOT_BAND_NAME, AOT_ERR_BAND_NAME, OLCI_SLSTR_OZO_TP_NAME,
            OLCI_SLSTR_SURF_PRESS_TP_NAME, OLCI_SLSTR_WATERVAPOR_TP_NAME};

    final static String[] OLCI_SLSTR_GEOM_BAND_NAMES_OLCI = {
            OLCI_SLSTR_SZA_TP_NAME,
            OLCI_SLSTR_SAA_TP_NAME,
            OLCI_SLSTR_VZA_TP_NAME,
            OLCI_SLSTR_VAA_TP_NAME};

    final static String[] OLCI_SLSTR_GEOM_BAND_NAMES_SLSTR_NADIR = {
            OLCI_SLSTR_SZA_TP_NADIR_NAME,
            OLCI_SLSTR_SAA_TP_NADIR_NAME,
            OLCI_SLSTR_VZA_TP_NADIR_NAME,
            OLCI_SLSTR_VAA_TP_NADIR_NAME};


    final static String OLCI_SLSTR_NDVI_EXPR = "(Oa17_reflectance - Oa08_reflectance) / (Oa17_reflectance + Oa08_reflectance)";
    final static String OLCI_SLSTR_NIR_NAME = "Oa17_reflectance";

    final static String OLCI_SLSTR_VALID_EXPR = "(!quality_flags.invalid "
            + " &&  (" + idepixFlagBandName + ".IDEPIX_LAND || " + idepixFlagBandName + ".IDEPIX_SNOW_ICE)"
            + " && !" + idepixFlagBandName + ".IDEPIX_CLOUD_BUFFER)";

    static final String OLCI_SLSTR_AOT_OUT_EXPR = "(!quality_flags.invalid "
//            + " &&  " + idepixFlagBandName + ".IDEPIX_LAND "
//            + " && (!" + idepixFlagBandName + ".IDEPIX_CLOUD_BUFFER || " + idepixFlagBandName + ".IDEPIX_SNOW_ICE)"
            + " && (" + idepixFlagBandName + ".IDEPIX_LAND || " + idepixFlagBandName + ".IDEPIX_SNOW_ICE)"
            + " && (" + OLCI_SLSTR_SZA_TP_NAME + "<70))";

    static final String LAND_EXPR_OLCI_SLSTR =
            "NOT quality_flags.invalid AND NOT quality_flags.cosmetic AND (" + COMMON_LAND_EXPR + ")";

    static final String INPUT_INCONSISTENCY_ERROR_MESSAGE =
            "No valid OLCI/SLSTR product";

    public static final String LAND_EXPR_OLCI_SLSTR_IGNORE_CLOUDS =
            "NOT quality_flags.invalid AND NOT quality_flags.cosmetic AND (" + COMMON_LAND_EXPR_IGNORE_CLOUDS + ")";

    public static final String OLCI_SLSTR_ALL_VALID =
            "NOT quality_flags.invalid AND NOT quality_flags.cosmetic";
}

