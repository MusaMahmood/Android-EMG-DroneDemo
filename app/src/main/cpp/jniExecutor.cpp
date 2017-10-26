//
// Created by mahmoodms on 4/3/2017.
//

#include "rt_nonfinite.h"
#include "ctrainingRoutine1ch.h"
#include "classifyArmEMG1ch.h"
/*Additional Includes*/
#include <jni.h>
#include <android/log.h>

#define  LOG_TAG "jniExecutor-cpp"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {
JNIEXPORT jdouble JNICALL
/**
 *
 * @param env
 * @param jobject1
 * @param allData 750x3 vector of data
 * @param params KNN features 1x4960
 * @param LastY
 * @return
 */
Java_com_yeolabgt_mahmoodms_emgdronedemo_DeviceControlActivity_jClassifyUsingKNN(
        JNIEnv *env, jobject jobject1, jdoubleArray allData, jdoubleArray params) {
    jdouble *X1 = env->GetDoubleArrayElements(allData, NULL);
    jdouble *PARAMS = env->GetDoubleArrayElements(params, NULL);
    if (X1 == NULL) LOGE("ERROR - C_ARRAY IS NULL");
    return classifyArmEMG1ch(X1, PARAMS);
}
}

extern "C" {
JNIEXPORT jdoubleArray JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_DeviceControlActivity_jTrainingRoutine(JNIEnv *env, jobject obj, jdoubleArray allData) {
    jdouble *X = env->GetDoubleArrayElements(allData, NULL);
    if (X==NULL) LOGE("ERROR - C_ARRAY");
    double KNNPARAMS[30315];
    ctrainingRoutine1ch(X, KNNPARAMS); //Returns features.
    jdoubleArray mReturnArray = env->NewDoubleArray(30315);
    env->SetDoubleArrayRegion(mReturnArray, 0, 30315, &KNNPARAMS[0]);
    return mReturnArray;
}
}

// Function Definitions
extern "C" {
JNIEXPORT jint JNICALL
Java_com_yeolabgt_mahmoodms_emgdronedemo_DeviceControlActivity_jmainInitialization(
        JNIEnv *env, jobject obj, jboolean terminate) {
    if (!(bool) terminate) {
        ctrainingRoutine1ch_initialize();
        classifyArmEMG1ch_initialize();
        return 0;
    } else {
        return -1;
    }
}
}
