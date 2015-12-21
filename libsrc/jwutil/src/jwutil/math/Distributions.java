// Distributions.java, created Oct 27, 2004 12:03:33 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.math;

import java.math.BigInteger;

/**
 * Various Distributions and other statistics-related functions.
 * Currently this only has the student-t distribution.
 * 
 * Based on the "Numerical Recipes" book and some code by Bryan Lewis.
 * 
 * @author jwhaley
 * @version $Id: Distributions.java,v 1.2 2005/05/28 10:23:15 joewhaley Exp $
 */
public class Distributions {
    
    /**
     * Factorial.
     * 
     * @param n  input number
     * @return  factorial
     */
    public static BigInteger factorial(int n) {
        BigInteger fact = BigInteger.ONE;
        for (int i = n; i > 1; i--) {
            fact = fact.multiply(BigInteger.valueOf(i));
        }
        return fact;
    }

    /**
     * Binomial distribution.
     */
    public static BigInteger binomial(int n, int k) {
        BigInteger r = BigInteger.ONE;
        int n_minus_k = n-k;
        while (n > k) {
            r = r.multiply(BigInteger.valueOf(n--));
        }
        while (n_minus_k > 0) {
            r = r.divide(BigInteger.valueOf(n_minus_k--));
        }
        return r;
    }
    
    /**
     * Sloane's A000670.
     * Also known as "preferential arrangements of n labeled elements" or
     * "weak orders on n labeled elements".
     */
    public static BigInteger recurrenceA000670(int n) {
        if (n <= 1) return BigInteger.ONE;
        BigInteger sum = BigInteger.ZERO;
        for (int k = 1; k <= n; ++k) {
            sum = sum.add(binomial(n, k).multiply(recurrenceA000670(n-k)));
        }
        return sum;
    }
    
    /**
     * An approximation to ln(gamma(x)).
     */
    public static double logGamma(double xx) {
        // define some constants...
        int j;
        double stp = 2.506628274650;
        double cof[] = new double[6];
        cof[0] = 76.18009173;
        cof[1] = -86.50532033;
        cof[2] = 24.01409822;
        cof[3] = -1.231739516;
        cof[4] = 0.120858003E-02;
        cof[5] = -0.536382E-05;
        double x = xx - 1;
        double tmp = x + 5.5;
        tmp = (x + 0.5) * Math.log(tmp) - tmp;
        double ser = 1;
        for (j = 0; j < 6; j++) {
            x++;
            ser = ser + cof[j] / x;
        }
        double retVal = tmp + Math.log(stp * ser);
        return retVal;
    }

    /**
     * An approximation of gamma(x).
     */
    public static double gamma(double x) {
        double f = 10E99;
        double g = 1;
        if (x > 0) {
            while (x < 3) {
                g = g * x;
                x = x + 1;
            }
            f = (1 - (2 / (7 * Math.pow(x, 2))) * (1 - 2 / (3 * Math.pow(x, 2))))
                / (30 * Math.pow(x, 2));
            f = (1 - f) / (12 * x) + x * (Math.log(x) - 1);
            f = (Math.exp(f) / g) * Math.pow(2 * Math.PI / x, 0.5);
        } else {
            f = Double.POSITIVE_INFINITY;
        }
        return f;
    }

    /**
     * A continued fraction representation of the beta function.
     */
    public static double betacf(double a, double b, double x) {
        int maxIterations = 50, m = 1;
        double eps = 3E-5;
        double am = 1;
        double bm = 1;
        double az = 1;
        double qab = a + b;
        double qap = a + 1;
        double qam = a - 1;
        double bz = 1 - qab * x / qap;
        double aold = 0;
        double em, tem, d, ap, bp, app, bpp;
        while ((m < maxIterations) && (Math.abs(az - aold) >= eps * Math.abs(az))) {
            em = m;
            tem = em + em;
            d = em * (b - m) * x / ((qam + tem) * (a + tem));
            ap = az + d * am;
            bp = bz + d * bm;
            d = -(a + em) * (qab + em) * x / ((a + tem) * (qap + tem));
            app = ap + d * az;
            bpp = bp + d * bz;
            aold = az;
            am = ap / bpp;
            bm = bp / bpp;
            az = app / bpp;
            bz = 1;
            m++;
        }
        return az;
    }

