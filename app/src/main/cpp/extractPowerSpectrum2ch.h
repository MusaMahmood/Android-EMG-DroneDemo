//
// Academic License - for use in teaching, academic research, and meeting
// course requirements at degree granting institutions only.  Not for
// government, commercial, or other organizational use.
// File: extractPowerSpectrum2ch.h
//
// MATLAB Coder version            : 3.3
// C/C++ source code generated on  : 27-Sep-2017 17:54:02
//
#ifndef EXTRACTPOWERSPECTRUM2CH_H
#define EXTRACTPOWERSPECTRUM2CH_H

// Include Files
#include <cmath>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include "rt_nonfinite.h"
#include "rtwtypes.h"
#include "extractPowerSpectrum2ch_types.h"

// Function Declarations
extern void extractPowerSpectrum2ch(const double X1[500], const double X2[500],
  double PSD[500]);
extern void extractPowerSpectrum2ch_initialize();

#endif

//
// File trailer for extractPowerSpectrum2ch.h
//
// [EOF]
//
