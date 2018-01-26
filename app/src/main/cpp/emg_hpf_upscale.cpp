//
// Academic License - for use in teaching, academic research, and meeting
// course requirements at degree granting institutions only.  Not for
// government, commercial, or other organizational use.
// File: emg_hpf_upscale.cpp
//
// MATLAB Coder version            : 3.3
// C/C++ source code generated on  : 26-Jan-2018 12:37:32
//

// Include Files
#include "rt_nonfinite.h"
#include "emg_hpf_upscale.h"

// Function Declarations
static void filter(double b[4], double a[4], const double x[146], const double
zi[3], double y[146]);

// Function Definitions

//
// Arguments    : double b[4]
//                double a[4]
//                const double x[146]
//                const double zi[3]
//                double y[146]
// Return Type  : void
//
static void filter(double b[4], double a[4], const double x[146], const double
zi[3], double y[146])
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

  memset(&y[3], 0, 143U * sizeof(double));
  for (k = 0; k < 146; k++) {
    naxpy = 146 - k;
    if (!(naxpy < 4)) {
      naxpy = 4;
    }

    for (j = 0; j + 1 <= naxpy; j++) {
      y[k + j] += x[k] * b[j];
    }

    naxpy = 145 - k;
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
// Filters EMG Signal; HPF; 1.5Hz; Fs =250; Butterworth:
// Arguments    : const double X[128]
//                float Y[128]
// Return Type  : void
//
void emg_hpf_upscale(const double X[128], float Y[128], double scale_factor)
{
  double xtmp;
  double d0;
  int i;
  double y[146];
  double dv0[4];
  static const double dv1[4] = { 0.963000502799344, -2.88900150839803,
                                 2.88900150839803, -0.963000502799344 };

  double dv2[4];
  static const double dv3[4] = { 1.0, -2.9246062354507, 2.85202781859389,
                                 -0.927369968350164 };

  double b_y[146];
  double a[3];
  static const double b_a[3] = { -0.96300050279560789, 1.9260010055914951,
                                 -0.96300050279587923 };

  xtmp = 2.0 * X[0];
  d0 = 2.0 * X[127];
  for (i = 0; i < 9; i++) {
    y[i] = xtmp - X[9 - i];
  }

  memcpy(&y[9], &X[0], sizeof(double) << 7);
  for (i = 0; i < 9; i++) {
    y[i + 137] = d0 - X[126 - i];
  }

  for (i = 0; i < 4; i++) {
    dv0[i] = dv1[i];
    dv2[i] = dv3[i];
  }

  for (i = 0; i < 3; i++) {
    a[i] = b_a[i] * y[0];
  }

  memcpy(&b_y[0], &y[0], 146U * sizeof(double));
  filter(dv0, dv2, b_y, a, y);
  for (i = 0; i < 73; i++) {
    xtmp = y[i];
    y[i] = y[145 - i];
    y[145 - i] = xtmp;
  }

  for (i = 0; i < 4; i++) {
    dv0[i] = dv1[i];
    dv2[i] = dv3[i];
  }

  for (i = 0; i < 3; i++) {
    a[i] = b_a[i] * y[0];
  }

  memcpy(&b_y[0], &y[0], 146U * sizeof(double));
  filter(dv0, dv2, b_y, a, y);
  for (i = 0; i < 73; i++) {
    xtmp = y[i];
    y[i] = y[145 - i];
    y[145 - i] = xtmp;
  }

  for (i = 0; i < 128; i++) {
    Y[i] = (float)(y[i + 9] * scale_factor);
  }
}

//
// Arguments    : void
// Return Type  : void
//
void emg_hpf_upscale_initialize()
{
  rt_InitInfAndNaN(8U);
}

//
// Arguments    : void
// Return Type  : void
//
void emg_hpf_upscale_terminate()
{
  // (no terminate code required)
}

//
// File trailer for emg_hpf_upscale.cpp
//
// [EOF]
//
