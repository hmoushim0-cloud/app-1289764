package sa.factory.mihwarmodbusindust.data.repository

import sa.factory.mihwarmodbusindust.domain.repository.MihwarModbusRepository
import sa.factory.mihwarmodbusindust.hardware.HardwareSensorDriver
import sa.factory.mihwarmodbusindust.model.MihwarModbusItem
import sa.factory.mihwarmodbusindust.model.MihwarModbusOrder
import sa.factory.mihwarmodbusindust.model.MihwarModbusTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MihwarModbusRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : MihwarModbusRepository {

    private val _catalog = MutableStateFlow<List<MihwarModbusItem>>(listOf(
        MihwarModbusItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع 3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe", 180.0, "معتمد", "الخدمات الرئيسية"),
        MihwarModbusItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        MihwarModbusItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<MihwarModbusItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<MihwarModbusItem>>(emptyList())
    override val activeCart: StateFlow<List<MihwarModbusItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<MihwarModbusTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            MihwarModbusTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: MihwarModbusItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: MihwarModbusItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): MihwarModbusOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = MihwarModbusOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
