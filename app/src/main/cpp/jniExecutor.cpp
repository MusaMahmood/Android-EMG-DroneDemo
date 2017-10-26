//
// Created by mahmoodms on 4/3/2017.
//

#include "rt_nonfinite.h"
#include "classifySSVEP.h"
#include "extractPowerSpectrum2ch.h"
#include "extractPowerSpectrum.h"
#include "classifySSVEP_4k.h"
/*Additional Includes*/
#include <jni.h>
#include <android/log.h>

#define  LOG_TAG "jniExecutor-cpp"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Function Definitions

extern "C" {
JNIEXPORT jdoubleArray JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_DeviceControlActivity_jClassifySSVEP(
        JNIEnv *env, jobject jobject1, jdoubleArray ch1, jdoubleArray ch2, jdouble threshold) {

    jdouble *X1 = env->GetDoubleArrayElements(ch1, NULL);
    jdouble *X2 = env->GetDoubleArrayElements(ch2, NULL);
    double Y[501]; // First two values = Y; last 499 = cPSD
    if (X1 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    if (X2 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    jdoubleArray m_result = env->NewDoubleArray(501);
    classifySSVEP(X1, X2, threshold, &Y[0], &Y[2]);
    env->SetDoubleArrayRegion(m_result, 0, 501, Y);
    return m_result;
}
}

extern "C" {
JNIEXPORT jdoubleArray JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_DeviceControlActivity_jClassifySSVEP4k(
        JNIEnv *env, jobject jobject1, jdoubleArray ch1, jdoubleArray ch2, jdouble threshold) {
    jdouble *X1 = env->GetDoubleArrayElements(ch1, NULL);
    jdouble *X2 = env->GetDoubleArrayElements(ch2, NULL);
    double Y[2]; // First two values = Y; last 499 = cPSD
    if (X1 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    if (X2 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    jdoubleArray m_result = env->NewDoubleArray(2);
    classifySSVEP_4k(X1, X2, threshold, &Y[0]);
    env->SetDoubleArrayRegion(m_result, 0, 2, Y);
    return m_result;
}
}

extern "C" {
JNIEXPORT jdoubleArray JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_DeviceControlActivity_jPSDExtraction(
        JNIEnv *env, jobject jobject1, jdoubleArray ch1, jdoubleArray ch2, jint sampleRate) {
    jdouble *X1 = env->GetDoubleArrayElements(ch1, NULL);
    jdouble *X2 = env->GetDoubleArrayElements(ch2, NULL);
    if (X1 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    if (X2 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    jdoubleArray m_result = env->NewDoubleArray(sampleRate*2);
    int size_array = sampleRate*2; //2s of data (X1 and X2)
    double Y[sampleRate*2];
    int PSD_size[2];
    extractPowerSpectrum(X1, &size_array, X2, &size_array, sampleRate, &Y[0], PSD_size);
    env->SetDoubleArrayRegion(m_result, 0, sampleRate*2, Y);
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
Java_com_yeolabgt_mahmoodms_emgdronedemo_DeviceControlActivity_jLoadfPSD(
        JNIEnv *env, jobject jobject1, jint sampleRate) {
    jdoubleArray m_result = env->NewDoubleArray(sampleRate);
    double fPSD[sampleRate];
    for (int i = 0; i < sampleRate; i++) {
        fPSD[i] = (double)i * (double)sampleRate / (double) (sampleRate*2);
    }
    env->SetDoubleArrayRegion(m_result, 0, sampleRate, fPSD);
    return m_result;
}
}

extern "C" {
JNIEXPORT jint JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_DeviceControlActivity_jmainInitialization(
        JNIEnv *env, jobject obj, jboolean terminate) {
    if (!(bool) terminate) {
        classifySSVEP_initialize();
        extractPowerSpectrum2ch_initialize();
        extractPowerSpectrum_initialize();
        classifySSVEP_4k_initialize();
        return 0;
    } else {
        return -1;
    }
}
}
