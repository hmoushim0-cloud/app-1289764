package sa.factory.mihwarmodbusindust.data.remote

import sa.factory.mihwarmodbusindust.model.MihwarModbusItem
import sa.factory.mihwarmodbusindust.model.MihwarModbusOrder

interface MihwarModbusApiService {
    suspend fun fetchCatalog(): List<MihwarModbusItem>
    suspend fun submitOrder(order: MihwarModbusOrder): Boolean
}
