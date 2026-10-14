package com.gestor.medicamentos.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile") fun observe(): Flow<List<Profile>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(p: Profile)
}

@Dao
interface MedDao {
    @Query("SELECT * FROM medications") fun observeAll(): Flow<List<Medication>>
    @Query("SELECT * FROM medications WHERE status = 'ACTIVE'") suspend fun active(): List<Medication>
    @Query("SELECT * FROM medications WHERE id = :id") suspend fun get(id: Long): Medication?
    @Insert suspend fun insert(m: Medication): Long
    @Update suspend fun update(m: Medication)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM dose_logs ORDER BY timeMillis DESC") fun all(): Flow<List<DoseLog>>
    @Insert suspend fun insert(l: DoseLog)
}

@Database(entities = [Profile::class, Medication::class, DoseLog::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profile(): ProfileDao
    abstract fun meds(): MedDao
    abstract fun logs(): LogDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "gestor.db")
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
