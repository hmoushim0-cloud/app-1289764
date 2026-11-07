package sa.factory.mihwarmodbusindust

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.mihwarmodbusindust.data.repository.MihwarModbusRepositoryImpl
import sa.factory.mihwarmodbusindust.engine.*
import sa.factory.mihwarmodbusindust.hardware.HardwareSensorDriver
import sa.factory.mihwarmodbusindust.model.MihwarModbusItem

class MihwarModbusRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = MihwarModbusRepositoryImpl(sensorDriver)

        val item1 = MihwarModbusItem("t1", "حساس اهتزاز صناعي", "الوصف", 1200.0, "معتمد", "عام")
        val item2 = MihwarModbusItem("t2", "بوابة ربط Modbus RS485", "الوصف", 800.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(2000.0, order.subtotalSar, 0.01)
        assertEquals(300.0, order.vatSar, 0.01)
        assertEquals(2300.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testModbusCrc16StandardCalculation() {
        val engine = ModbusRtuProtocolEngine()
        // Standard Modbus test vector: [0x01, 0x03, 0x00, 0x00, 0x00, 0x0A] -> CRC is 0xC5CD (Low: 0xC5, High: 0xCD)
        val data = byteArrayOf(0x01, 0x03, 0x00, 0x00, 0x00, 0x0A)
        val crc = engine.calculateCrc16(data, data.size)

        assertEquals(0xCDC5, crc)
    }

    @Test
    fun testModbusReadHoldingRegistersRequestFormat() {
        val engine = ModbusRtuProtocolEngine()
        val req = engine.buildReadRegistersRequest(slaveAddress = 1, startRegister = 100, count = 5)

        assertEquals(8, req.size)
        assertEquals(0x01.toByte(), req[0])
        assertEquals(0x03.toByte(), req[1])
        assertEquals(0x00.toByte(), req[2])
        assertEquals(100.toByte(), req[3])
        assertEquals(0x00.toByte(), req[4])
        assertEquals(5.toByte(), req[5])

        // Verify CRC matches
        val expectedCrc = engine.calculateCrc16(req, 6)
        val actualCrc = ((req[7].toInt() and 0xFF) shl 8) or (req[6].toInt() and 0xFF)
        assertEquals(expectedCrc, actualCrc)
    }

    @Test
    fun testModbusFrameCorruptionDetection() {
        val engine = ModbusRtuProtocolEngine()
        val validFrame = engine.buildVibrationTelemetryResponse(
            slaveAddress = 1,
            xRmsMmS = 1.25,
            yRmsMmS = 1.30,
            zRmsMmS = 0.85,
            peakFreqHz = 50.0,
            tempCelsius = 45.0
        )

        // Corrupt a byte in the payload
        val corruptedFrame = validFrame.clone()
        corruptedFrame[4] = (corruptedFrame[4] + 1).toByte()

        try {
            engine.parseHoldingRegistersResponse(corruptedFrame)
            fail("Expected SecurityException due to CRC mismatch")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("CRC mismatch"))
        }
    }

    @Test
    fun testModbusVibrationTelemetryEncodingAndDecoding() {
        val engine = ModbusRtuProtocolEngine()
        val frame = engine.buildVibrationTelemetryResponse(
            slaveAddress = 2,
            xRmsMmS = 2.45,
            yRmsMmS = 3.12,
            zRmsMmS = 1.88,
            peakFreqHz = 49.8,
            tempCelsius = 48.6
        )

        val registers = engine.parseHoldingRegistersResponse(frame)
        assertEquals(5, registers.size)

        val x = registers[0] / 100.0
        val y = registers[1] / 100.0
        val z = registers[2] / 100.0
        val freq = registers[3] / 10.0
        val temp = registers[4] / 10.0

        assertEquals(2.45, x, 0.01)
        assertEquals(3.12, y, 0.01)
        assertEquals(1.88, z, 0.01)
        assertEquals(49.8, freq, 0.1)
        assertEquals(48.6, temp, 0.1)
    }

    @Test
    fun testTriaxialRmsCalculation() {
        val engine = Iso10816VibrationDiagnosticEngine()

        // 3-4-0 -> sqrt(9 + 16 + 0) = 5.0
        val rms1 = engine.computeTriaxialRms(3.0, 4.0, 0.0)
        assertEquals(5.0, rms1, 0.01)

        // 1.0, 1.0, 1.0 -> sqrt(3) = 1.73
        val rms2 = engine.computeTriaxialRms(1.0, 1.0, 1.0)
        assertEquals(1.73, rms2, 0.02)
    }

    @Test
    fun testIso10816SeverityZoneClassification() {
        val engine = Iso10816VibrationDiagnosticEngine()

        // Class III Large Rigid Turbine:
        // <= 1.80 -> Zone A
        // 1.81 - 4.50 -> Zone B
        // 4.51 - 7.10 -> Zone C
        // > 7.10 -> Zone D
        assertEquals(IsoSeverityZone.ZONE_A, engine.evaluateSeverity(1.20, MachineClass.CLASS_III_LARGE_RIGID))
        assertEquals(IsoSeverityZone.ZONE_B, engine.evaluateSeverity(3.20, MachineClass.CLASS_III_LARGE_RIGID))
        assertEquals(IsoSeverityZone.ZONE_C, engine.evaluateSeverity(5.80, MachineClass.CLASS_III_LARGE_RIGID))
        assertEquals(IsoSeverityZone.ZONE_D, engine.evaluateSeverity(8.50, MachineClass.CLASS_III_LARGE_RIGID))
    }

    @Test
    fun testSpectralImbalanceAndMisalignmentDiagnosis() {
        val engine = Iso10816VibrationDiagnosticEngine()

        // 1X RPM dominant radial vibration -> Imbalance
        val findingImbalance = engine.diagnoseFailureMode(
            xRms = 3.5, yRms = 3.2, zRms = 0.5,
            peakFreqHz = 50.0, nominalRpm = 3000.0 // running freq = 50 Hz
        )
        assertTrue(findingImbalance.contains("اختلال توازن ديناميكي"))

        // 2X RPM harmonic / high axial component -> Misalignment
        val findingMisalignment = engine.diagnoseFailureMode(
            xRms = 2.8, yRms = 2.6, zRms = 3.2,
            peakFreqHz = 100.0, nominalRpm = 3000.0 // 2X harmonic = 100 Hz
        )
        assertTrue(findingMisalignment.contains("عدم محاذاة محورية"))
    }

    @Test
    fun testHardwareSensorDriverInitialStream() = runBlocking {
        val driver = HardwareSensorDriver()
        val frames = driver.observePeripheralStream().take(2).toList()

        assertEquals(2, frames.size)
        assertEquals("CONNECTING", frames[0].connectionState)
        assertEquals("ONLINE", frames[1].connectionState)
        assertTrue(frames[1].livePayload.contains("Turbine Shaft #1"))
        assertTrue(frames[1].livePayload.contains("Triaxial RMS:"))
        assertTrue(frames[1].livePayload.contains("ISO Zone:"))
    }
}
