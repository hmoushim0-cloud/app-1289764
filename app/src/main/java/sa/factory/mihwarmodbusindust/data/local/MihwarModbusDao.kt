package sa.factory.mihwarmodbusindust.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MihwarModbusDao {
    @Query("SELECT * FROM industrial_iot_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<MihwarModbusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MihwarModbusEntity>)

    @Query("DELETE FROM industrial_iot_items_cache")
    suspend fun clearAll()
}
