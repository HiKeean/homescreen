package com.hizkia.homescreen.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense): Long

    @Query("SELECT * FROM expense ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query("DELETE FROM expense WHERE id = :id")
    suspend fun delete(id: Long)
}
