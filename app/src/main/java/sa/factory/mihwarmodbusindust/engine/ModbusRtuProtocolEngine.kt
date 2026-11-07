package sa.factory.mihwarmodbusindust.engine

class ModbusRtuProtocolEngine {

    companion object {
        const val FUNCTION_READ_HOLDING_REGISTERS: Byte = 0x03
        const val FUNCTION_READ_INPUT_REGISTERS: Byte = 0x04
        const val CRC_POLYNOMIAL = 0xA001
        const val CRC_INITIAL = 0xFFFF
    }

    fun calculateCrc16(bytes: ByteArray, length: Int = bytes.size): Int {
        var crc = CRC_INITIAL
        for (i in 0 until length) {
            crc = crc xor (bytes[i].toInt() and 0xFF)
            for (j in 0 until 8) {
                crc = if ((crc and 0x0001) != 0) {
                    (crc ushr 1) xor CRC_POLYNOMIAL
                } else {
                    crc ushr 1
                }
            }
        }
        return crc and 0xFFFF
    }

    fun buildReadRegistersRequest(slaveAddress: Int, startRegister: Int, count: Int): ByteArray {
        val buffer = ByteArray(8)
        buffer[0] = (slaveAddress and 0xFF).toByte()
        buffer[1] = FUNCTION_READ_HOLDING_REGISTERS
        buffer[2] = ((startRegister ushr 8) and 0xFF).toByte()
        buffer[3] = (startRegister and 0xFF).toByte()
        buffer[4] = ((count ushr 8) and 0xFF).toByte()
        buffer[5] = (count and 0xFF).toByte()

        val crc = calculateCrc16(buffer, 6)
        buffer[6] = (crc and 0xFF).toByte() // CRC Low
        buffer[7] = ((crc ushr 8) and 0xFF).toByte() // CRC High
        return buffer
    }

    fun parseHoldingRegistersResponse(frame: ByteArray): List<Int> {
        if (frame.size < 5) {
            throw IllegalArgumentException("Frame too short for Modbus RTU response (size: ${frame.size})")
        }

        // Verify CRC-16
        val dataLen = frame.size - 2
        val expectedCrc = calculateCrc16(frame, dataLen)
        val actualCrcLow = frame[dataLen].toInt() and 0xFF
        val actualCrcHigh = frame[dataLen + 1].toInt() and 0xFF
        val actualCrc = (actualCrcHigh shl 8) or actualCrcLow

        if (expectedCrc != actualCrc) {
            throw SecurityException("Modbus RTU CRC mismatch: calculated 0x%04X != received 0x%04X".format(expectedCrc, actualCrc))
        }

        val functionCode = frame[1]
        if (functionCode.toInt() and 0x80 != 0) {
            val exceptionCode = frame[2].toInt() and 0xFF
            throw IllegalStateException("Modbus Exception response: 0x%02X".format(exceptionCode))
        }

        val byteCount = frame[2].toInt() and 0xFF
        val registers = mutableListOf<Int>()
        var offset = 3
        val registerCount = byteCount / 2

        for (i in 0 until registerCount) {
            val high = frame[offset].toInt() and 0xFF
            val low = frame[offset + 1].toInt() and 0xFF
            offset += 2
            registers.add((high shl 8) or low)
        }

        return registers
    }

    fun buildVibrationTelemetryResponse(
        slaveAddress: Int,
        xRmsMmS: Double,
        yRmsMmS: Double,
        zRmsMmS: Double,
        peakFreqHz: Double,
        tempCelsius: Double
    ): ByteArray {
        val regX = (xRmsMmS * 100.0).toInt().coerceIn(0, 0xFFFF)
        val regY = (yRmsMmS * 100.0).toInt().coerceIn(0, 0xFFFF)
        val regZ = (zRmsMmS * 100.0).toInt().coerceIn(0, 0xFFFF)
        val regFreq = (peakFreqHz * 10.0).toInt().coerceIn(0, 0xFFFF)
        val regTemp = (tempCelsius * 10.0).toInt().coerceIn(0, 0xFFFF)

        val registers = listOf(regX, regY, regZ, regFreq, regTemp)
        val byteCount = registers.size * 2
        val frame = ByteArray(3 + byteCount + 2)

        frame[0] = (slaveAddress and 0xFF).toByte()
        frame[1] = FUNCTION_READ_HOLDING_REGISTERS
        frame[2] = byteCount.toByte()

        var offset = 3
        for (reg in registers) {
            frame[offset] = ((reg ushr 8) and 0xFF).toByte()
            frame[offset + 1] = (reg and 0xFF).toByte()
            offset += 2
        }

        val crc = calculateCrc16(frame, offset)
        frame[offset] = (crc and 0xFF).toByte()
        frame[offset + 1] = ((crc ushr 8) and 0xFF).toByte()
        return frame
    }
}
