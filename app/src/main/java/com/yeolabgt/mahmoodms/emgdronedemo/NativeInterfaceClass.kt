package com.yeolabgt.mahmoodms.emgdronedemo

/**
 * Created by Musa Mahmood on 1/14/2018.
 *
 */

class NativeInterfaceClass {


    @Throws(IllegalArgumentException::class)
    external fun jmainInitialization(b: Boolean): Int
    @Throws(IllegalArgumentException::class)
    external fun jrescaleMinmax(data: FloatArray, size: Int): FloatArray
    @Throws(IllegalArgumentException::class)
    external fun jfiltRescale(data: DoubleArray, scaleFactor: Double): FloatArray

    companion object {
        init {
            System.loadLibrary("ssvep-lib")
        }
    }
}