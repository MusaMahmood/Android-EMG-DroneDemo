//
// Academic License - for use in teaching, academic research, and meeting
// course requirements at degree granting institutions only.  Not for
// government, commercial, or other organizational use.
// File: getRescaleFactor.h
//
// MATLAB Coder version            : 3.3
// C/C++ source code generated on  : 28-Jan-2018 16:40:23
//
#ifndef GETRESCALEFACTOR_H
#define GETRESCALEFACTOR_H

// Include Files
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include "rt_nonfinite.h"
#include "rtwtypes.h"
#include "getRescaleFactor_types.h"

// Function Declarations
extern double getRescaleFactor(const double X[1000], double mean_p2p);
extern double getPeak2PeakVoltage(const double X[1000]);
extern void getRescaleFactor_initialize();
extern void getRescaleFactor_terminate();

#endif

//
// File trailer for getRescaleFactor.h
//
// [EOF]
//
