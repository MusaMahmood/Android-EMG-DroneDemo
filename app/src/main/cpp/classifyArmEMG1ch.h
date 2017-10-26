//
// Academic License - for use in teaching, academic research, and meeting
// course requirements at degree granting institutions only.  Not for
// government, commercial, or other organizational use.
// File: classifyArmEMG1ch.h
//
// MATLAB Coder version            : 3.3
// C/C++ source code generated on  : 26-Oct-2017 16:55:36
//
#ifndef CLASSIFYARMEMG1CH_H
#define CLASSIFYARMEMG1CH_H

// Include Files
#include <cmath>
#include <math.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include "rt_nonfinite.h"
#include "rtwtypes.h"
#include "classifyArmEMG1ch_types.h"

// Function Declarations
extern double classifyArmEMG1ch(const double rawData[500], const double F[30315]);
extern void classifyArmEMG1ch_initialize();
extern void classifyArmEMG1ch_terminate();

#endif

//
// File trailer for classifyArmEMG1ch.h
//
// [EOF]
//
