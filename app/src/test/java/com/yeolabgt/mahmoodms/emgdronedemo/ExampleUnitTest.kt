package com.yeolabgt.mahmoodms.emgdronedemo

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun array_test() {
        val a = intArrayOf(0,0,0,0,0)
        assert(allValuesSame(a))


    }

    @Test
    fun array_test2() {
        val b = intArrayOf(0,0,1,0,0)
        assertEquals(false, allValuesSame(b))
    }

    @Test
    fun array_test3() {
        val v = intArrayOf(3,3,3,3,3)
        assert(allValuesSame(v))
    }

    private fun allValuesSame(y: IntArray): Boolean {
        return y.map { it == y[0] }.find { it == false } == null
    }

    @Test
    private fun filterData() {
        val fA = DoubleArray(128)
        val mNIC = NativeInterfaceClass()
        val p = mNIC.jfiltRescale(fA, 20.0)
    }
}
