package org.esa.s3tbx.c3solcislstr.ac.aot.math;

/**
 * Interface providing a function.
 *
 * @author Andreas Heckel, Olaf Danne
 */
public interface Function {

    /**
     *  Univariate function definition
     *
     *@param  x  - input value
     *@return      return value
     */
    double f(double x);
}
