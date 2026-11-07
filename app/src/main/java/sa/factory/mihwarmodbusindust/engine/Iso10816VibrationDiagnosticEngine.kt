package sa.factory.mihwarmodbusindust.engine

import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class MachineClass(val arabicDescription: String) {
    CLASS_I_SMALL_MOTORS("المحركات الصغيرة والكهربائية (أقل من 15 كيلوواط)"),
    CLASS_II_MEDIUM_MACHINES("الآلات والمضخات المتوسطة (15 - 75 كيلوواط)"),
    CLASS_III_LARGE_RIGID("الآلات التوربينية الضخمة ذات القواعد الصلبة"),
    CLASS_IV_LARGE_FLEXIBLE("التوربينات والمولدات الضخمة ذات القواعد المرنة")
}

enum class IsoSeverityZone(val code: String, val arabicStatus: String, val actionRequired: String) {
    ZONE_A("A", "مثالي - مطابق للمواصفات", "تشغيل مستمر غير مقيد"),
    ZONE_B("B", "مقبول - تشغيل اعتيادي", "متابعة دورية مجدولة"),
    ZONE_C("C", "تحذير - تشغيل مقيد", "صيانة استباقية مطلوبة قبل التلف"),
    ZONE_D("D", "خطر حرج - إيقاف فوري", "إيقاف طارئ لتفادي الانفجار الميكانيكي")
}

data class VibrationTelemetryRecord(
    val xRmsMmS: Double,
    val yRmsMmS: Double,
    val zRmsMmS: Double,
    val triaxialRmsMmS: Double,
    val peakFrequencyHz: Double,
    val temperatureCelsius: Double,
    val severityZone: IsoSeverityZone,
    val diagnosticFinding: String,
    val machineClass: MachineClass = MachineClass.CLASS_III_LARGE_RIGID,
    val timestamp: Long = System.currentTimeMillis()
)

class Iso10816VibrationDiagnosticEngine {

    fun computeTriaxialRms(x: Double, y: Double, z: Double): Double {
        val sum = x * x + y * y + z * z
        val rms = sqrt(sum)
        return (rms * 100.0).roundToInt() / 100.0
    }

    fun evaluateSeverity(triaxialRmsMmS: Double, machineClass: MachineClass = MachineClass.CLASS_III_LARGE_RIGID): IsoSeverityZone {
        return when (machineClass) {
            MachineClass.CLASS_I_SMALL_MOTORS -> when {
                triaxialRmsMmS <= 0.71 -> IsoSeverityZone.ZONE_A
                triaxialRmsMmS <= 1.80 -> IsoSeverityZone.ZONE_B
                triaxialRmsMmS <= 4.50 -> IsoSeverityZone.ZONE_C
                else -> IsoSeverityZone.ZONE_D
            }
            MachineClass.CLASS_II_MEDIUM_MACHINES -> when {
                triaxialRmsMmS <= 1.12 -> IsoSeverityZone.ZONE_A
                triaxialRmsMmS <= 2.80 -> IsoSeverityZone.ZONE_B
                triaxialRmsMmS <= 7.10 -> IsoSeverityZone.ZONE_C
                else -> IsoSeverityZone.ZONE_D
            }
            MachineClass.CLASS_III_LARGE_RIGID -> when {
                triaxialRmsMmS <= 1.80 -> IsoSeverityZone.ZONE_A
                triaxialRmsMmS <= 4.50 -> IsoSeverityZone.ZONE_B
                triaxialRmsMmS <= 7.10 -> IsoSeverityZone.ZONE_C
                else -> IsoSeverityZone.ZONE_D
            }
            MachineClass.CLASS_IV_LARGE_FLEXIBLE -> when {
                triaxialRmsMmS <= 2.80 -> IsoSeverityZone.ZONE_A
                triaxialRmsMmS <= 7.10 -> IsoSeverityZone.ZONE_B
                triaxialRmsMmS <= 11.20 -> IsoSeverityZone.ZONE_C
                else -> IsoSeverityZone.ZONE_D
            }
        }
    }

    fun diagnoseFailureMode(
        xRms: Double,
        yRms: Double,
        zRms: Double,
        peakFreqHz: Double,
        nominalRpm: Double = 3000.0
    ): String {
        val runningFreqHz = nominalRpm / 60.0 // 50 Hz for 3000 RPM
        val freqRatio = peakFreqHz / runningFreqHz

        val radialRms = sqrt(xRms * xRms + yRms * yRms)
        val axialRatio = if (radialRms > 0.01) zRms / radialRms else 0.0

        return when {
            // Imbalance: strong 1x running frequency in radial plane
            freqRatio in 0.90..1.15 && radialRms > 2.5 && axialRatio < 0.6 ->
                "اختلال توازن ديناميكي (1X Running Frequency Imbalance)"

            // Misalignment: strong 2x harmonic or high axial component
            (freqRatio in 1.85..2.15 || axialRatio > 0.85) && (xRms > 2.0 || zRms > 2.0) ->
                "عدم محاذاة محورية (2X Mechanical Misalignment / Coupling Wear)"

            // High frequency bearing/gear mesh defect
            freqRatio > 3.8 ->
                "إجهاد محامل دوران فائق التردد (Bearing Defect / High Frequency BPFO/BPFI)"

            // Looseness / Sub-harmonic
            freqRatio in 0.40..0.60 ->
                "ارتخاء ميكانيكي في القواعد (Sub-harmonic Mechanical Looseness)"

            else ->
                "أداء تشغيلي مستقر ومطابق للمعايير القياسية (Normal Operating Condition)"
        }
    }

    fun analyzeVibrationSnapshot(
        xRms: Double,
        yRms: Double,
        zRms: Double,
        peakFreqHz: Double,
        tempCelsius: Double,
        machineClass: MachineClass = MachineClass.CLASS_III_LARGE_RIGID,
        nominalRpm: Double = 3000.0
    ): VibrationTelemetryRecord {
        val triaxial = computeTriaxialRms(xRms, yRms, zRms)
        val zone = evaluateSeverity(triaxial, machineClass)
        val finding = diagnoseFailureMode(xRms, yRms, zRms, peakFreqHz, nominalRpm)

        return VibrationTelemetryRecord(
            xRmsMmS = xRms,
            yRmsMmS = yRms,
            zRmsMmS = zRms,
            triaxialRmsMmS = triaxial,
            peakFrequencyHz = peakFreqHz,
            temperatureCelsius = tempCelsius,
            severityZone = zone,
            diagnosticFinding = finding,
            machineClass = machineClass
        )
    }
}
