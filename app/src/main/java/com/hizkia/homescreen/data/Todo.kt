package com.hizkia.homescreen.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** [dueAt] = awal hari tenggat (epoch millis, zona lokal); null = tanpa tenggat. */
@Entity(tableName = "todo")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueAt: Long?,
    val doneAt: Long?,
    val createdAt: Long,
)

@Dao
interface TodoDao {
    @Insert
    suspend fun insert(todo: Todo): Long

    @Query("SELECT * FROM todo ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Todo>>

    @Query("UPDATE todo SET doneAt = :doneAt WHERE id = :id")
    suspend fun setDone(id: Long, doneAt: Long?)

    @Query("DELETE FROM todo WHERE id = :id")
    suspend fun delete(id: Long)
}
