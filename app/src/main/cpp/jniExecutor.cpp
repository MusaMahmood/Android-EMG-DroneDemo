//
// Created by mahmoodms on 4/3/2017.
//

#include "rt_nonfinite.h"
#include "classifySSVEP.h"
#include "extractPowerSpectrum.h"
#include "ssvep_filter_f32.h"
#include "tf_psd_rescale_w256.h"
#include "tf_psd_rescale_w384.h"
#include "tf_psd_rescale_w512.h"

/*Additional Includes*/
#include <jni.h>
#include <android/log.h>

#define  LOG_TAG "jniExecutor-cpp"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Function Definitions
extern "C" {
JNIEXPORT jfloatArray JNICALL
Java_com_yeolabgt_mahmoodms_ssvepinterfacetf_NativeInterfaceClass_jSSVEPCfilter(
        JNIEnv *env, jobject jobject1, jdoubleArray data) {
    jdouble *X1 = env->GetDoubleArrayElements(data, NULL);
    float Y[1000]; // First two values = Y; last 499 = cPSD
    if (X1 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    jfloatArray m_result = env->NewFloatArray(1000);
    ssvep_filter_f32(X1, Y);
    env->SetFloatArrayRegion(m_result, 0, 1000, Y);
    return m_result;
}
}

extern "C" {
JNIEXPORT jdoubleArray JNICALL
Java_com_yeolabgt_mahmoodms_ssvepinterfacetf_NativeInterfaceClass_jClassifySSVEP(
        JNIEnv *env, jobject jobject1, jdoubleArray ch1, jdoubleArray ch2, jdouble threshold) {

    jdouble *X1 = env->GetDoubleArrayElements(ch1, NULL);
    jdouble *X2 = env->GetDoubleArrayElements(ch2, NULL);
    double Y[2]; // First two values = Y; last 499 = cPSD
    double PSD[499]; // First two values = Y; last 499 = cPSD
    if (X1 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    if (X2 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    classifySSVEP(X1, X2, threshold, &Y[0], &PSD[0]);
    jdoubleArray m_result = env->NewDoubleArray(2);
    env->SetDoubleArrayRegion(m_result, 0, 2, Y);
    return m_result;
}
}

extern "C" {
JNIEXPORT jfloatArray JNICALL
Java_com_yeolabgt_mahmoodms_ssvepinterfacetf_NativeInterfaceClass_jTFPSDExtraction(
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
Java_com_yeolabgt_mahmoodms_ssvepinterfacetf_NativeInterfaceClass_jPSDExtraction(
        JNIEnv *env, jobject jobject1, jdoubleArray ch1, jdoubleArray ch2, jint sampleRate, jint length) {
    jdouble *X1 = env->GetDoubleArrayElements(ch1, NULL); if (X1 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    jdouble *X2 = env->GetDoubleArrayElements(ch2, NULL); if (X2 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    if(length==0) {
        LOGE("ERROR: LENGTH INVALID");
        return nullptr;
    } else {
        jdoubleArray m_result = env->NewDoubleArray(length);
        double Y[length]; //length/2*2=Divide by two for normal length, but we are looking at 2 vectors.
        int PSD_size[2];
        extractPowerSpectrum(X1, &length, X2, &length, sampleRate, &Y[0], PSD_size);
        env->SetDoubleArrayRegion(m_result, 0, length, Y);
        return m_result;
    }
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
Java_com_yeolabgt_mahmoodms_ssvepinterfacetf_NativeInterfaceClass_jLoadfPSD(
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
Java_com_yeolabgt_mahmoodms_ssvepinterfacetf_NativeInterfaceClass_jmainInitialization(
        JNIEnv *env, jobject obj, jboolean terminate) {
    if (!(bool) terminate) {
        classifySSVEP_initialize(); //Only need to call once.
//        extractPowerSpectrum_initialize();
        return 0;
    } else {
        return -1;
    }
}
}
