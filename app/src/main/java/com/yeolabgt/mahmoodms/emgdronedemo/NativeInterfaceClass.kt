package com.yeolabgt.mahmoodms.emgdronedemo

/**
 * Created by Musa Mahmood on 1/14/2018.
 *
 */

class NativeInterfaceClass {


    @Throws(IllegalArgumentException::class)
    external fun jmainInitialization(b: Boolean): Int
    @Throws(IllegalArgumentException::class)
    external fun jfiltRescale(data: DoubleArray, scaleFactor: Double): FloatArray
    @Throws(IllegalArgumentException::class)
    external fun jclassifyPosition(x: DoubleArray, y: DoubleArray, z: DoubleArray,
                                   max_threshold: Double, min_threshold: Double): Double
    @Throws(IllegalArgumentException::class)
    external fun jgetPeak2PeakVoltage(X: DoubleArray): Double // X_size = [1000 x 1]

    companion object {
        init {
            System.loadLibrary("ssvep-lib")
        }
    }
}