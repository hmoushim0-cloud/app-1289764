package sa.factory.mihwarmodbusindust.domain.usecase

import sa.factory.mihwarmodbusindust.domain.repository.MihwarModbusRepository
import sa.factory.mihwarmodbusindust.model.MihwarModbusItem
import sa.factory.mihwarmodbusindust.model.MihwarModbusOrder
import sa.factory.mihwarmodbusindust.model.MihwarModbusTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: MihwarModbusRepository) {
    operator fun invoke(): Flow<List<MihwarModbusItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: MihwarModbusRepository) {
    val cart = repository.activeCart
    fun add(item: MihwarModbusItem) = repository.addToCart(item)
    fun remove(item: MihwarModbusItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: MihwarModbusRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): MihwarModbusOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: MihwarModbusRepository) {
    operator fun invoke(): Flow<MihwarModbusTelemetry> = repository.telemetryFlow
}
