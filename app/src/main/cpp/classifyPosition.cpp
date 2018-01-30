//
// Academic License - for use in teaching, academic research, and meeting
// course requirements at degree granting institutions only.  Not for
// government, commercial, or other organizational use.
// File: classifyPosition.cpp
//
// MATLAB Coder version            : 3.3
// C/C++ source code generated on  : 30-Jan-2018 14:44:21
//

// Include Files
#include "rt_nonfinite.h"
#include "classifyPosition.h"

// Function Definitions

//
// x,y,z are [50x1] vectors:
// Arguments    : const double x[50]
//                const double y[50]
//                const double z[50]
// Return Type  : double
//
double classifyPosition(const double [50], const double [50], const double z[50],
                        double max_threshold, double min_threshold)
{
  double C;
  int i;
  double b_y;
  boolean_T b_x[50];
  for (i = 0; i < 50; i++) {
    b_x[i] = (z[i] > max_threshold/*0.80*/);
  }

  b_y = b_x[0];
  for (i = 0; i < 49; i++) {
    b_y += (double)b_x[i + 1];
  }

  if (b_y >= 26.0) {
    C = 4.0;
  } else {
    for (i = 0; i < 50; i++) {
      b_x[i] = (z[i] < min_threshold/*0.40*/);
    }

    b_y = b_x[0];
    for (i = 0; i < 49; i++) {
      b_y += (double)b_x[i + 1];
    }

    if (b_y >= 26.0) {
      C = 5.0;
    } else {
      C = 0.0;
    }
  }

  return C;
}

//
// Arguments    : void
// Return Type  : void
//
void classifyPosition_initialize()
{
  rt_InitInfAndNaN(8U);
}

//
// Arguments    : void
// Return Type  : void
//
void classifyPosition_terminate()
{
  // (no terminate code required)
}

//
// File trailer for classifyPosition.cpp
//
// [EOF]
//
