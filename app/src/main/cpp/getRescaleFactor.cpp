//
// Academic License - for use in teaching, academic research, and meeting
// course requirements at degree granting institutions only.  Not for
// government, commercial, or other organizational use.
// File: getRescaleFactor.cpp
//
// MATLAB Coder version            : 3.3
// C/C++ source code generated on  : 28-Jan-2018 16:40:23
//

// Include Files
#include "rt_nonfinite.h"
#include "getRescaleFactor.h"

// Function Declarations
static void filter(double b[4], double a[4], const double x[1018], const double
                   zi[3], double y[1018]);
static void filtfilt(const double x_in[1000], double y_out[1000]);
static void flipud(double x[1018]);

// Function Definitions

//
// Arguments    : double b[4]
//                double a[4]
//                const double x[1018]
//                const double zi[3]
//                double y[1018]
// Return Type  : void
//
static void filter(double b[4], double a[4], const double x[1018], const double
                   zi[3], double y[1018])
{
  double a1;
  int k;
  int naxpy;
  int j;
  a1 = a[0];
  if ((!rtIsInf(a[0])) && (!rtIsNaN(a[0])) && (!(a[0] == 0.0)) && (a[0] != 1.0))
  {
    for (k = 0; k < 4; k++) {
      b[k] /= a1;
    }

    for (k = 0; k < 3; k++) {
      a[k + 1] /= a1;
    }

    a[0] = 1.0;
  }

  for (k = 0; k < 3; k++) {
    y[k] = zi[k];
  }

  memset(&y[3], 0, 1015U * sizeof(double));
  for (k = 0; k < 1018; k++) {
    naxpy = 1018 - k;
    if (!(naxpy < 4)) {
      naxpy = 4;
    }

    for (j = 0; j + 1 <= naxpy; j++) {
      y[k + j] += x[k] * b[j];
    }

    naxpy = 1017 - k;
    if (!(naxpy < 3)) {
      naxpy = 3;
    }

    a1 = -y[k];
    for (j = 1; j <= naxpy; j++) {
      y[k + j] += a1 * a[j];
    }
  }
}

//
// Arguments    : const double x_in[1000]
//                double y_out[1000]
// Return Type  : void
//
static void filtfilt(const double x_in[1000], double y_out[1000])
{
  double d0;
  double d1;
  int i;
  double y[1018];
  double dv0[4];
  static const double dv1[4] = { 0.963000502799344, -2.88900150839803,
    2.88900150839803, -0.963000502799344 };

  double dv2[4];
  static const double dv3[4] = { 1.0, -2.9246062354507, 2.85202781859389,
    -0.927369968350164 };

  double b_y[1018];
  double a[3];
  static const double b_a[3] = { -0.96300050279560789, 1.9260010055914951,
    -0.96300050279587923 };

  d0 = 2.0 * x_in[0];
  d1 = 2.0 * x_in[999];
  for (i = 0; i < 9; i++) {
    y[i] = d0 - x_in[9 - i];
  }

  memcpy(&y[9], &x_in[0], 1000U * sizeof(double));
  for (i = 0; i < 9; i++) {
    y[i + 1009] = d1 - x_in[998 - i];
  }

  for (i = 0; i < 4; i++) {
    dv0[i] = dv1[i];
    dv2[i] = dv3[i];
  }

  for (i = 0; i < 3; i++) {
    a[i] = b_a[i] * y[0];
  }

  memcpy(&b_y[0], &y[0], 1018U * sizeof(double));
  filter(dv0, dv2, b_y, a, y);
  flipud(y);
  for (i = 0; i < 4; i++) {
    dv0[i] = dv1[i];
    dv2[i] = dv3[i];
  }

  for (i = 0; i < 3; i++) {
    a[i] = b_a[i] * y[0];
  }

  memcpy(&b_y[0], &y[0], 1018U * sizeof(double));
  filter(dv0, dv2, b_y, a, y);
  flipud(y);
  memcpy(&y_out[0], &y[9], 1000U * sizeof(double));
}

//
// Arguments    : double x[1018]
// Return Type  : void
//
static void flipud(double x[1018])
{
  int i;
  double xtmp;
  for (i = 0; i < 509; i++) {
    xtmp = x[i];
    x[i] = x[1017 - i];
    x[1017 - i] = xtmp;
  }
}

//
// Input trial data of 1x1000 EMG data at Fs = 250
// Arguments    : const double X[1000]
//                double mean_p2p
// Return Type  : double
//
double getRescaleFactor(const double X[1000], double mean_p2p)
{
  double SF;
  double X_f[1000];
  int ixstart;
  double mtmp;
  int ix;
  boolean_T exitg1;

  //  Y = [0, 0, 0];
  filtfilt(X, X_f);
  ixstart = 1;
  mtmp = X_f[0];
  if (rtIsNaN(X_f[0])) {
    ix = 2;
    exitg1 = false;
    while ((!exitg1) && (ix < 1001)) {
      ixstart = ix;
      if (!rtIsNaN(X_f[ix - 1])) {
        mtmp = X_f[ix - 1];
        exitg1 = true;
      } else {
        ix++;
      }
    }
  }

  if (ixstart < 1000) {
    while (ixstart + 1 < 1001) {
      if (X_f[ixstart] > mtmp) {
        mtmp = X_f[ixstart];
      }

      ixstart++;
    }
  }

  SF = mean_p2p / mtmp;

  //      Y = [m_min, m_max, p2p];
  return SF;
}

double getPeak2PeakVoltage(const double X[1000]) {
  double X_f[1000];
  int ixstart;
  double p2p;
  int ix;
  boolean_T exitg1;

  //  Y = [0, 0, 0];
  filtfilt(X, X_f);
  ixstart = 1;
  p2p = X_f[0];
  if (rtIsNaN(X_f[0])) {
    ix = 2;
    exitg1 = false;
    while ((!exitg1) && (ix < 1001)) {
      ixstart = ix;
      if (!rtIsNaN(X_f[ix - 1])) {
        p2p = X_f[ix - 1];
        exitg1 = true;
      } else {
        ix++;
      }
    }
  }

  if (ixstart < 1000) {
    while (ixstart + 1 < 1001) {
      if (X_f[ixstart] > p2p) {
        p2p = X_f[ixstart];
      }

      ixstart++;
    }
  }

  //      Y = [m_min, m_max, p2p];
  return p2p;
}

//
// Arguments    : void
// Return Type  : void
//
void getRescaleFactor_initialize()
{
  rt_InitInfAndNaN(8U);
}

//
// Arguments    : void
// Return Type  : void
//
void getRescaleFactor_terminate()
{
  // (no terminate code required)
}

//
// File trailer for getRescaleFactor.cpp
//
// [EOF]
//
