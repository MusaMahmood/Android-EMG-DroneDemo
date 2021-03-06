package com.yeolabgt.mahmoodms.emgdronedemo

import android.os.Environment
import android.util.Log

import com.opencsv.CSVWriter

import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Created by mmahmood31 on 10/19/2017.
 * Kotlin class for organizing and storing data to CSV.
 */

internal class SaveDataFile @Throws(IOException::class)
constructor(directory: String, fileName: String, byteResolution: Int, increment: Double) {
    //Math Stuff
    private var mLinesWritten: Int = 0 //for timestamp
    private var mIncrement: Double = 0.toDouble()
    private var fpPrecision: Short = 64 //float vs double (default)
    var resolutionBits: Int = 0
        private set
    private var includeClass = true //Saves by default, no need to change.
    private var saveTimestamps = false
    var initialized = false
    private var csvWriter: CSVWriter? = null
    lateinit var file: File

    init {
        val root = Environment.getExternalStorageDirectory()
        if (root.canWrite()) {
            val dir = File(root.absolutePath + directory)
            val resultMkdir = dir.mkdirs()
            if (!resultMkdir) {
                Log.e(TAG, "MKDIRS FAILED")
            }
            this.file = File(dir, fileName + ".csv")
            if (this.file.exists() && !this.file.isDirectory) {
                Log.d(TAG, "File " + this.file.toString()
                        + " already exists - appending data")
                val fileWriter = FileWriter(this.file, true)
                this.csvWriter = CSVWriter(fileWriter)
            } else {
                this.csvWriter = CSVWriter(FileWriter(this.file))
            }
            this.resolutionBits = byteResolution
            this.mIncrement = increment
            this.initialized = true
        }
    }

    fun setFpPrecision(fpPrecision: Short) {
        this.fpPrecision = fpPrecision
    }

    fun setSaveTimestamps(saveTimestamps: Boolean) {
        this.saveTimestamps = saveTimestamps
    }

    fun setIncludeClass(includeClass: Boolean) {
        this.includeClass = includeClass
    }

    fun writeToDisk(vararg byteArrays: ByteArray?) {
        if (this.fpPrecision.toInt() == 64) {
            writeToDiskDouble(*byteArrays)
        } else if (this.fpPrecision.toInt() == 32) {
            writeToDiskFloat(*byteArrays)
        }
    }

    /**
     *
     * @param bytes split into 6 colns:
     */
    fun exportDataWithTimestampMPU(bytes: ByteArray?) {
        for (i in 0 until bytes!!.size / 12) {
            val ax = DataChannel.bytesToDoubleMPUAccel(bytes[12 * i], bytes[12 * i + 1])
            val ay = DataChannel.bytesToDoubleMPUAccel(bytes[12 * i + 2], bytes[12 * i + 3])
            val az = DataChannel.bytesToDoubleMPUAccel(bytes[12 * i + 4], bytes[12 * i + 5])
            val gx = DataChannel.bytesToDoubleMPUGyro(bytes[12 * i + 6], bytes[12 * i + 7])
            val gy = DataChannel.bytesToDoubleMPUGyro(bytes[12 * i + 8], bytes[12 * i + 9])
            val gz = DataChannel.bytesToDoubleMPUGyro(bytes[12 * i + 10], bytes[12 * i + 11])
            exportDataDouble(ax, ay, az, gx, gy, gz)
        }
    }

    private fun writeToDiskFloat(vararg byteArrays: ByteArray?) {
        val len = byteArrays.size // Number of channels
        val floats = if (this.resolutionBits==16)
            Array(len){FloatArray(byteArrays[0]!!.size/2)}
            else Array(len){FloatArray(byteArrays[0]!!.size/3)}
        for (ch in 0 until len) { // each channel
            if (this.resolutionBits == 16) {
                for (dp in 0 until byteArrays[ch]!!.size / 2) { // each datapoint
                    floats[ch][dp] = DataChannel.bytesToFloat32(byteArrays[ch]!![2*dp],
                            byteArrays[ch]!![2 * dp + 1])
                }
            } else if (this.resolutionBits == 24) {
                for (dp in 0 until byteArrays[ch]!!.size / 3) {
                    floats[ch][dp] = DataChannel.bytesToFloat32(byteArrays[ch]!![3 * dp],
                            byteArrays[ch]!![3 * dp + 1], byteArrays[ch]!![3 * dp + 2])
                }
            }
        }
        try {
            exportFile(*floats)
        } catch (e: IOException) {
            Log.e("IOException", e.toString())
        }
    }

    private fun writeToDiskDouble(vararg byteArrays: ByteArray?) {
        val len = byteArrays.size // Number of channels
        val doubles = if (this.resolutionBits==16)
            Array(len){DoubleArray(byteArrays[0]!!.size/2)}
            else Array(len) {DoubleArray(byteArrays[0]!!.size/3)}
        for (ch in 0 until len) { // each channel
            if (this.resolutionBits == 16) {
                for (dp in 0 until byteArrays[ch]!!.size / 2) { // each datapoint
                    doubles[ch][dp] = DataChannel.bytesToDouble(byteArrays[ch]!![2*dp],
                            byteArrays[ch]!![2 * dp + 1])
                }
            } else if (this.resolutionBits == 24) {
                for (dp in 0 until byteArrays[ch]!!.size / 3) {
                    doubles[ch][dp] = DataChannel.bytesToDouble(byteArrays[ch]!![3 * dp],
                            byteArrays[ch]!![3 * dp + 1], byteArrays[ch]!![3 * dp + 2])
                }
            }
        }
        try {
            exportFile(*doubles)
        } catch (e: IOException) {
            Log.e("IOException", e.toString())
        }

    }

    @Throws(IOException::class)
    private fun exportFile(vararg floats: FloatArray) {
        val numDp = floats[0].size
        val numChannels = floats.size
        val columns = numChannels + ((if (this.includeClass) 1 else 0)
                + if (this.saveTimestamps) 1 else 0)
        val writeCSVValue: Array<Array<String?>>
        writeCSVValue = Array(numDp) { arrayOfNulls<String>(columns) }
        for (dp in 0 until numDp) {
            if (this.saveTimestamps) {
                if (fpPrecision.toInt() == 64)
                    writeCSVValue[dp][0] = (mLinesWritten.toDouble() * mIncrement).toString() + ""
                else
                    writeCSVValue[dp][0] = (mLinesWritten.toFloat() * mIncrement).toString() + ""
            }
            for (ch in 0 until numChannels) {
                if (!this.saveTimestamps)
                    writeCSVValue[dp][ch] = floats[ch][dp].toString() + ""
                else
                    writeCSVValue[dp][ch + 1] = floats[ch][dp].toString() + ""
            }
            if (this.includeClass) {
                writeCSVValue[dp][columns - 1] = DeviceControlActivity.mEMGClass.toString() + ""
            }
            csvWriter!!.writeNext(writeCSVValue[dp], false)
            this.mLinesWritten++
        }
    }

    /**
     * Writes 6 data points + timestamp
     * @param a accx
     * @param b accy
     * @param c accz
     * @param d gyrx
     * @param e gyry
     * @param f gyrz
     */
    fun exportDataDouble(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
        val writeCSVValue = arrayOfNulls<String>(7)
        val timestamp = mLinesWritten.toDouble() * mIncrement
        writeCSVValue[0] = timestamp.toString() + ""
        writeCSVValue[1] = a.toString() + ""
        writeCSVValue[2] = b.toString() + ""
        writeCSVValue[3] = c.toString() + ""
        writeCSVValue[4] = d.toString() + ""
        writeCSVValue[5] = e.toString() + ""
        writeCSVValue[6] = f.toString() + ""
        this.csvWriter!!.writeNext(writeCSVValue, false)
        this.mLinesWritten++
    }

    @Throws(IOException::class)
    private fun exportFile(vararg doubles: DoubleArray) {
        val numDp = doubles[0].size
        val numChannels = doubles.size
        val columns = numChannels + ((if (this.includeClass) 1 else 0)
                + if (this.saveTimestamps) 1 else 0)
        val writeCSVValue: Array<Array<String?>>
        writeCSVValue = Array(numDp) { arrayOfNulls<String>(columns) }
        for (dp in 0 until numDp) {
            if (this.saveTimestamps) {
                if (fpPrecision.toInt() == 64)
                    writeCSVValue[dp][0] = (mLinesWritten.toDouble() * mIncrement).toString() + ""
                else
                    writeCSVValue[dp][0] = (mLinesWritten.toFloat() * mIncrement).toString() + ""
            }
            for (ch in 0 until numChannels) {
                if (!this.saveTimestamps)
                    writeCSVValue[dp][ch] = doubles[ch][dp].toString() + ""
                else
                    writeCSVValue[dp][ch + 1] = doubles[ch][dp].toString() + ""
            }
            if (this.includeClass) {
                writeCSVValue[dp][columns - 1] = DeviceControlActivity.mEMGClass.toString() + ""
            }
            csvWriter!!.writeNext(writeCSVValue[dp], false)
            this.mLinesWritten++
        }
    }

    @Throws(IOException::class)
    fun terminateDataFileWriter() {
        if (this.initialized) {
            this.csvWriter!!.flush()
            this.csvWriter!!.close()
            this.initialized = false
            this.mLinesWritten = 0
        }
    }

    companion object {

        private val TAG = SaveDataFile::class.java.simpleName
    }
}
