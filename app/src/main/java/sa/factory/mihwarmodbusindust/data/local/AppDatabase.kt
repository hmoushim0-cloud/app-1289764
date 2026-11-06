package sa.factory.mihwarmodbusindust.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MihwarModbusEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun industrial_iotDao(): MihwarModbusDao
}
