//
// Academic License - for use in teaching, academic research, and meeting
// course requirements at degree granting institutions only.  Not for
// government, commercial, or other organizational use.
// File: emg_hpf_upscale.h
//
// MATLAB Coder version            : 3.3
// C/C++ source code generated on  : 26-Jan-2018 12:37:32
//
#ifndef EMG_HPF_UPSCALE_H
#define EMG_HPF_UPSCALE_H

// Include Files
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include "rt_nonfinite.h"
#include "rtwtypes.h"
#include "emg_hpf_upscale_types.h"

// Function Declarations
extern void emg_hpf_upscale(const double X[128], float Y[128], double scale_factor);
extern void emg_hpf_upscale_initialize();
extern void emg_hpf_upscale_terminate();

#endif

//
// File trailer for emg_hpf_upscale.h
//
// [EOF]
//
