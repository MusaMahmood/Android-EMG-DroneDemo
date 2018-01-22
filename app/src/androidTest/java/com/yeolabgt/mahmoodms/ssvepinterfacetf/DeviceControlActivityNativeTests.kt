package com.yeolabgt.mahmoodms.ssvepinterfacetf

import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Created by Musa Mahmood on 1/14/2018.
 *
 */

@RunWith(AndroidJUnit4::class)
class ClassifySSVEPTests {

    private val mNativeInterface = NativeInterfaceClass()

    @Test
    fun testNativeInitialization() {
        Assert.assertEquals(mNativeInterface.jmainInitialization(false), 0)
    }

    @Test
    fun jClassifyWorks() {
        val a = DoubleArray(1000)
        Arrays.fill(a, 0.0)
        val b = DoubleArray(1000)
        Arrays.fill(b, 0.0)
        val c = mNativeInterface.jClassifySSVEP(a, b, 1.0)
        Log.d("ClassifySSVEPTests", Arrays.toString(c))
        Assert.assertEquals(c[0].toInt(), 0.0.toInt())
    }
}