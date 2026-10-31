package com.hizkia.homescreen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val category: String,
    val note: String,
    val createdAt: Long,
    /** Package app bank yang dipakai; null = tunai / belum dicatat. */
    val bankPackage: String? = null,
)
