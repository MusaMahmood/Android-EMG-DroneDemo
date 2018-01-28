//
// Created by mahmoodms on 4/3/2017.
//

#include "rt_nonfinite.h"
#include "tf_psd_rescale_w256.h"
#include "tf_psd_rescale_w384.h"
#include "tf_psd_rescale_w512.h"
#include "emg_hpf_upscale.h"
#include "getRescaleFactor.h"

/*Additional Includes*/
#include <jni.h>
#include <android/log.h>

#define  LOG_TAG "jniExecutor-cpp"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void rescale_minmax_floats(const float X[], float Y[], const int size)
{
    int ixstart;
    float mtmp;
    int ix;
    boolean_T exitg1;
    float b_mtmp;
    ixstart = 1;
    mtmp = X[0];
    if (rtIsNaN(X[0])) {
        ix = 2;
        exitg1 = false;
        while ((!exitg1) && (ix < size + 1)) {
            ixstart = ix;
            if (!rtIsNaN(X[ix - 1])) {
                mtmp = X[ix - 1];
                exitg1 = true;
            } else {
                ix++;
            }
        }
    }

    if (ixstart < size) {
        while (ixstart + 1 < size + 1) {
            if (X[ixstart] < mtmp) {
                mtmp = X[ixstart];
            }
            ixstart++;
        }
    }

    ixstart = 1;
    b_mtmp = X[0];
    if (rtIsNaN(X[0])) {
        ix = 2;
        exitg1 = false;
        while ((!exitg1) && (ix < size + 1)) {
            ixstart = ix;
            if (!rtIsNaN(X[ix - 1])) {
                b_mtmp = X[ix - 1];
                exitg1 = true;
            } else {
                ix++;
            }
        }
    }

    if (ixstart < size) {
        while (ixstart + 1 < size+1) {
            if (X[ixstart] > b_mtmp) {
                b_mtmp = X[ixstart];
            }

            ixstart++;
        }
    }

    b_mtmp -= mtmp;
    for (ixstart = 0; ixstart < size; ixstart++) {
        Y[ixstart] = (X[ixstart] - mtmp) / b_mtmp;
    }
}

extern "C" {
JNIEXPORT jfloatArray JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_NativeInterfaceClass_jfiltRescale(
        JNIEnv *env, jobject jobject1, jdoubleArray data, jdouble scale_factor) {
    jdouble *X = env->GetDoubleArrayElements(data, NULL);
    if (X == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    float Y[128];
    jfloatArray m_result = env->NewFloatArray(128);
    emg_hpf_upscale(X, Y, scale_factor);
    env->SetFloatArrayRegion(m_result, 0, 128, Y);
    return m_result;
}
}

extern "C" {
JNIEXPORT jfloatArray JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_NativeInterfaceClass_jrescaleMinmax(
        JNIEnv *env, jobject jobject1, jfloatArray data, jint size) {
    jfloat *X = env->GetFloatArrayElements(data, NULL);
    if (X == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    float Y[size];
    jfloatArray m_result = env->NewFloatArray(size);
    rescale_minmax_floats(X, Y, size);
    env->SetFloatArrayRegion(m_result, 0, size, Y);
    return m_result;
}
}

extern "C" {
JNIEXPORT jfloatArray JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_NativeInterfaceClass_jTFPSDExtraction(
        JNIEnv *env, jobject jobject1, jdoubleArray ch1_2_data, jint length) {
    jdouble *X = env->GetDoubleArrayElements(ch1_2_data, NULL); if (X == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    jfloatArray m_result = env->NewFloatArray(length);
    float Y[length]; //length/2*2=Divide by two for normal length, but we are looking at 2 vectors.
    if (length == 256)
        tf_psd_rescale_w256(X, Y);
    else if (length == 384)
        tf_psd_rescale_w384(X, Y);
    else if (length == 512)
        tf_psd_rescale_w512(X, Y);
    env->SetFloatArrayRegion(m_result, 0, length, Y);
    return m_result;
}
}

extern "C" {
JNIEXPORT jdoubleArray JNICALL
/**
 *
 * @param env
 * @param jobject1
 * @return array of frequencies (Hz) corresponding to a raw input signal.
 */
Java_com_yeolabgt_mahmoodms_emgdronedemo_NativeInterfaceClass_jLoadfPSD(
        JNIEnv *env, jobject jobject1, jint sampleRate, jint win_length) {
    double fPSD[win_length/2];
    for (int i = 0; i < win_length/2; i++) {
        fPSD[i] = (double) i * (double) sampleRate / (double) win_length;
    }
    jdoubleArray m_result = env->NewDoubleArray(win_length/2);
    env->SetDoubleArrayRegion(m_result, 0, win_length/2, fPSD);
    return m_result;
}
}

extern "C" {
JNIEXPORT jint JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_NativeInterfaceClass_jmainInitialization(
        JNIEnv *env, jobject obj, jboolean terminate) {
    if (!(bool) terminate) {
        emg_hpf_upscale_initialize();
        return 0;
    } else {
        return -1;
    }
}
}
