package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {
    @Query("SELECT * FROM days WHERE date = :date LIMIT 1")
    fun getDay(date: String): Flow<DayEntity?>

    @Query("SELECT * FROM days WHERE date = :date LIMIT 1")
    suspend fun getDayDirect(date: String): DayEntity?

    @Query("SELECT * FROM days ORDER BY date DESC")
    fun getAllDays(): Flow<List<DayEntity>>

    @Query("SELECT * FROM days ORDER BY date DESC")
    suspend fun getAllDaysList(): List<DayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(day: DayEntity)

    @Query("DELETE FROM days WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM days")
    suspend fun clearAll()
}
