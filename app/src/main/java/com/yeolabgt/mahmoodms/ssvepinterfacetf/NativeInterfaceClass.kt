package com.yeolabgt.mahmoodms.ssvepinterfacetf

/**
 * Created by Musa Mahmood on 1/14/2018.
 *
 */

class NativeInterfaceClass {


    @Throws(IllegalArgumentException::class)
    external fun jSSVEPCfilter(data: DoubleArray): FloatArray
    @Throws(IllegalArgumentException::class)
    external fun jmainInitialization(b: Boolean): Int
    @Throws(IllegalArgumentException::class)
    external fun jClassifySSVEP(a: DoubleArray, b: DoubleArray, c: Double): DoubleArray
    @Throws(IllegalArgumentException::class)
    external fun jPSDExtraction(a: DoubleArray, b: DoubleArray, sampleRate: Int, len: Int): DoubleArray
    @Throws(IllegalArgumentException::class)
    external fun jTFPSDExtraction(a: DoubleArray, length: Int): FloatArray
    @Throws(IllegalArgumentException::class)
    external fun jLoadfPSD(sampleRate: Int, windowLength: Int): DoubleArray

    companion object {
        init {
            System.loadLibrary("ssvep-lib")
        }
    }
}