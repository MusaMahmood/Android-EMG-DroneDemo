package com.yeolabgt.mahmoodms.ssvepinterfacetf

import com.google.common.primitives.Bytes

/**
 * Created by mmahmood31 on 9/19/2017.
 * For Handling BLE incoming data packets.
 */

internal class DataChannel(var chEnabled: Boolean, MSBFirst: Boolean, //Classification:
                           classificationBufferSize: Int) {
    var characteristicDataPacketBytes: ByteArray? = null
    var packetCounter: Short = 0
    var totalDataPointsReceived: Int = 0
    var dataBuffer: ByteArray? = null
    var classificationBufferSize: Int = 1000
        set(value) {
            this.classificationBuffer = DoubleArray(value)
            this.classificationBufferFloats = FloatArray(value)
            field = value
        }
    var classificationBuffer: DoubleArray
    var classificationBufferFloats: FloatArray

    init {
        this.packetCounter = 0
        this.totalDataPointsReceived = 0
        this.classificationBufferSize = classificationBufferSize
        this.classificationBuffer = DoubleArray(classificationBufferSize)
        this.classificationBufferFloats = FloatArray(classificationBufferSize)
        Companion.MSBFirst = MSBFirst
    }

    /**
     * If 'dataBuffer' is not null, concatenate new data using Guava lib
     * else: initialize dataBuffer with new data.
     *
     * @param newDataPacket new data packet received via BLE>
     */
    fun handleNewData(newDataPacket: ByteArray) {
        this.characteristicDataPacketBytes = newDataPacket
        if (this.dataBuffer != null) {
            this.dataBuffer = Bytes.concat(this.dataBuffer, newDataPacket)
        } else {
            this.dataBuffer = newDataPacket
        }
        for (i in 0 until newDataPacket.size / 3) {
            addToBuffer(bytesToDouble(newDataPacket[3 * i], newDataPacket[3 * i + 1], newDataPacket[3 * i + 2]))
        }
        this.totalDataPointsReceived += newDataPacket.size / 3
        this.packetCounter++
    }

    private fun addToBuffer(a: Double) {
        if (this.classificationBufferSize > 0) {
            System.arraycopy(this.classificationBuffer, 1, this.classificationBuffer, 0, this.classificationBufferSize - 1) //shift backwards
            System.arraycopy(this.classificationBufferFloats, 1, this.classificationBufferFloats, 0, this.classificationBufferSize - 1) //shift backwards
            this.classificationBuffer[this.classificationBufferSize - 1] = a //add to front:
            this.classificationBufferFloats[this.classificationBufferSize - 1] = a.toFloat()
        }
    }

    companion object {
        private val HEX_CHARS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                        'A', 'B', 'C', 'D', 'E', 'F')

        fun byteArrayToHexString(byteArray: ByteArray) : String = byteArray.toHex()

        private fun ByteArray.toHex() : String {
            val result = StringBuffer()

            forEach {
                val octet = it.toInt()
                val firstIndex = (octet and 0xF0).ushr(4)
                val secondIndex = octet and 0x0F
                result.append(HEX_CHARS[firstIndex])
                result.append(HEX_CHARS[secondIndex])
            }

            return result.toString()
        }

        fun getIndexOfLargest(array: FloatArray?): Int {
            if (array == null || array.isEmpty()) return -1 // null or empty

            var largest = 0
            for (i in 1 until array.size) {
                if (array[i] > array[largest]) largest = i
            }
            return largest // position of the first largest found
        }

        private var MSBFirst: Boolean = false

        fun bytesToFloat32(a1: Byte, a2: Byte, a3: Byte): Float {
            val unsigned = unsignedBytesToInt(a1, a2, a3, MSBFirst)
            return unsignedToSigned24bit(unsigned).toFloat() / 8388607.0.toFloat() * 2.25.toFloat()
        }

        fun bytesToFloat32(a1: Byte, a2: Byte): Float {
            val unsigned = unsignedBytesToInt(a1, a2, MSBFirst)
            return unsignedToSigned16bit(unsigned).toFloat() / 32767.0.toFloat() * 2.25.toFloat()
        }

        fun bytesToDouble(a1: Byte, a2: Byte): Double {
            val unsigned = unsignedBytesToInt(a1, a2, MSBFirst)
            return unsignedToSigned16bit(unsigned).toDouble() / 32767.0 * 2.25
        }


        fun bytesToDouble(a1: Byte, a2: Byte, a3: Byte): Double {
            val unsigned = unsignedBytesToInt(a1, a2, a3, MSBFirst)
            return unsignedToSigned24bit(unsigned).toDouble() / 8388607.0 * 2.25
        }

        private fun unsignedToSigned16bit(unsigned: Int): Int {
            return if (unsigned and 0x8000 != 0)
                -1 * (0x8000 - (unsigned and 0x8000 - 1))
            else
                unsigned
        }

        private fun unsignedToSigned24bit(unsigned: Int): Int {
            return if (unsigned and 0x800000 != 0) -1 * (0x800000 - (unsigned and 0x800000 - 1))
            else unsigned
        }

        private fun unsignedBytesToInt(b0: Byte, b1: Byte, MSBFirst: Boolean): Int {
            return if (MSBFirst)
                (unsignedByteToInt(b0) shl 8) + unsignedByteToInt(b1)
            else
                unsignedByteToInt(b0) + (unsignedByteToInt(b1) shl 8)
        }

        private fun unsignedBytesToInt(b0: Byte, b1: Byte, b2: Byte, MSBFirst: Boolean): Int {
            return if (MSBFirst)
                (unsignedByteToInt(b0) shl 16) + (unsignedByteToInt(b1) shl 8) + unsignedByteToInt(b2)
            else
                unsignedByteToInt(b0) + (unsignedByteToInt(b1) shl 8) + (unsignedByteToInt(b2) shl 16)
        }

        private fun unsignedByteToInt(b: Byte): Int {
            return (b.toInt() and 0xFF)
        }

//        private fun unsignedToSigned(unsignedInt: Int, size: Int): Int {
//            var unsigned = unsignedInt
//            if (unsigned and (1 shl size - 1) != 0) unsigned = -1 * ((1 shl size - 1) - (unsigned and (1 shl size - 1) - 1))
//            return unsigned
//        }
    }
}
