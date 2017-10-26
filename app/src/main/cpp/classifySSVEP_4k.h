//
// Academic License - for use in teaching, academic research, and meeting
// course requirements at degree granting institutions only.  Not for
// government, commercial, or other organizational use.
// File: classifySSVEP_4k.h
//
// MATLAB Coder version            : 3.3
// C/C++ source code generated on  : 05-Oct-2017 15:39:19
//
#ifndef CLASSIFYSSVEP_4K_H
#define CLASSIFYSSVEP_4K_H

// Include Files
#include <cmath>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include "rt_nonfinite.h"
#include "rtwtypes.h"
#include "classifySSVEP_4k_types.h"

// Function Declarations
extern void classifySSVEP_4k(const double X1[8000], const double X2[8000],
  double thresholdFraction, double Y[2]);
extern void classifySSVEP_4k_initialize();
extern void classifySSVEP_4k_terminate();

#endif

//
// File trailer for classifySSVEP_4k.h
//
// [EOF]
//
