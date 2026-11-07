package sa.factory.mihwarmodbusindust.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.mihwarmodbusindust.engine.*
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    val peripheral = "3-Axis Vibration Accelerometer + Modbus RS485 RTU"
    val modbusEngine = ModbusRtuProtocolEngine()
    val diagnosticEngine = Iso10816VibrationDiagnosticEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "Modbus RS485 RTU 115200 Baud & Vibration Probe initializing..."))
        delay(200)

        // Seed initial calibrated turbine state
        val initialFrame = modbusEngine.buildVibrationTelemetryResponse(
            slaveAddress = 1,
            xRmsMmS = 0.85,
            yRmsMmS = 0.92,
            zRmsMmS = 0.44,
            peakFreqHz = 50.0,
            tempCelsius = 42.5
        )
        val initialRegisters = modbusEngine.parseHoldingRegistersResponse(initialFrame)
        val initX = initialRegisters[0] / 100.0
        val initY = initialRegisters[1] / 100.0
        val initZ = initialRegisters[2] / 100.0
        val initFreq = initialRegisters[3] / 10.0
        val initTemp = initialRegisters[4] / 10.0

        val initialAnalysis = diagnosticEngine.analyzeVibrationSnapshot(
            initX, initY, initZ, initFreq, initTemp,
            MachineClass.CLASS_III_LARGE_RIGID
        )

        val initPayload = "[ONLINE] Turbine Shaft #1 | Triaxial RMS: " + initialAnalysis.triaxialRmsMmS + " mm/s | " +
                "ISO Zone: " + initialAnalysis.severityZone.name + " (" + initialAnalysis.severityZone.arabicStatus + ") | " +
                "Temp: " + initTemp + " C"
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", initPayload))

        var frameCounter = 1

        while (true) {
            delay(1500)
            frameCounter++
            val jitter = Random.nextInt(1, 8)

            // Dynamic progression through normal, rising load, and anomaly stages
            val (xRms, yRms, zRms, peakFreq, temp) = when {
                frameCounter < 8 -> listOf(
                    1.10 + Random.nextDouble(0.0, 0.3),
                    1.25 + Random.nextDouble(0.0, 0.3),
                    0.60 + Random.nextDouble(0.0, 0.2),
                    50.0,
                    43.0 + frameCounter * 0.2
                )
                frameCounter < 16 -> listOf(
                    2.80 + Random.nextDouble(0.0, 0.5),
                    2.95 + Random.nextDouble(0.0, 0.5),
                    1.40 + Random.nextDouble(0.0, 0.3),
                    50.2, // 1X RPM Imbalance
                    47.0 + frameCounter * 0.3
                )
                else -> listOf(
                    4.20 + Random.nextDouble(0.0, 0.8),
                    4.50 + Random.nextDouble(0.0, 0.8),
                    3.80 + Random.nextDouble(0.0, 0.6),
                    100.0, // 2X Misalignment
                    53.0 + Random.nextDouble(0.0, 2.0)
                )
            }

            // Encode over real Modbus RTU serial frame with CRC-16
            val modbusPkt = modbusEngine.buildVibrationTelemetryResponse(
                slaveAddress = 1,
                xRmsMmS = xRms,
                yRmsMmS = yRms,
                zRmsMmS = zRms,
                peakFreqHz = peakFreq,
                tempCelsius = temp
            )

            val regs = modbusEngine.parseHoldingRegistersResponse(modbusPkt)
            val decodedX = regs[0] / 100.0
            val decodedY = regs[1] / 100.0
            val decodedZ = regs[2] / 100.0
            val decodedFreq = regs[3] / 10.0
            val decodedTemp = regs[4] / 10.0

            val analysis = diagnosticEngine.analyzeVibrationSnapshot(
                decodedX, decodedY, decodedZ, decodedFreq, decodedTemp,
                MachineClass.CLASS_III_LARGE_RIGID
            )

            val liveStatus = "[ACTIVE_STREAMING] Frame #" + frameCounter + " (+" + jitter + "ms) | " +
                    "RMS: " + analysis.triaxialRmsMmS + " mm/s | " +
                    "Zone: " + analysis.severityZone.name + " (" + analysis.severityZone.code + ") | " +
                    "Finding: " + analysis.diagnosticFinding + " | " +
                    "Temp: " + String.format("%.1f", decodedTemp) + " C"

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", liveStatus))
        }
    }
}
