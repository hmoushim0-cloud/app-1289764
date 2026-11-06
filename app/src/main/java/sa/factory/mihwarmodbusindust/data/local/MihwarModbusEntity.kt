package sa.factory.mihwarmodbusindust.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.mihwarmodbusindust.model.MihwarModbusItem

@Entity(tableName = "industrial_iot_items_cache")
data class MihwarModbusEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): MihwarModbusItem = MihwarModbusItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
