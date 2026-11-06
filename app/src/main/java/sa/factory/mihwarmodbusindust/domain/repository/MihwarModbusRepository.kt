package sa.factory.mihwarmodbusindust.domain.repository

import sa.factory.mihwarmodbusindust.model.MihwarModbusItem
import sa.factory.mihwarmodbusindust.model.MihwarModbusOrder
import sa.factory.mihwarmodbusindust.model.MihwarModbusTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MihwarModbusRepository {
    val catalogFlow: Flow<List<MihwarModbusItem>>
    val activeCart: StateFlow<List<MihwarModbusItem>>
    val telemetryFlow: Flow<MihwarModbusTelemetry>
    
    fun addToCart(item: MihwarModbusItem)
    fun removeFromCart(item: MihwarModbusItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): MihwarModbusOrder
    suspend fun refreshCatalog()
}