    /**
     * The incomplete beta function from 0 to x with parameters a, b.
     * x must be in (0,1) (else returns error)
     */
    public static double betai(double a, double b, double x) {
        double bt = 0, beta = Double.POSITIVE_INFINITY;
        if (x == 0 || x == 1) {
            bt = 0;
        } else if ((x > 0) && (x < 1)) {
            bt = gamma(a + b) * Math.pow(x, a) * Math.pow(1 - x, b) / (gamma(a) * gamma(b));
        }
        if (x < (a + 1) / (a + b + 2)) {
            beta = bt * betacf(a, b, x) / a;
        } else {
            beta = 1 - bt * betacf(b, a, 1 - x) / b;
        }
        return beta;
    }

    /**
     * F distribution with v1, v2 deg. freedom P(x>f)
     */
    public static double fDist(double v1, double v2, double f) {
        double p = betai(v1 / 2, v2 / 2, v1 / (v1 + v2 * f));
        return p;
    }

    /**
     * Coefficient appearing in Student's t distribution.
     */
    static double student_c(double v) {
        return Math.exp(logGamma((v + 1) / 2))
            / (Math.sqrt(Math.PI * v) * Math.exp(logGamma(v / 2)));
    }

    /**
     * Student's t density with v degrees of freedom.
     */
    public static double student_tDen(double v, double t) {
        return student_c(v) * Math.pow(1 + (t * t) / v, -0.5 * (v + 1));
    }

    /**
     * Student's t distribution with v degrees of freedom.
     * 
     * This only uses compound trapezoid, pending a good integration package
     * Returned value is P( x > t) for a r.v. x with v deg. freedom.
     * 
     * NOTE: With the gamma function supplied here, and the simple
     * trapezoidal sum used for integration, the accuracy is only about 5
     * decimal places. Values below 0.00001 are returned as zero.
     */
    public static double stDist(double v, double t) {
        double sm = 0.5;
        double u = 0;
        double sign = 1;
        double stepSize = t / 5000;
        if (t < 0) {
            sign = -1;
        }
        for (u = 0; u <= (sign * t); u = u + stepSize) {
            sm = sm + stepSize * student_tDen(v, u);
        }
        if (sign < 0) {
            sm = 0.5 - sm;
        } else {
            sm = 1 - sm;
        }
        if (sm < 0) {
            sm = 0; // do not allow probability less than zero from roundoff
                    // error
        } else if (sm > 1) {
            sm = 1; // do not allow probability more than one from roundoff error
        }
        return sm;
    }
    
    /**
     * Returns the upper critical values of Student's t distribution with v
     * degrees of freedom and the given significance level.
     * 
     * This uses a lookup table so it is fast but not accurate for values
     * that don't appear in the table.
     */
    public static double uc_stDist(double sigLevel, double v) {
        int i;
        for (i = 0; i < students_t_table[0].length - 1; ++i) {
            if (sigLevel >= students_t_table[0][i]) break;
        }
        int v_i = (int) v;
        v_i = Math.max(1, v_i);
        v_i = Math.min(students_t_table.length-1, v_i);
        return students_t_table[v_i][i];
    }
    
    static final double[][] students_t_table = {
           {   0.1,   0.05,  0.025,   0.01,  0.005,  0.001 },
           { 3.078,  6.314, 12.706, 31.821, 63.657,318.313 }, // 1
           { 1.886,  2.920,  4.303,  6.965,  9.925, 22.327 }, // 2
           { 1.638,  2.353,  3.182,  4.541,  5.841, 10.215 }, // ...
           { 1.533,  2.132,  2.776,  3.747,  4.604,  7.173 },
           { 1.476,  2.015,  2.571,  3.365,  4.032,  5.893 },
           { 1.440,  1.943,  2.447,  3.143,  3.707,  5.208 },
           { 1.415,  1.895,  2.365,  2.998,  3.499,  4.782 },
           { 1.397,  1.860,  2.306,  2.896,  3.355,  4.499 },
           { 1.383,  1.833,  2.262,  2.821,  3.250,  4.296 },
           { 1.372,  1.812,  2.228,  2.764,  3.169,  4.143 },
           { 1.363,  1.796,  2.201,  2.718,  3.106,  4.024 },
           { 1.356,  1.782,  2.179,  2.681,  3.055,  3.929 },
           { 1.350,  1.771,  2.160,  2.650,  3.012,  3.852 },
           { 1.345,  1.761,  2.145,  2.624,  2.977,  3.787 },
           { 1.341,  1.753,  2.131,  2.602,  2.947,  3.733 },
           { 1.337,  1.746,  2.120,  2.583,  2.921,  3.686 },
           { 1.333,  1.740,  2.110,  2.567,  2.898,  3.646 },
           { 1.330,  1.734,  2.101,  2.552,  2.878,  3.610 },
           { 1.328,  1.729,  2.093,  2.539,  2.861,  3.579 },
           { 1.325,  1.725,  2.086,  2.528,  2.845,  3.552 },
           { 1.323,  1.721,  2.080,  2.518,  2.831,  3.527 },
           { 1.321,  1.717,  2.074,  2.508,  2.819,  3.505 },
           { 1.319,  1.714,  2.069,  2.500,  2.807,  3.485 },
           { 1.318,  1.711,  2.064,  2.492,  2.797,  3.467 },
           { 1.316,  1.708,  2.060,  2.485,  2.787,  3.450 },
           { 1.315,  1.706,  2.056,  2.479,  2.779,  3.435 },
           { 1.314,  1.703,  2.052,  2.473,  2.771,  3.421 },
           { 1.313,  1.701,  2.048,  2.467,  2.763,  3.408 },
           { 1.311,  1.699,  2.045,  2.462,  2.756,  3.396 },
           { 1.310,  1.697,  2.042,  2.457,  2.750,  3.385 },
           { 1.309,  1.696,  2.040,  2.453,  2.744,  3.375 },
           { 1.309,  1.694,  2.037,  2.449,  2.738,  3.365 },
           { 1.308,  1.692,  2.035,  2.445,  2.733,  3.356 },
           { 1.307,  1.691,  2.032,  2.441,  2.728,  3.348 },
           { 1.306,  1.690,  2.030,  2.438,  2.724,  3.340 },
           { 1.306,  1.688,  2.028,  2.434,  2.719,  3.333 },
           { 1.305,  1.687,  2.026,  2.431,  2.715,  3.326 },
           { 1.304,  1.686,  2.024,  2.429,  2.712,  3.319 },
           { 1.304,  1.685,  2.023,  2.426,  2.708,  3.313 },
           { 1.303,  1.684,  2.021,  2.423,  2.704,  3.307 },
           { 1.303,  1.683,  2.020,  2.421,  2.701,  3.301 },
           { 1.302,  1.682,  2.018,  2.418,  2.698,  3.296 },
           { 1.302,  1.681,  2.017,  2.416,  2.695,  3.291 },
           { 1.301,  1.680,  2.015,  2.414,  2.692,  3.286 },
           { 1.301,  1.679,  2.014,  2.412,  2.690,  3.281 },
           { 1.300,  1.679,  2.013,  2.410,  2.687,  3.277 },
           { 1.300,  1.678,  2.012,  2.408,  2.685,  3.273 },
           { 1.299,  1.677,  2.011,  2.407,  2.682,  3.269 },
           { 1.299,  1.677,  2.010,  2.405,  2.680,  3.265 },
           { 1.299,  1.676,  2.009,  2.403,  2.678,  3.261 },
           { 1.298,  1.675,  2.008,  2.402,  2.676,  3.258 },
           { 1.298,  1.675,  2.007,  2.400,  2.674,  3.255 },
           { 1.298,  1.674,  2.006,  2.399,  2.672,  3.251 },
           { 1.297,  1.674,  2.005,  2.397,  2.670,  3.248 },
           { 1.297,  1.673,  2.004,  2.396,  2.668,  3.245 },
           { 1.297,  1.673,  2.003,  2.395,  2.667,  3.242 },
           { 1.297,  1.672,  2.002,  2.394,  2.665,  3.239 },
           { 1.296,  1.672,  2.002,  2.392,  2.663,  3.237 },
           { 1.296,  1.671,  2.001,  2.391,  2.662,  3.234 },
           { 1.296,  1.671,  2.000,  2.390,  2.660,  3.232 },
           { 1.296,  1.670,  2.000,  2.389,  2.659,  3.229 },
           { 1.295,  1.670,  1.999,  2.388,  2.657,  3.227 },
           { 1.295,  1.669,  1.998,  2.387,  2.656,  3.225 },
           { 1.295,  1.669,  1.998,  2.386,  2.655,  3.223 },
           { 1.295,  1.669,  1.997,  2.385,  2.654,  3.220 },
           { 1.295,  1.668,  1.997,  2.384,  2.652,  3.218 },
           { 1.294,  1.668,  1.996,  2.383,  2.651,  3.216 },
           { 1.294,  1.668,  1.995,  2.382,  2.650,  3.214 },
           { 1.294,  1.667,  1.995,  2.382,  2.649,  3.213 },
           { 1.294,  1.667,  1.994,  2.381,  2.648,  3.211 },
           { 1.294,  1.667,  1.994,  2.380,  2.647,  3.209 },
           { 1.293,  1.666,  1.993,  2.379,  2.646,  3.207 },
           { 1.293,  1.666,  1.993,  2.379,  2.645,  3.206 },
           { 1.293,  1.666,  1.993,  2.378,  2.644,  3.204 },
           { 1.293,  1.665,  1.992,  2.377,  2.643,  3.202 },
           { 1.293,  1.665,  1.992,  2.376,  2.642,  3.201 },
           { 1.293,  1.665,  1.991,  2.376,  2.641,  3.199 },
           { 1.292,  1.665,  1.991,  2.375,  2.640,  3.198 },
           { 1.292,  1.664,  1.990,  2.374,  2.640,  3.197 },
           { 1.292,  1.664,  1.990,  2.374,  2.639,  3.195 },
           { 1.292,  1.664,  1.990,  2.373,  2.638,  3.194 },
           { 1.292,  1.664,  1.989,  2.373,  2.637,  3.193 },
           { 1.292,  1.663,  1.989,  2.372,  2.636,  3.191 },
           { 1.292,  1.663,  1.989,  2.372,  2.636,  3.190 },
           { 1.292,  1.663,  1.988,  2.371,  2.635,  3.189 },
           { 1.291,  1.663,  1.988,  2.370,  2.634,  3.188 },
           { 1.291,  1.663,  1.988,  2.370,  2.634,  3.187 },
           { 1.291,  1.662,  1.987,  2.369,  2.633,  3.185 },
           { 1.291,  1.662,  1.987,  2.369,  2.632,  3.184 },
           { 1.291,  1.662,  1.987,  2.368,  2.632,  3.183 },
           { 1.291,  1.662,  1.986,  2.368,  2.631,  3.182 },
           { 1.291,  1.662,  1.986,  2.368,  2.630,  3.181 },
           { 1.291,  1.661,  1.986,  2.367,  2.630,  3.180 },
           { 1.291,  1.661,  1.986,  2.367,  2.629,  3.179 },
           { 1.291,  1.661,  1.985,  2.366,  2.629,  3.178 },
           { 1.290,  1.661,  1.985,  2.366,  2.628,  3.177 },
           { 1.290,  1.661,  1.985,  2.365,  2.627,  3.176 },
           { 1.290,  1.661,  1.984,  2.365,  2.627,  3.175 },
           { 1.290,  1.660,  1.984,  2.365,  2.626,  3.175 }, // 99
           { 1.290,  1.660,  1.984,  2.364,  2.626,  3.174 }, // 100
           { 1.282,  1.645,  1.960,  2.326,  2.576,  3.090 }  // infinity
  };
}
